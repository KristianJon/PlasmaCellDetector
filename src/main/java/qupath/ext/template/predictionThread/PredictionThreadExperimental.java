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
import java.util.ArrayList;
import java.util.Objects;
import java.util.ResourceBundle;

public class PredictionThreadExperimental extends Task<PredictionResult> {
    private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.ext.template.ui.strings");
    private final PathObject annotation;
    private final int start;
    private final int stop;
    private final int widthAndHeight;
    private int redCounter = 0;
    private int orangeCounter = 0;
    private int yellowCounter = 0;
    private int greenCounter = 0;
    private int totalAnnotations = 0;
    private double areaCoveredByCurrentRun = 0;
    private double totalAreaCovered = 0;
    private long areaOfFullImage;
    private static final Logger logger = LoggerFactory.getLogger(PredictionThreadExperimental.class);

    public PredictionThreadExperimental(PathObject annotation, int widthAndHeight, int start, int stop){
            this.annotation = annotation;
            this.widthAndHeight = widthAndHeight;
            this.start = start;
            this.stop = stop;
    }

    @Override
    protected PredictionResult call() throws Exception {

        areaOfFullImage = (long) QuPathGUI.getInstance().getImageData().getServer().getHeight() * QuPathGUI.getInstance().getImageData().getServer().getWidth();
        long startTime = System.nanoTime();
        //buttonsDisabled(true);
        try (ZooModel<Image, DetectedObjects> model = PredictionController.getCriteria().loadModel();
             Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
                logger.info("Model device: {}", model.getNDManager().getDevice());

                // Iterate through all selected annotations
//                int annotationCounter = 1;
//                for (PathObject annotation : selectedAnnotations) {
                calculateAreaOfSelectedAnnotation(annotation);

//           // Informing user of status
//                updateMessage("Processing annotation: " + annotationCounter + "/" + selectedAnnotations.size());
//                annotationCounter++;

            // Lock the annotation so the user does not move it when scrolling through results
                    annotation.setLocked(true);

                    // Calculate how many rows and columns based on size of selected annotation
                    int numberOfColumns = (int) Math.floor(annotation.getROI().getBoundsWidth() / widthAndHeight);
                    int numberOfRows = (int) Math.floor(annotation.getROI().getBoundsHeight() / widthAndHeight);

                    logger.info("Annotation width: {}, Annotation height: {}, Number of Columns: {}", annotation.getROI().getBoundsWidth(), annotation.getROI().getBoundsHeight(), numberOfColumns);

                    //
                    int totalNumberOfIterations = numberOfColumns * numberOfRows;

//                    logger.info("Number of columns: {}, number of rows: {}", numberOfColumns, numberOfRows);
//                    logger.info("Top left corner of annotation: ({},{})", annotation.getROI().getBoundsX(), annotation.getROI().getBoundsY());
//                    logger.info("Width and height of annotations: ({},{})", annotation.getROI().getBoundsWidth(), annotation.getROI().getBoundsHeight());
//                    logger.info("End of annotation (x, y): ({},{})", annotation.getROI().getBoundsX() + annotation.getROI().getBoundsWidth(),
//                            annotation.getROI().getBoundsY() + annotation.getROI().getBoundsHeight());

                    long counter = 1;

                    // Iterate through each cell
                    for (int i = start; i < stop; i++) {
                        for (int j = 0; j < numberOfColumns; j++) {
                            logger.info("THREAD: {}, Start: {}, Stop: {}", Thread.currentThread(), start, stop);
                            logger.info("THREAD: {}, i (row): {}, j (column): {}", Thread.currentThread(), i, j);
                            ROI currentPatch = calculateROIPatch(annotation, i, j);

                            Image image = fetchImageRegionFromPatch(QuPathGUI.getInstance().getImageData().getServer(), currentPatch);

                            DetectedObjects detection = predictor.predict(image);
                            logger.info("Detections: {}", detection);

                            createAnnotation(currentPatch, annotation, detection);

                            Commands.refreshObjectIDs(QuPathGUI.getInstance().getImageData(), false);

                            updateProgress(counter, totalNumberOfIterations);
                            counter++;
                        }
                    }
                }
            catch (TranslateException | IOException | ModelNotFoundException | MalformedModelException e) {
                Dialogs.showErrorMessage(resources.getString("error.prediction"), resources.getString("error.prediction.description"));
                logger.error(e.getMessage());
            }
            calculateTotalAreaCovered();
            //buttonsDisabled(false);
//            viewResults.setVisible(true);
            long endTime = System.nanoTime();
            return new PredictionResult(endTime - startTime, redCounter, orangeCounter, yellowCounter, greenCounter, areaCoveredByCurrentRun, totalAreaCovered, totalAnnotations);
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
                    //logger.info("Area calculated for object: {}", tempArea);
                    //logger.info("Object: {}", obj);
                    //logger.info("Width, height: {}, {}", obj.getROI().getBoundsWidth(), obj.getROI().getBoundsHeight());
                }
            }
            //logger.info("Total area after iterating: {}", tempArea);
            totalAreaCovered = ((tempArea/areaOfFullImage) * 100);
            //logger.info("Total area after dividing: {}", totalAreaCovered);
            //logger.info("Total area of full image: {}", areaOfFullImage);
        }

//        void buttonsDisabled(boolean bool){
//            modelSelectionButton.setDisable(bool);
//            makePredictionButton.setDisable(bool);
//            imageSizeButton.setDisable(bool);
//            viewResults.setDisable(bool);
//        }
        private ROI calculateROIPatch(PathObject annotation, int i, int j){
            double newTopLeftX = annotation.getROI().getBoundsX() + (widthAndHeight * j);
            double newTopLeftY = annotation.getROI().getBoundsY() + (widthAndHeight * i);
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

                // style pathObject
                stylePathObject(pathObject, detection);

                //logger.info("Chosen color: {}", pathObject.getColor());
                // save annotation
                annotation.addChildObject(pathObject);
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
            //pathObject.getMetadata().put("Model", chosenFile.getName());
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
