package qupath.ext.template.djl;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.BoundingBox;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.modality.cv.output.Rectangle;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;

/**
 * A translator that performs pre- and post-processing steps in relation to model inference. The expected input to
 * pre-processing is an image of ai.djl.modality.cv package and the expected output of post-processing is a
 * collection of detected objects consisting of class name, bounding box and class probabilities
 */
public class Yolo11Translator implements Translator<Image, DetectedObjects> {

    /**
     * variable used to keep reference of expected image dimensions
     */
    private final int imageDimensions;

    /**
     * Logger for Yolo11Translator class, this is used to display both information and error in QuPath's log
     */
    private static final Logger logger = LoggerFactory.getLogger(Yolo11Translator.class);

    /**
     * variable used to filter predictions based on desired confidence threshold
     */
    private final float confThreshold;

    /**
     * variable used to filter predictions based on IoU threshold
     */
    private final float iouThreshold;

//    private long startTime;
//
//    private long stopTime;
//
//    private ArrayList<Long> inferenceTimes = new ArrayList<>();

//    public ArrayList<Long> getInferenceTimes(){
//        return inferenceTimes;
//    }

    public Yolo11Translator(float confThreshold, float iouThreshold, int imageDimensions){
        this.confThreshold = confThreshold;
        this.iouThreshold = iouThreshold;
        this.imageDimensions = imageDimensions;
    }

    /**
     * Receives image to process along with translator context
     *
     * @param translatorContext context properties such as NDManager
     * @param image image to be processed
     * @return NDList image data in appropriate data structure
     */
    @Override
    public NDList processInput(TranslatorContext translatorContext, Image image) throws Exception {
        // Need to convert image data to NDArray -> shape will be (width, height, channels) = (640, 640, 3)
        NDArray imageValuesArray = image.toNDArray(translatorContext.getNDManager());

        // The model expects normalized pixel values and the shape (channels, width, height)
        imageValuesArray = imageValuesArray.div(255f).transpose(2, 0, 1);

//        startTime = System.nanoTime();
        // Return the expected data structure of NDList
        return new NDList(imageValuesArray);
    }

    /**
     * Receives list of ALL predictions made by model
     *
     * @param translatorContext context properties
     * @param ndList all predictions made by model
     * @return all predictions left after NMS and confidence filtering
     */
    @Override
    public DetectedObjects processOutput(TranslatorContext translatorContext, NDList ndList) throws Exception {
//        stopTime = System.nanoTime();
//        inferenceTimes.add((stopTime - startTime));
//        logger.info("Inference time: {}", (stopTime - startTime));
        ArrayList<String> classNames = new ArrayList<>(); // holds class names (only one for this model)
        ArrayList<BoundingBox> boundingBoxes = new ArrayList<>(); // bounding box coordinates, expressed through rectangle class
        ArrayList<Double> probabilities = new ArrayList<>(); // probability for the specific prediction

        // https://javadoc.io/static/ai.djl/api/0.4.1/index.html?ai/djl/ndarray/NDList.html
        NDArray array = ndList.singletonOrThrow(); // fetch the array containing predictions

        logger.info("Shape of input list: {}", array.getShape());
        // Perform non-maximum suppression on detection list
        ArrayList<Detection> finalPredictions = NMS(processPredictions(array));

        // Add items from filtered list to appropriate list
        for(Detection detection : finalPredictions){
            classNames.add(detection.className());
            probabilities.add(detection.probability());
            boundingBoxes.add(detection.boundingBox());
        }

        // Return final detections
        return new DetectedObjects(classNames, probabilities, boundingBoxes);
    }

    private ArrayList<Detection> processPredictions(NDArray array){
        ArrayList<Detection> allDetections = new ArrayList<>();
        //int numberOfPredictions = (int) array.getShape().get(1); // get the number of predictions (8400 for instance)

        float[][] predictions = new float[5][(int) array.getShape().get(1)];
        for (int row = 0; row < 5; row++) {
            predictions[row] = array.get(row).toFloatArray();
        }

//        float[][] predictions = new float[(int) array.getShape().get(0)][(int) array.getShape().get(1)];
//        for (int row = 0; row < array.getShape().get(1); row++) {
//            predictions[row] = array.get(row).toFloatArray();
//        }
//
//        logger.info("Array for YOLOv10: {}", predictions.length);
//        for (float[] prediction : predictions) {
//            if (prediction[4] > 0.4) {
//                logger.info("Array for YOLOv10: {}", prediction);
//            }
//        }
        int numberOfPredictions = predictions[0].length;
//        logger.info("Length of temp array: {}", predictions.length);
//        logger.info("Shape of input array: {}", array.getShape());
//        for(int i = 0; i < 300; i++){
//            if(array.get(i).get(4).getFloat() > 0.3){
//                logger.info("Get first row: {}", array.get(i));
//            }
//        }
//
//        logger.info("Device for array in process predictions: {}", array.getDevice());

        // iterate over each prediction (5 rows x 8400 columns for model trained on 640x640)
        // format for each prediction -> objectCenterX, objectCenterY, boxWidth, boxHeight
        for(int i = 0; i < numberOfPredictions; i++){
            if(predictions[4][i] > confThreshold){ // the fifth row (index 4) contains all probabilities

                float objectCenterX = predictions[0][i];//array.getFloat(0, i);
                float objectCenterY = predictions[1][i];//array.getFloat(1, i);
                float boxWidth = predictions[2][i];//array.getFloat(2, i);
                float boxHeight = predictions[3][i];//array.getFloat(3, i);

                // Rectangle object requires top-left corner, not object center -> convert to correct format
                float topLeftCornerX = objectCenterX - (boxWidth/2);
                float topLeftCornerY = objectCenterY - (boxHeight/2);

                // add detections to list
                allDetections.add(
                        new Detection("plasma cell",
                                predictions[4][i],
                                new Rectangle(
                                        topLeftCornerX/imageDimensions, topLeftCornerY/imageDimensions,
                                        boxWidth/imageDimensions, boxHeight/imageDimensions
                                )));
            }
        }

        // Sort detection list
        allDetections.sort(Comparator.comparingDouble(Detection::probability).reversed());
        return allDetections;
    }

    public double calculateIOU(Rectangle box1, Rectangle box2){
        // Find top left and bottom right point of first box
        double topLeftXBox1 = box1.getX() * imageDimensions;
        double topLeftYBox1 = box1.getY() * imageDimensions;
        double bottomRightXBox1 = topLeftXBox1 + (box1.getWidth() * imageDimensions);
        double bottomRightYBox1 = topLeftYBox1 + (box1.getHeight() *imageDimensions);

        // Find top left and bottom right point of second box
        double topLeftXBox2 = box2.getX() * imageDimensions;
        double topLeftYBox2 = box2.getY() * imageDimensions;
        double bottomRightXBox2 = topLeftXBox2 + (box2.getWidth() * imageDimensions);
        double bottomRightYBox2 = topLeftYBox2 + (box2.getHeight() * imageDimensions);

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
        double areaBox1 = (box1.getWidth() * imageDimensions)
                * (box1.getHeight() * imageDimensions);

        double areaBox2 = (box2.getWidth() * imageDimensions)
                * (box2.getHeight() * imageDimensions);

        return intersectionArea/(areaBox1 + areaBox2 - intersectionArea);
    }

    public ArrayList<Detection> NMS(ArrayList<Detection> allDetections){

        ArrayList<Detection> keep = new ArrayList<>();

        while(!allDetections.isEmpty()){
            Detection detection = allDetections.getFirst();
            keep.add(detection);
            allDetections.removeFirst();

            allDetections.removeIf(element -> calculateIOU(detection.boundingBox(), element.boundingBox()) > iouThreshold);
        }

        return keep;
    }
}