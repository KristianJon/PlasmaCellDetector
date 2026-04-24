package qupath.ext.template.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.template.errorHandling.ErrorHandling;
import qupath.ext.template.services.ActiveLearningService;
import qupath.ext.template.services.StopWatchService;
import qupath.ext.template.stageManager.StageManager;
import qupath.ext.template.ui.dto.GridObject;
import qupath.ext.template.ui.dto.comparisonBetweenPathObjects;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.commands.Commands;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.objects.hierarchy.PathObjectHierarchy;
import qupath.lib.objects.hierarchy.events.PathObjectHierarchyEvent;
import qupath.lib.objects.hierarchy.events.PathObjectHierarchyListener;
import qupath.lib.objects.hierarchy.events.PathObjectSelectionListener;
import qupath.lib.objects.hierarchy.events.PathObjectSelectionModel;
import qupath.lib.regions.RegionRequest;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ActiveLearningController extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(ActiveLearningController.class);
    private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.ext.template.ui.strings");
    private List<PathObject> predictionsMadeByModel = new ArrayList<>();
    private final ArrayList<PathObject> selectedAnnotations = new ArrayList<>();
    private final ArrayList<GridObject> allAnnotationsInGrid = new ArrayList<>();
    private int currentGridIndex = 0;
    private boolean reviewMode = false;
    private boolean singleViewMode = false;
    private int singleViewCounter = 0;
    private PathObject selectedPathObject;
    private final PathObjectHierarchyListener changeInClassificationListener = this::setUpPathObjectClassificationChangeListener;
    private final PathObjectSelectionListener pathObjectSelectionListener = this::setUpPathObjectChangeListener;
    private ChangeListener<ImageData<BufferedImage>> imageDataListener;

    @FXML
    BorderPane borderPaneWindow;

    @FXML
    GridPane gridNode;

    @FXML
    Button previous;

    @FXML
    Button next;

    @FXML
    Button verifyAnnotations;

    @FXML
    CheckBox reviewSelection;

    @FXML
    Label totalPageCount;

    @FXML
    Label currentPageID;

    @FXML
    CheckBox singleView;

    @FXML
    Label timerLabel;

    public static ActiveLearningController createInstance() throws IOException {
        return new ActiveLearningController();
    }

    private ActiveLearningController() throws IOException {
        var url = ResultsController.class.getResource("activeLearningWindow.fxml");
        FXMLLoader loader = new FXMLLoader(url, resources);
        loader.setRoot(this);
        loader.setController(this);
        borderPaneWindow = loader.load();
        borderPaneWindow.setRight(PredictionController.createInstance().getMainBox());

        setUpImageChangeListener();
        //setUpPathObjectChangeListener();
        //setUpPathObjectClassificationChangeListener();
    }

    private void setUpImageChangeListener() {
        if(!(ErrorHandling.isImagePresent())) return;
        attachListeners(QuPathGUI.getInstance().getImageData());

        imageDataListener = (observable, oldValue, newValue) -> {
            logger.info("Received new image");
            if(oldValue != null){
                detachListeners(oldValue);
                logger.info("Detaching previous listeners");
            }
            if(newValue != null){
                attachListeners(newValue);
                logger.info("Attaching listeners to image");
            }
        };

        QuPathGUI.getInstance().imageDataProperty().addListener(imageDataListener);

//        QuPathGUI.getInstance().imageDataProperty().addListener((
//                (observable, oldValue, newValue) -> {
//                    logger.info("Received new image");
//                    if(oldValue != null){
//                        detachListeners(oldValue);
//                        logger.info("Detaching previous listeners");
//                    }
//                    if(newValue != null){
//                        attachListeners(newValue);
//                        logger.info("Attaching listeners to image");
//                    }
//                }));
    }

    public void detachListeners(ImageData<BufferedImage> previousImage){
        previousImage.getHierarchy().getSelectionModel().removePathObjectSelectionListener(pathObjectSelectionListener);
        previousImage.getHierarchy().removeListener(changeInClassificationListener);
    }

    public void attachListeners(ImageData<BufferedImage> newImage){
        newImage.getHierarchy().getSelectionModel().addPathObjectSelectionListener(pathObjectSelectionListener);
        newImage.getHierarchy().addListener(changeInClassificationListener);
    }

    private void setUpPathObjectChangeListener(PathObject newPathObject, PathObject oldPathObject, Collection<PathObject> pathObjects) {
        if(newPathObject != null){
            selectedPathObject = newPathObject;
            if(QuPathGUI.getInstance().getStage().isFocused()){
                openSpecificGridPageBasedOnPathObject(newPathObject);
                StageManager.getInstance().getActiveLearningStage().requestFocus();
                //addPredictionsToGrid(currentGridIndex);
                logger.info("Newly selected object from the main window: {}", newPathObject.getID());
            }
        }

//        PathObjectSelectionModel selectionModel = QuPathGUI.getInstance().getImageData().getHierarchy().getSelectionModel();
//
//        selectionModel.addPathObjectSelectionListener(new PathObjectSelectionListener() {
//            @Override
//            public void selectedPathObjectChanged(PathObject pathObjectSelected, PathObject previousObject, Collection<PathObject> allSelected) {
//                if(pathObjectSelected != null){
//                    selectedPathObject = pathObjectSelected;
//                    if(QuPathGUI.getInstance().getStage().isFocused()){
//                        openSpecificGridPageBasedOnPathObject(pathObjectSelected);
//                        StageManager.getInstance().getActiveLearningStage().requestFocus();
//                        //addPredictionsToGrid(currentGridIndex);
//                        logger.info("Newly selected object from the main window: {}", pathObjectSelected.getID());
//                    }
//                }
//            }
//        });
    }

    public void setUpPathObjectClassificationChangeListener(PathObjectHierarchyEvent pathObjectHierarchyEvent){
//            logger.info("We entered method to change classification");
            if(selectedPathObject == null) return;

            if(pathObjectHierarchyEvent.getEventType() != PathObjectHierarchyEvent.HierarchyEventType.CHANGE_CLASSIFICATION) return;

            if(pathObjectHierarchyEvent.getChangedObjects().contains(selectedPathObject)){
                logger.info("We verified that selected object is the one with changed classification");
                Platform.runLater(() -> {
                    addPredictionsToGrid(currentGridIndex);

                });
            }
        //};

        //QuPathGUI.getInstance().getImageData().getHierarchy().addListener(changeInClassificationListener);
    }

    public void setup(){
        currentGridIndex = 0;
        currentPageID.setText("1");
        deleteGrid();
        predictionsMadeByModel = ActiveLearningService.fetchPredictionsMadeByModel(reviewMode);
        handleButtons();
        calculatePageInformation(true, 0);
        addPredictionsToGrid(currentGridIndex);
    }

    public void refreshGrid(){
        predictionsMadeByModel = ActiveLearningService.fetchPredictionsMadeByModel(reviewMode);
        handleButtons();
        calculatePageInformation(true, 0);
        addPredictionsToGrid(currentGridIndex);
    }

    private void calculatePageInformation(boolean increment, int value) {
        int getCurrentPage = Integer.parseInt(currentPageID.textProperty().get());
        if(increment){
            currentPageID.setText(String.valueOf(getCurrentPage + value));
        }
        else{
            currentPageID.setText(String.valueOf(getCurrentPage - value));
        }

        int numberOfPages = (int) Math.ceil((double) predictionsMadeByModel.size() / 25);
        if(numberOfPages == 0){
            numberOfPages = 1;
        }
        totalPageCount.setText(String.valueOf(numberOfPages));
    }

    private void handleButtons() {
        if((currentGridIndex == 0)){
            previous.setDisable(true);
        }
        if((Integer.parseInt(currentPageID.textProperty().get()) > 1)){
            previous.setDisable(false);
        }
        next.setDisable((predictionsMadeByModel.size() < 25) || ((currentGridIndex + 26) > predictionsMadeByModel.size()));
    }

    private void addPredictionsToGrid(int currentGridIndex){
        deleteGrid();

        int row = 0;
        int column = 0;
        int preferredWidthAndHeight = 120;

        int iterationStopValue = Math.min((currentGridIndex + 25), predictionsMadeByModel.size());
        for (int i = currentGridIndex; i < iterationStopValue; i++) {

            StackPane pane = new StackPane();
            pane.setPrefWidth(preferredWidthAndHeight);
            pane.setPrefHeight(preferredWidthAndHeight);
            pane.setAlignment(Pos.CENTER);

            PathObject currentObject = predictionsMadeByModel.get(i);
            allAnnotationsInGrid.add(new GridObject(pane, currentObject));

            ROI correctROI = calculateAppropriateROIFromCurrentObject(currentObject, preferredWidthAndHeight);

            BufferedImage patch = fetchPatch(correctROI);
            if(patch == null) return;

            drawRectangleWithinPatch(currentObject, patch, correctROI);

            patch.flush();

            // FOR LATER WHEN WE IMPLEMENT SINGLE VIEW
//                if(i == 10){
//                    image.setFitHeight(200);
//                    image.setFitWidth(200);
//                    image.setPreserveRatio(true);
//                    image.setSmooth(true);
//                }

            pane.getChildren().add(new ImageView(SwingFXUtils.toFXImage(patch, null)));

            if(reviewMode) handleLabelsForReviewMode(pane, currentObject);

            handlePaneClicks(pane, currentObject);

            reselectExistingSelections();

            gridNode.add(pane, column, row);

            column++;

            if (column % 5 == 0) {
                column = 0;
                row += 1;
            }
        }
    }

    private void reselectExistingSelections() {
        for(PathObject pathObject : selectedAnnotations){
            for(GridObject gridObject : allAnnotationsInGrid){
                if(gridObject.pathObject().equals(pathObject)){
                    gridObject.pane().setStyle("-fx-border-color: #87CEEB; -fx-border-width: 4px;");
                }
            }
        }
    }

    private BufferedImage fetchPatch(ROI correctROI){
        try {
            ImageServer<?> imageServer = QuPathGUI.getInstance().getImageData().getServer();
            RegionRequest fetchRegionRequest = RegionRequest.createInstance(imageServer.getPath(), 1, correctROI);
            return (BufferedImage) imageServer.readRegion(fetchRegionRequest);
        } catch (IOException e) {
            Dialogs.showErrorMessage("Error reading region", "Could not read requested region");
        }
        return null;
    }

    private void handleLabelsForReviewMode(StackPane pane, PathObject currentObject) {
        Label label = new Label();
        if(Objects.equals(currentObject.getClassification(), "Verified plasma cell")){
            label.setText("PL");
            label.setTextFill(Color.BLUE);
            label.setFont(Font.font(label.getFont().getFamily(), FontWeight.BOLD, label.getFont().getSize()));
            pane.getChildren().add(label);
        }
        if(Objects.equals(currentObject.getClassification(), "Not plasma cell")){
            label.setText("Not PL");
            label.setTextFill(Color.RED);
            label.setFont(Font.font(label.getFont().getFamily(), FontWeight.BOLD, label.getFont().getSize()));
            pane.getChildren().add(label);
        }
    }

    private void drawRectangleWithinPatch(PathObject currentObject, BufferedImage patch, ROI correctROI){
        Graphics2D drawRect = patch.createGraphics();
        if(currentObject.getMeasurementList().get("Probability") <= 0.6){
            drawRect.setColor(java.awt.Color.RED);
        }
        else if(currentObject.getMeasurementList().get("Probability") <= 0.8){
            drawRect.setColor(java.awt.Color.ORANGE);
        }
        else if(currentObject.getMeasurementList().get("Probability") <= 0.9){
            drawRect.setColor(java.awt.Color.YELLOW);
        }
        else if(currentObject.getMeasurementList().get("Probability") <= 1.0){
            drawRect.setColor(java.awt.Color.GREEN);
        }

        int convertXCoordinate = (int) (currentObject.getROI().getBoundsX() - correctROI.getBoundsX());

        int convertYCoordinate = (int) (currentObject.getROI().getBoundsY() - correctROI.getBoundsY());

        drawRect.setStroke(new BasicStroke(2.5f));
        drawRect.drawRect(
                convertXCoordinate,
                convertYCoordinate,
                (int) currentObject.getROI().getBoundsWidth(),
                (int) currentObject.getROI().getBoundsHeight());
        drawRect.dispose();
    }

    private ROI calculateAppropriateROIFromCurrentObject(PathObject currentObject, int prefHeightAndWidth) {
        ROI currentROI = currentObject.getROI();
        int desiredWidthAndHeight = prefHeightAndWidth;
        double changeInHeight = ((desiredWidthAndHeight - 20) - currentROI.getBoundsHeight());
        double changeInWidth = (desiredWidthAndHeight - currentROI.getBoundsWidth());

        if((changeInHeight > 0) || (changeInWidth > 0)){
            return ROIs.createRectangleROI(
                    currentROI.getBoundsX() - (changeInWidth / 2),
                    currentROI.getBoundsY() - (changeInHeight / 2),
                    currentROI.getBoundsWidth() + changeInWidth,
                    currentROI.getBoundsHeight() + changeInHeight);
        }
        if((changeInWidth < 0)){
            return ROIs.createRectangleROI(
                    currentROI.getBoundsX(),
                    currentROI.getBoundsY() - (changeInHeight / 2),
                    currentROI.getBoundsWidth(),
                    currentROI.getBoundsHeight() + changeInHeight);
        }
        if(!(changeInHeight < 0)){
            return ROIs.createRectangleROI(
                    currentROI.getBoundsX() - (changeInWidth / 2),
                    currentROI.getBoundsY(),
                    currentROI.getBoundsWidth() + changeInWidth,
                    currentROI.getBoundsHeight());
        }
        return currentROI;
    }

    private void handlePaneClicks(StackPane pane, PathObject o) {
        PauseTransition singleClickDelay = new PauseTransition(Duration.millis(150));

        singleClickDelay.setOnFinished(event -> {
            if(selectedAnnotations.contains(o)){
                pane.setStyle("");
                selectedAnnotations.remove(o);
            } else{
                pane.setStyle("-fx-border-color: #87CEEB; -fx-border-width: 4px;");
                selectedAnnotations.add(o);
            }
        });

        pane.setOnMouseClicked(e -> {

            if(e.getButton() != MouseButton.PRIMARY) return;

            if(e.getClickCount() == 1){
                singleClickDelay.playFromStart();
            } else if(e.getClickCount() == 2){
                singleClickDelay.stop();
                if(QuPathGUI.getInstance().getViewer().getMagnification() < 40){
                    QuPathGUI.getInstance().getViewer().setMagnification(65.0);
                }
                QuPathGUI.getInstance().getViewer().centerROI(o.getROI());
                QuPathGUI.getInstance().getImageData().getHierarchy().getSelectionModel().setSelectedObject(o);
                e.consume();
            }
        });
    }

    @FXML
    public void fetchNextPredictions(){
        if(!(next.isDisable())){
            currentGridIndex += 25;
            addPredictionsToGrid(currentGridIndex);
            calculatePageInformation(true, 1);
            //clearSelectedAnnotations();
            handleButtons();
        }
    }

    @FXML
    public void fetchPreviousPredictions(){
        if(currentGridIndex != 0){
            //clearSelectedAnnotations();
            currentGridIndex -= 25;
            addPredictionsToGrid(currentGridIndex);
            calculatePageInformation(false, 1);
            handleButtons();
        }
    }

    private void clearSelectedAnnotations(){
        selectedAnnotations.clear();
    }

    @FXML
    public void deleteGrid(){
        gridNode.getChildren().clear();
        allAnnotationsInGrid.clear();
    }

    // TODO -> If pathobject is present in selected annotations AND current grid annotations, then we can process it
    // TODO -> When creating grid, if pathobject in current grid already exists in selectedAnnotations, re-select the tile
    @FXML
    public void verifySelectedAnnotations() {
        ArrayList<PathObject> pathObjectsWithChangedClassification = new ArrayList<>();
        if(reviewMode){
            ArrayList<GridObject> tempGridObject = new ArrayList<>();
            //ArrayList<PathObject> tempPathObjects = new ArrayList<>();
            for(PathObject pathObject : selectedAnnotations){
                logger.info("Current path object in list of selected path objects: {}", pathObject);
                for(GridObject gridObject : allAnnotationsInGrid) {
                    if(gridObject.pathObject().equals(pathObject)) {
                        pathObjectsWithChangedClassification.add(gridObject.pathObject());
                        if (Objects.equals(pathObject.getClassification(), "Verified plasma cell")) {
                            logger.info("Swapped from 'verified plasma cell' to 'not plasma cell'");
                            pathObject.setPathClass(PathClass.getInstance("Not plasma cell"));
                            pathObject.setClassification("Not plasma cell");
                        } else {
                            logger.info("Swapped from 'not plasma cell' to 'verified plasma cell'");
                            pathObject.setPathClass(PathClass.getInstance("Verified plasma cell"));
                            pathObject.setClassification("Verified plasma cell");
                        }
                        //tempPathObjects.add(pathObject);
                        tempGridObject.add(gridObject);
                        // TODO -> Method to remove css selection
                    }
                }
            }
            for(GridObject gridObject : tempGridObject){
                selectedAnnotations.remove(gridObject.pathObject());
                logger.info("Removed path object from selected annotations: {}", gridObject.pathObject());
                gridObject.pane().setStyle("");
            }
            addPredictionsToGrid(currentGridIndex);
            //clearSelectedAnnotations();
            //return;
        }
        else{
            ArrayList<GridObject> temp = new ArrayList<>();
            for(PathObject pathObject : selectedAnnotations){
                for(GridObject gridObject : allAnnotationsInGrid){
                    if(gridObject.pathObject().equals(pathObject)){
                        pathObjectsWithChangedClassification.add(gridObject.pathObject());
                        temp.add(gridObject);
                        pathObject.setPathClass(PathClass.getInstance("Verified plasma cell"));
                        pathObject.setClassification("Verified plasma cell");
                    }
                }
            }
            for(GridObject gridObject : temp){
                allAnnotationsInGrid.remove(gridObject);
            }
            for(GridObject gridObject : allAnnotationsInGrid){
                gridObject.pathObject().setPathClass(PathClass.getInstance("Not plasma cell"));
                gridObject.pathObject().setClassification("Not plasma cell");
                //pathObjectsWithChangedClassification.add(gridObject.pathObject());
            }
            // TODO -> jump back one page instead of setup
            predictionsMadeByModel = ActiveLearningService.fetchPredictionsMadeByModel(false);
            if((currentGridIndex >= predictionsMadeByModel.size()) && (currentGridIndex >= 25)){
                logger.info("Branch 1 taken: Current grid index: {}, predictions made by model size: {} ", currentGridIndex, predictionsMadeByModel.size());
                currentGridIndex -= 25;
                currentPageID.setText(String.valueOf((Integer.parseInt(currentPageID.getText()) - 1)));
                addPredictionsToGrid(currentGridIndex);
                calculatePageInformation(true, 0);
                handleButtons();
            } else if(currentGridIndex < predictionsMadeByModel.size()){
                logger.info("Branch 2 taken: Current grid index: {}, predictions made by model size: {} ", currentGridIndex, predictionsMadeByModel.size());
                calculatePageInformation(true, 0);
                addPredictionsToGrid(currentGridIndex);
                handleButtons();
            } else{
                logger.info("Branch 3 taken: Current grid index: {}, predictions made by model size: {} ", currentGridIndex, predictionsMadeByModel.size());
                setup();
            }
        }
        QuPathGUI.getInstance().getImageData().getHierarchy().fireObjectClassificationsChangedEvent(this, pathObjectsWithChangedClassification);
    }

    @FXML
    public void changeViewToReview(){
        clearSelectedAnnotations();
        if(reviewSelection.isSelected()){
            reviewMode = true;
            verifyAnnotations.setText("Flip");
            setup();
        }
        else{
            reviewMode = false;
            verifyAnnotations.setText("Verify");
            setup();
        }
    }

    private void openSpecificGridPageBasedOnPathObject(PathObject pathObject) {
        if(!ErrorHandling.isPathObjectModelPrediction(pathObject)) return;

        handleReviewToggleIfNeeded(pathObject);

        int index = findIndexOfSelectedPathObject(pathObject);

        if(index < 0) return;

        if(StageManager.getInstance().getActiveLearningStage().isShowing()){
            int tempCurrentGridIndex = currentGridIndex;

            currentGridIndex = (int) Math.floor((double) index /25) * 25;

            if(tempCurrentGridIndex != currentGridIndex) addPredictionsToGrid(currentGridIndex);

            updateCurrentPageNumber();

            markSelectedPathObjectInGrid(index);

            handleButtons();

            if(!selectedAnnotations.contains(pathObject)) selectedAnnotations.add(pathObject);
            logger.info("Added pathObject to list of selected annotations: {}", pathObject.getID());
        }
    }

    private void updateCurrentPageNumber() {
        if(currentGridIndex == 0){
            currentPageID.setText("1");
        } else{
            int pageForClickedPathObject = (int) Math.floor((double) currentGridIndex /25) + 1;
            currentPageID.setText(String.valueOf(pageForClickedPathObject));
        }
    }

    private void handleReviewToggleIfNeeded(PathObject pathObject) {
        if(ErrorHandling.isPathObjectAlreadyProcessed(pathObject) && !reviewSelection.isSelected()){
            reviewSelection.setSelected(true);
            changeViewToReview();
            return;
        }

        if(!ErrorHandling.isPathObjectAlreadyProcessed(pathObject) && reviewSelection.isSelected()){
            reviewSelection.setSelected(false);
            changeViewToReview();
        }
    }

    private void markSelectedPathObjectInGrid(int index) {
        Node foundPane = gridNode.getChildren().get((index - currentGridIndex));
        foundPane.setStyle("-fx-border-color: #87CEEB; -fx-border-width: 4px;");
    }

    private int findIndexOfSelectedPathObject(PathObject pathObject){
        for(int i = 0; i < predictionsMadeByModel.size(); i++){
            if(predictionsMadeByModel.get(i).getID() == pathObject.getID()){
                return i;
            }
        }
        return -1;
    }

    @FXML
    public void startTimer(){
        StopWatchService.getInstance().startTimer();
    }

    @FXML
    public void pauseTimer(){
        StopWatchService.getInstance().pauseTimer();
    }

    @FXML
    public void resetTimer(){
        StopWatchService.getInstance().resetTimer();
    }

    public Label getTimerLabel(){
        return timerLabel;
    }
}