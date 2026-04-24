package qupath.ext.template.stageManager;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.template.DemoExtension;
import qupath.ext.template.errorHandling.ErrorHandling;
import qupath.ext.template.ui.ActiveLearningController;
import qupath.ext.template.ui.PredictionController;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.gui.QuPathGUI;

import java.io.IOException;
import java.util.ResourceBundle;

public class StageManager {
    private static StageManager stageManager;

    /**
     * A resource bundle containing all the text used by the extension. This may be useful for translation to other languages.
     * Note that this is optional, and you can define the text within the code and FXML files that you use.
     */
    private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.ext.template.ui.strings");

    /**
     * Logger for DemoExtension class, this is used to display both information and error in QuPath's log
     */
    private static final Logger logger = LoggerFactory.getLogger(StageManager.class);


    /**
     * Create a stage for the prediction window
     */
    private Stage predictionStage;

    /**
     * Create a stage for the active learning window
     */
    private Stage activeLearningStage;

    /**
     * Reference to controller for active learning window, needed to call refresh functions etc
     */
    private ActiveLearningController activeLearningController;


    private StageManager(){}

    public Stage getActiveLearningStage(){
        return activeLearningStage;
    }

    public ActiveLearningController getActiveLearningController(){
        return activeLearningController;
    }
    public Stage getPredictionStage(){
        return predictionStage;
    }

    /**
     * Creating a new stage with a JavaFX FXML interface.
     */
    public void createPredictionStage() {
        if (predictionStage == null) {
            try {
                predictionStage = new Stage();
                Scene scene = new Scene(PredictionController.createInstance());
                predictionStage.initOwner(QuPathGUI.getInstance().getStage());
                predictionStage.setTitle(resources.getString("stage.title"));
                predictionStage.setScene(scene);
                predictionStage.setResizable(false);
            } catch (IOException e) {
                Dialogs.showErrorMessage(resources.getString("error"), resources.getString("error.gui-loading-failed"));
                logger.error("Unable to load extension interface FXML", e);
            }
        }
        predictionStage.show();
    }

    /**
     * Creating a new stage with a JavaFX FXML interface.
     */
    public void createActiveLearningStage() {
//        if(!ErrorHandling.isPredictionsPresent()){
//            return;
//        }

        DemoExtension.createClassificationsInQuPath();
        if (activeLearningStage == null) {
            try {
                activeLearningStage = new Stage();
                activeLearningController = ActiveLearningController.createInstance();
                Scene scene = new Scene(activeLearningController);
                scene.setOnKeyPressed(event -> {
                    if(event.getCode() == KeyCode.A){
                        activeLearningController.fetchPreviousPredictions();
                    }
                    if(event.getCode() == KeyCode.D){
                        activeLearningController.fetchNextPredictions();
                    }
                });
                activeLearningStage.initOwner(QuPathGUI.getInstance().getStage());
                activeLearningStage.setTitle(resources.getString("stage.title"));
                activeLearningStage.setScene(scene);
                activeLearningStage.setResizable(false);
            } catch (IOException e) {
                Dialogs.showErrorMessage(resources.getString("error"), resources.getString("error.gui-loading-failed"));
                logger.error("Unable to load extension interface FXML", e);
            }
        }

        activeLearningController.setup();
        activeLearningStage.show();
    }

    public static StageManager getInstance(){
        if(stageManager == null){
            stageManager = new StageManager();
        }
        return stageManager;
    }
}