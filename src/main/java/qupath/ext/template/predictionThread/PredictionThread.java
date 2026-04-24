package qupath.ext.template.predictionThread;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.template.ui.PredictionController;
import qupath.ext.template.ui.dto.PredictionResult;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.commands.Commands;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.PathObjects;
import qupath.lib.regions.RegionRequest;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

public class PredictionThread extends Task<PredictionResult> {
    private final Logger logger = LoggerFactory.getLogger(PredictionThread.class);
    private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.ext.template.ui.strings");
    private final Set<PathObject> selectedAnnotations;
    private int redCounter = 0;
    private int orangeCounter = 0;
    private int yellowCounter = 0;
    private int greenCounter = 0;
    private int totalAnnotations = 0;
    private double areaCoveredByCurrentRun = 0;
    private double totalAreaCovered = 0;
    private final boolean isSlidingWindow = false;
    private final int widthAndHeight;
    private long areaOfFullImage;
    private final String modelName;

    public PredictionThread(Set<PathObject> selectedAnnotations, int widthAndHeight, String modelName){
        this.selectedAnnotations = selectedAnnotations;
        this.widthAndHeight = widthAndHeight;
        this.modelName = modelName;
    }

    @Override
    protected PredictionResult call() throws Exception {
        long startTime = System.nanoTime();

        areaOfFullImage = (long) QuPathGUI.getInstance().getImageData().getServer().getHeight()
                * QuPathGUI.getInstance().getImageData().getServer().getWidth();

        //buttonsDisabled(true);
        try (ZooModel<Image, DetectedObjects> model = PredictionController.getCriteria().loadModel();
             Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
            logger.info("Model device: {}", model.getNDManager().getDevice());

            // Iterate through all selected annotations
            int annotationCounter = 1;
            for (PathObject annotation : selectedAnnotations) {
                calculateAreaOfSelectedAnnotation(annotation);

                // Informing user of status
                updateMessage("Processing annotation: " + annotationCounter + "/" + selectedAnnotations.size());
                annotationCounter++;

                // Lock the annotation so the user does not move it when scrolling through results
                annotation.setLocked(true);

                // Calculate how many rows and columns based on size of selected annotation
                int numberOfColumns;
                int numberOfRows;
                if(isSlidingWindow){
                    numberOfColumns = (int) Math.floor(annotation.getROI().getBoundsWidth() / (widthAndHeight - (widthAndHeight*0.2)));
                    numberOfRows = (int) Math.floor(annotation.getROI().getBoundsHeight() / (widthAndHeight - (widthAndHeight*0.2)));
                } else{
                    numberOfColumns = (int) Math.floor(annotation.getROI().getBoundsWidth() / widthAndHeight);
                    numberOfRows = (int) Math.floor(annotation.getROI().getBoundsHeight() / widthAndHeight);
                }

                int totalNumberOfIterations = numberOfColumns * numberOfRows;


                long counter = 1;

                // Iterate through each cell
                for (int i = 0; i < numberOfRows; i++) {
                    for (int j = 0; j < numberOfColumns; j++) {
                        ROI currentPatch = calculateROIPatch(annotation, i, j);

                        // TODO -> REMOVE IF PATCHWISE DOES NOT TURN OUT GREAT
                        //annotation.addChildObject(PathObjects.createAnnotationObject(currentPatch));

                        Image image = fetchImageRegionFromPatch(QuPathGUI.getInstance().getImageData().getServer(), currentPatch);

                        DetectedObjects detection = predictor.predict(image);
                        logger.info("Detections: {}", detection);

                        // Let's start simple:
                        // Fetch all predictions from current patch
                        // Find annotations with IoU > 0 to current detection (that means there is overlap)
                        // Compare confidence with every found annotation, keep the one with highest confidence discard the rest
                        //detectionIsDuplicate(detection, currentPatch);

                        createAnnotation(currentPatch, annotation, detection);

                        //Commands.refreshObjectIDs(QuPathGUI.getInstance().getImageData(), false);

                        updateProgress(counter, totalNumberOfIterations);
                        counter++;
                    }
                }
            }
        }
        catch (TranslateException | IOException | ModelNotFoundException | MalformedModelException e) {
            Dialogs.showErrorMessage(resources.getString("error.prediction"), resources.getString("error.prediction.description"));
            logger.error(e.getMessage());
        }
        calculateTotalAreaCovered();
        long endTime = System.nanoTime();
        return new PredictionResult(endTime - startTime, redCounter, orangeCounter, yellowCounter, greenCounter, areaCoveredByCurrentRun, totalAreaCovered, totalAnnotations);
    }

//    private void detectionIsDuplicate(DetectedObjects detection, ROI currentPatch){
//        // If IoU is greater than 10, we compare confidence and keep the one with the highest confidence
//        //Collection<PathObject> allAnnotationsFromCurrentPatch = QuPathGUI.getInstance().getImageData().getHierarchy().getAnnotationsForROI(currentPatch);
//        Collection<PathObject> allAnnotationsFromCurrentPatch =
//                QuPathGUI.getInstance().getImageData().getHierarchy().getAnnotationsForRegion(ImageRegion.createInstance(currentPatch));
//        ArrayList<PathObject> allPreviousDetecions = new ArrayList<>();
//        for(PathObject obj : allAnnotationsFromCurrentPatch){
//            if(!Double.isNaN(obj.getMeasurementList().get("Probability"))){
//                allPreviousDetecions.add(obj);
//            }
//        }
//
//        for(int i = 0; i < detection.getNumberOfObjects(); i++){
//            PathObject pathObject = createPathObject(detection.item(i), currentPatch);
//            for(PathObject existingAnnotations : allPreviousDetecions){
//                logger.info("IoU between existing and new detection: {}", IoU(pathObject, existingAnnotations));
//                if(IoU(pathObject, existingAnnotations) >= 0.3){
//                    logger.info("Probability of new detection: {}, Probability of old detection: {}", detection.item(i).getProbability(), existingAnnotations.getMeasurementList().get("Probability"));
//                    if(detection.item(i).getProbability() > existingAnnotations.getMeasurementList().get("Probability")){
//                        QuPathGUI.getInstance().getImageData().getHierarchy().removeObject(existingAnnotations, false);
//                        logger.info("Deleted existing annotation with confidence: {}", existingAnnotations.getMeasurementList().get("Probability"));
//                    }
//                }
//            }
//        }
//    }

    private double IoU(PathObject newDetection, PathObject oldDetection){
        // Find top left and bottom right point of first box
        double topLeftXBox1 = newDetection.getROI().getBoundsX();
        double topLeftYBox1 = newDetection.getROI().getBoundsY();
        double bottomRightXBox1 = topLeftXBox1 + newDetection.getROI().getBoundsWidth();
        double bottomRightYBox1 = topLeftYBox1 + newDetection.getROI().getBoundsHeight();

        // Find top left and bottom right point of second box
        double topLeftXBox2 = oldDetection.getROI().getBoundsX();
        double topLeftYBox2 = oldDetection.getROI().getBoundsY();
        double bottomRightXBox2 = topLeftXBox2 + oldDetection.getROI().getBoundsWidth();
        double bottomRightYBox2 = topLeftYBox2 + oldDetection.getROI().getBoundsHeight();

        // Find max of top left corners
        double maxValueTopLeftX = Math.max(topLeftXBox1, topLeftXBox2);
        double maxValueTopLeftY = Math.max(topLeftYBox1, topLeftYBox2);

        // Find minimum of bottom right corners
        double minValueBottomRightX = Math.min(bottomRightXBox1, bottomRightXBox2);
        double minValueBottomRightY = Math.min(bottomRightYBox1, bottomRightYBox2);

        // Find intersection
        double intersectionWidth = Math.max(0, (minValueBottomRightX - maxValueTopLeftX));
        double intersectionHeight = Math.max(0, (minValueBottomRightY - maxValueTopLeftY));

        // Calculate intersection area, if == 0 the boxes don't overlap
        double intersectionArea = intersectionWidth * intersectionHeight;

        // Calculate area of both boxes
        double areaBox1 = newDetection.getROI().getBoundsWidth()
                * newDetection.getROI().getBoundsHeight();

        double areaBox2 = oldDetection.getROI().getBoundsWidth()
                * oldDetection.getROI().getBoundsHeight();

        return intersectionArea/(areaBox1 + areaBox2 - intersectionArea);
    }

    private void calculateAreaOfSelectedAnnotation(PathObject annotation) {
        double area = (((annotation.getROI().getBoundsHeight() * annotation.getROI().getBoundsWidth())/areaOfFullImage) * 100);
        areaCoveredByCurrentRun += area;
    }

    private void calculateTotalAreaCovered(){
        double tempArea = 0;
        ArrayList<PathObject> allAnnotations =
                (ArrayList<PathObject>) QuPathGUI.getInstance().getImageData().getHierarchy().getAnnotationObjects();

        for(PathObject obj : allAnnotations){
            if(obj.hasChildObjects()){
                double areaCalculated = obj.getROI().getBoundsHeight() * obj.getROI().getBoundsWidth();
                tempArea += areaCalculated;
            }
        }
        totalAreaCovered = ((tempArea/areaOfFullImage) * 100);
    }

    private ROI calculateROIPatch(PathObject annotation, int i, int j){
        // TODO -> REMOVE IF PATCHWISE DOES NOT TURN OUT GREAT
        double newTopLeftX;
        double newTopLeftY;
        if(isSlidingWindow){
            newTopLeftX = annotation.getROI().getBoundsX() + (((widthAndHeight - (widthAndHeight*0.2)) * j));
            newTopLeftY = annotation.getROI().getBoundsY() + (((widthAndHeight - (widthAndHeight*0.2)) * i));
        } else{
            newTopLeftX = annotation.getROI().getBoundsX() + (widthAndHeight * j);
            newTopLeftY = annotation.getROI().getBoundsY() + (widthAndHeight* i);
        }
        logger.info("New top left corner: ({}, {})", newTopLeftX, newTopLeftY);
        return ROIs.createRectangleROI(newTopLeftX, newTopLeftY, widthAndHeight, widthAndHeight);
    }

    private Image fetchImageRegionFromPatch(ImageServer<?> imageServer, ROI currentPatch) throws IOException {
        RegionRequest fetchRegionRequest = RegionRequest.createInstance(imageServer.getPath(), 1, currentPatch);

        BufferedImage patch = (BufferedImage) imageServer.readRegion(fetchRegionRequest);

        Image extractedImage = ImageFactory.getInstance().fromImage(patch);

        patch.flush();

        return extractedImage;
    }


    private void createAnnotation(ROI roi, PathObject annotation, DetectedObjects detections){
        for (int i = 0; i < Objects.requireNonNull(detections).getNumberOfObjects(); i++) {
            // iterate through, and fetch, detections
            DetectedObjects.DetectedObject detection = detections.item(i);

            // create pathObject
            PathObject pathObject = createPathObject(detection, roi);

//                logger.info("Does current detection box have existing prediction: {}",
//                        QuPathGUI.getInstance().getImageData().getHierarchy().hasAnnotationsForRegion(ImageRegion.createInstance(pathObject.getROI())));
            Collection<PathObject> allAnnots = QuPathGUI.getInstance().getImageData().getHierarchy().getAnnotationsForROI(roi);
            logger.info("Region: {}, Region ID: {}, all objects at this region: {}", pathObject.getROI(), pathObject.getID(), allAnnots);
            ArrayList<PathObject> allPreviousDetecions = new ArrayList<>();
            for(PathObject obj : allAnnots){
                if(!Double.isNaN(obj.getMeasurementList().get("Probability"))){
                    allPreviousDetecions.add(obj);
                }
            }

            //logger.info("Existing detections are: {}", allPreviousDetecions.stream().map(PathObject::getID));
            for(PathObject pathObject1 : allPreviousDetecions){
                logger.info("Path object ID: {}, Path Conf: {}", pathObject1.getID(), pathObject1.getMeasurementList().get("Probability"));
            }

            // style pathObject
            stylePathObject(pathObject, detection);

            //logger.info("Chosen color: {}", pathObject.getColor());
            // save annotation
            annotation.addChildObject(pathObject);
            QuPathGUI.getInstance().getImageData().getHierarchy().fireHierarchyChangedEvent(this, annotation);
            //predictionResult.setText("Detection added: " + pathObject + ", Probability: " + detection.getProbability());
        }
    }

    private PathObject createPathObject(DetectedObjects.DetectedObject detection, ROI roi){
        BoundingBox box = detection.getBoundingBox();
        double x = (box.getBounds().getX() * widthAndHeight) + roi.getBoundsX();
        double y = (box.getBounds().getY() * widthAndHeight) + roi.getBoundsY();
        double width = box.getBounds().getWidth() * widthAndHeight;
        double height = box.getBounds().getHeight() * widthAndHeight;
        return PathObjects.createAnnotationObject(ROIs.createRectangleROI(x, y, width, height));
    }

    private void stylePathObject(PathObject pathObject, DetectedObjects.DetectedObject detection){
        pathObject.setClassification(detection.getClassName());
        double probability = detection.getProbability();

        pathObject.getMeasurements().put("Probability", probability);
        pathObject.getMetadata().put("Probability", Double.toString(probability));
        pathObject.getMetadata().put("Model", modelName);
        if(probability <= 0.60){
            pathObject.setColor(255, 0, 0); // -65536
            redCounter += 1;
            totalAnnotations += 1;
            //PredictionSummary.incrementRedAnnotations();
        }
        else if((probability > 0.60) && (probability <= 0.80)){
            pathObject.setColor(255, 128, 0); // -32768
            orangeCounter += 1;
            totalAnnotations += 1;
            //PredictionSummary.incrementOrangeAnnotations();
        }
        else if((probability > 0.80) && (probability <= 0.90)){
            pathObject.setColor(255, 255, 0); // -256
            yellowCounter += 1;
            totalAnnotations += 1;
            //PredictionSummary.incrementYellowAnnotations();
        }
        else {
            pathObject.setColor(0, 255, 0);
            greenCounter += 1;
            totalAnnotations += 1;
            //PredictionSummary.incrementGreenAnnotations();
        }
    }
}