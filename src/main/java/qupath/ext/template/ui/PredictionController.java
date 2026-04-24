package qupath.ext.template.ui;

import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.template.djl.Yolo10Translator;
import qupath.ext.template.djl.Yolo11Translator;
import qupath.ext.template.errorHandling.ErrorHandling;
import qupath.ext.template.predictionThread.PredictionThread;
import qupath.ext.template.stageManager.StageManager;
import qupath.ext.template.ui.dto.PredictionResult;
import qupath.fx.dialogs.Dialogs;
import qupath.fx.dialogs.FileChoosers;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.commands.Commands;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.regions.ImageRegion;
import qupath.lib.regions.RegionRequest;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PredictionController extends HBox {
    private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.ext.template.ui.strings");
    private static final Logger logger = LoggerFactory.getLogger(PredictionController.class);
    private int widthAndHeight;
    private File chosenFile;
    private static Criteria<Image, DetectedObjects> criteria;
    private Stage stage;
    private int finalImageSize;

    @FXML
    TextField modelLabel;

    @FXML
    ProgressBar progressBar;

    @FXML
    Button makePredictionButton;

    @FXML
    Button modelSelectionButton;

    @FXML
    TextField imageSize;

    @FXML
    Button imageSizeButton;

    @FXML
    Label annotationProcessingLabel;

    @FXML
    Label percentLabel;

    @FXML
    Button viewSummary;

    @FXML
    HBox mainBox;

    public static PredictionController createInstance() throws IOException {
        return new PredictionController();
    }

    private PredictionController() throws IOException {
        var url = PredictionController.class.getResource("predictionWindow.fxml");
        FXMLLoader loader = new FXMLLoader(url, resources);
        loader.setRoot(this);
        loader.setController(this);
        mainBox = loader.load();
        progressBar.setVisible(false);
        modelLabel.setText("Choose model...");
        viewSummary.setVisible(false);

        // TODO -> Improve this flow, need it initialized here to activate listeners
        //ResultsController.createInstance();
    }


    @FXML
    public void promptForFile(){
        try {
            makePredictionButton.setDisable(true);
            imageSize.setText("");
            imageSize.setDisable(true);
            imageSizeButton.setDisable(true);
            chosenFile = FileChoosers.promptForFile("Model selection");
            modelLabel.setText(chosenFile.getName());
            imageSize.setDisable(false);
            imageSizeButton.setDisable(false);
        } catch (NullPointerException npe){
            Dialogs.showErrorMessage("File selection error", "No file selected");
        }
    }

    @FXML
    public void registerImageSize(){
        try {
            finalImageSize = Integer.parseInt(imageSize.getText());
            if(finalImageSize < 32 || finalImageSize > 1600){
                throw new IllegalArgumentException();
            }
            widthAndHeight = finalImageSize;
            defineCriteria(Paths.get(chosenFile.getAbsolutePath()), chosenFile.getName(), finalImageSize);
            makePredictionButton.setDisable(false);
            imageSize.setDisable(true);
        } catch (NumberFormatException nfe){
            Dialogs.showErrorMessage("Invalid input",
                    "Please enter the size of expected input image as '640' or '1024' for example");
        } catch (IllegalArgumentException iae){
            Dialogs.showErrorMessage("Invalid image size",
                    "Image size should be between 32 and 1600");
        }
    }

    public void defineCriteria(Path absolutePath, String fileName, int imageSize) {
        //logger.info("GPU Count: {}, has GPU: {}", Engine.getInstance().getGpuCount(), Engine.getInstance().getGpuCount() > 0);
        criteria =  Criteria.builder()
                .optEngine("PyTorch")
                .setTypes(Image.class, DetectedObjects.class)
                .optTranslator(new Yolo11Translator(0.4F, 0.5F, imageSize))
                //.optTranslator(YoloV8Translator.builder().build())
                //.optDevice(Device.gpu())
                .optModelPath(absolutePath)
                .optModelName(fileName)
                .build();
    }

    public static Criteria<Image, DetectedObjects> getCriteria(){
        return criteria;
    }

//    @FXML
//    public void openActiveLearningWindow(){
//        StageManager.getInstance().createActiveLearningStage();
//    }

    @FXML
    public void handlePrediction() throws IOException {

        if(performErrorHandlingBeforePredictionBeings()) return;

        // set all variables to 0 for each new prediction
        PredictionSummary.initializeFields();

        makeElementsVisible();

        startPredictionThread();
    }

    private void startPredictionThread() {
        Set<PathObject> selectedAnnotations =
                QuPathGUI.getInstance().getImageData().getHierarchy().getSelectionModel().getSelectedObjects();

        PredictionThread predictionThread = new PredictionThread(selectedAnnotations, widthAndHeight, chosenFile.getName());
        progressBar.progressProperty().bind(predictionThread.progressProperty());
        annotationProcessingLabel.textProperty().bind(predictionThread.messageProperty());
        percentLabel.textProperty().bind(predictionThread.progressProperty().multiply(100).asString("%.0f%%"));

        Thread thread = new Thread(predictionThread);
        thread.setDaemon(true);
        thread.start();
        buttonsDisabled(true);

        handleSuccessfulPrediction(predictionThread);
    }

    private void handleSuccessfulPrediction(PredictionThread predictionThread){
        predictionThread.setOnSucceeded(e -> {
            viewSummary.setVisible(true);

            PredictionResult result = predictionThread.getValue();
            buttonsDisabled(false);

            PredictionSummary.setTimeUsageRawValue(result.time());
            PredictionSummary.setTotalAnnotations(result.totalAnnotation());
            PredictionSummary.setRedAnnotations(result.red());
            PredictionSummary.setOrangeAnnotations(result.orange());
            PredictionSummary.setYellowAnnotations(result.yellow());
            PredictionSummary.setGreenAnnotations(result.green());
            PredictionSummary.setAreaCoveredByCurrentRun(result.areaOfCurrentSelection());
            PredictionSummary.setTotalAreaCovered(result.totalAreaCovered());

            StageManager.getInstance().getActiveLearningController().refreshGrid();
        });
    }

    private boolean performErrorHandlingBeforePredictionBeings() {
        if(!ErrorHandling.isImagePresent()) {
            return true;
        }

        if(!ErrorHandling.isSelectionPresent()){
            return true;
        }

        for(PathObject annotation : QuPathGUI.getInstance().getImageData().getHierarchy().getSelectionModel().getSelectedObjects()){
            if(ErrorHandling.isImageLessThanChosenDimensions(annotation, finalImageSize)) return true;
        }
        return false;
    }

    void buttonsDisabled(boolean bool){
        modelSelectionButton.setDisable(bool);
        makePredictionButton.setDisable(bool);
        imageSizeButton.setDisable(bool);
        viewSummary.setDisable(bool);
    }

    private void makeElementsVisible() {
        progressBar.setVisible(true);
    }


    @FXML
    public void openSummaryScene(){
        if (stage == null) {
            try {
                stage = new Stage();
                Scene scene = new Scene(ResultsController.createInstance());
                stage.initOwner(QuPathGUI.getInstance().getStage());
                stage.setTitle(resources.getString("stage.title"));
                stage.setScene(scene);
                stage.setResizable(false);
            } catch (IOException e) {
                Dialogs.showErrorMessage(resources.getString("error"), resources.getString("error.gui-loading-failed"));
                logger.error("Unable to load extension interface FXML", e);
            }
        }
        stage.show();
    }

    public HBox getMainBox(){
        return mainBox;
    }
}