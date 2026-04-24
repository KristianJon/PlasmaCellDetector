import ai.djl.modality.cv.output.Rectangle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.template.djl.Detection;
import qupath.ext.template.djl.Yolo11Translator;

import java.util.ArrayList;
import java.util.Comparator;

public class TranslatorTest {
    private final float iouThreshold = 0.5f;
    private final float confThreshold = 0.4f;
    private final int imageSize = 640;

    /**
     * TESTS FOR IOU
     */

    @Test
    public void testIOUOf0(){
        Yolo11Translator yolo11Translator = new Yolo11Translator(confThreshold, iouThreshold, imageSize);

        Rectangle rectangle1 =
                new Rectangle(0.0/imageSize, 0.0/imageSize, 10.0/imageSize, 10.0/imageSize);

        Rectangle rectangle2 =
                new Rectangle(20.0/imageSize, 20.0/imageSize, 10.0/imageSize, 10.0/imageSize);

        double iou = yolo11Translator.calculateIOU(rectangle1, rectangle2);

        Assertions.assertEquals(0.0, iou);
    }

    @Test
    public void testIOUOf1(){
        Yolo11Translator yolo11Translator = new Yolo11Translator(confThreshold, iouThreshold, imageSize);

        Rectangle rectangle1 =
                new Rectangle(0.0/imageSize, 0.0/imageSize, 10.0/imageSize, 10.0/imageSize);

        Rectangle rectangle2 =
                new Rectangle(0.0/imageSize, 0.0/imageSize, 10.0/imageSize, 10.0/imageSize);

        double iou = yolo11Translator.calculateIOU(rectangle1, rectangle2);

        Assertions.assertEquals(1.0, iou);
    }

    @Test
    public void testIOUOf33(){
        Yolo11Translator yolo11Translator = new Yolo11Translator(confThreshold, iouThreshold, imageSize);

        Rectangle rectangle1 =
                new Rectangle(0.0/imageSize, 0.0/imageSize, 10.0/imageSize, 10.0/imageSize);

        Rectangle rectangle2 =
                new Rectangle(5.0/imageSize, 0.0/imageSize, 10.0/imageSize, 10.0/imageSize);

        double iou = yolo11Translator.calculateIOU(rectangle1, rectangle2);
        iou = Math.floor(iou * 100)/100;

        Assertions.assertEquals(0.33, iou);
    }

    @Test
    public void testIOUOf28(){
        Yolo11Translator yolo11Translator = new Yolo11Translator(confThreshold, iouThreshold, imageSize);

        Rectangle rectangle1 =
                new Rectangle(0.0/imageSize, 0.0/imageSize, 10.0/imageSize, 10.0/imageSize);

        Rectangle rectangle2 =
                new Rectangle(5.0/imageSize, 0.0/imageSize, 10.0/imageSize, 8.0/imageSize);

        //double iou = rectangle2.getIoU(rectangle1);

        double iou = yolo11Translator.calculateIOU(rectangle1, rectangle2);

        iou = (Math.floor(iou*100)/100);

        Assertions.assertEquals(0.28, iou);
    }

    @Test
    public void testIOUOf50(){
        Yolo11Translator yolo11Translator = new Yolo11Translator(confThreshold, iouThreshold, imageSize);

        Rectangle rectangle1 =
                new Rectangle(0.0/imageSize, 0.0/imageSize, 10.0/imageSize, 10.0/imageSize);

        Rectangle rectangle2 =
                new Rectangle(5.0/imageSize, 0.0/imageSize, 5.0/imageSize, 10.0/imageSize);

        //double iou = rectangle2.getIoU(rectangle1);

        double iou = yolo11Translator.calculateIOU(rectangle1, rectangle2);

        //iou = (Math.floor(iou*100)/100);

        Assertions.assertEquals(0.5, iou);
    }

    @Test
    public void testIOUOf0WithTouchingEdge(){
        Yolo11Translator yolo11Translator = new Yolo11Translator(confThreshold, iouThreshold, imageSize);

        Rectangle rectangle1 =
                new Rectangle(0.0/imageSize, 0.0/imageSize, 10.0/imageSize, 10.0/imageSize);

        Rectangle rectangle2 =
                new Rectangle(10.0/imageSize, 0.0/imageSize, 10.0/imageSize, 10.0/imageSize);

        //double iou = rectangle2.getIoU(rectangle1);

        double iou = yolo11Translator.calculateIOU(rectangle1, rectangle2);

        //iou = (Math.floor(iou*100)/100);

        Assertions.assertEquals(0.0, iou);
    }

    @Test
    public void testIOUOf0WithTouchingCorner(){
        Yolo11Translator yolo11Translator = new Yolo11Translator(confThreshold, iouThreshold, imageSize);

        Rectangle rectangle1 =
                new Rectangle(0.0/imageSize, 0.0/imageSize, 10.0/imageSize, 10.0/imageSize);

        Rectangle rectangle2 =
                new Rectangle(10.0/imageSize, 10.0/imageSize, 10.0/imageSize, 10.0/imageSize);

        //double iou = rectangle2.getIoU(rectangle1);

        double iou = yolo11Translator.calculateIOU(rectangle1, rectangle2);

        //iou = (Math.floor(iou*100)/100);

        Assertions.assertEquals(0.0, iou);
    }

    @Test
    public void testIOUOfBoxAInsideBoxB(){
        Yolo11Translator yolo11Translator = new Yolo11Translator(confThreshold, iouThreshold, imageSize);

        Rectangle rectangle1 =
                new Rectangle(0.0/imageSize, 0.0/imageSize, 10.0/imageSize, 10.0/imageSize);

        Rectangle rectangle2 =
                new Rectangle(2.0/imageSize, 2.0/imageSize, 4.0/imageSize, 4.0/imageSize);

        //double iou = rectangle2.getIoU(rectangle1);

        double iou = yolo11Translator.calculateIOU(rectangle1, rectangle2);

        //iou = (Math.floor(iou*100)/100);

        Assertions.assertEquals(0.16, iou);
    }

    @Test
    public void testIOUOfBoxBInsideBoxA(){
        Yolo11Translator yolo11Translator = new Yolo11Translator(confThreshold, iouThreshold, imageSize);
        Rectangle rectangle1 =
                new Rectangle(2.0/imageSize, 2.0/imageSize, 4.0/imageSize, 4.0/imageSize);

        Rectangle rectangle2 =
                new Rectangle(0.0/imageSize, 0.0/imageSize, 10.0/imageSize, 10.0/imageSize);

        //double iou = rectangle2.getIoU(rectangle1);

        double iou = yolo11Translator.calculateIOU(rectangle1, rectangle2);

        //iou = (Math.floor(iou*100)/100);

        Assertions.assertEquals(0.16, iou);
    }

    @Test
    public void testIOUOfBoxWithNegativeCoordinates(){
        Yolo11Translator yolo11Translator = new Yolo11Translator(confThreshold, iouThreshold, imageSize);
        Rectangle rectangle1 =
                new Rectangle(-5.0/imageSize, 0.0/imageSize, 10.0/imageSize, 10.0/imageSize);

        Rectangle rectangle2 =
                new Rectangle(0.0/imageSize, 0.0/imageSize, 5.0/imageSize, 10.0/imageSize);

        //double iou = rectangle2.getIoU(rectangle1);

        double iou = yolo11Translator.calculateIOU(rectangle1, rectangle2);

        //iou = (Math.floor(iou*100)/100);

        Assertions.assertEquals(0.5, iou);
    }

    @Test
    public void testIOUOfMoreThan50(){
        Yolo11Translator yolo11Translator = new Yolo11Translator(confThreshold, iouThreshold, imageSize);
        Rectangle rectangle1 =
                new Rectangle(90.0/imageSize, 90.0/imageSize,
                        20.0/imageSize, 20.0/imageSize);

        Rectangle rectangle2 =
                new Rectangle(90.0/imageSize, 90.0/imageSize,
                        20.0/imageSize, 12.0/imageSize);

        //double iou = rectangle2.getIoU(rectangle1);

        double iou = yolo11Translator.calculateIOU(rectangle1, rectangle2);

        //iou = (Math.floor(iou*100)/100);

        Assertions.assertEquals(0.6, iou);
    }





    /**
     * TESTS FOR NMS
     */

    @Test
    public void testNMSAcrossThreeOverlappingBoxesWithIOUThreshold05(){
        Yolo11Translator yolo11Translator = new Yolo11Translator(confThreshold, 0.5f, imageSize);
        ArrayList<Detection> allDetections = new ArrayList<>();

        // Add box with 0.9 conf to list of detections
        allDetections.add(
                new Detection("plasma cell",
                        0.9,
                        new Rectangle(
                                90.0/imageSize, 90.0/imageSize,
                                20.0/imageSize, 20.0/imageSize
                        )));
        // Add box with lower conf and IOU value of 0.75
        allDetections.add(
                new Detection("plasma cell",
                        0.8,
                        new Rectangle(
                                90.0/imageSize, 90.0/imageSize,
                                20.0/imageSize, 15.0/imageSize
                        )));

        // Add box with lower conf and IOU value of 0.6
        allDetections.add(
                new Detection("plasma cell",
                        0.7,
                        new Rectangle(
                                90.0/imageSize, 90.0/imageSize,
                                20.0/imageSize, 12.0/imageSize
                        )));

        allDetections.sort(Comparator.comparingDouble(Detection::probability).reversed());

        ArrayList<Detection> results = yolo11Translator.NMS(allDetections);

        Assertions.assertEquals(1, results.size());
    }

    @Test
    public void testNMSAcrossThreeOverlappingBoxesAndThreeAdditionalOverlappingBoxesWithIOUThreshold05(){
        Yolo11Translator yolo11Translator = new Yolo11Translator(confThreshold, 0.5f, imageSize);
        ArrayList<Detection> allDetections = new ArrayList<>();

        // Add box with 0.9 conf to list of detections (should be kept)
        allDetections.add(
                new Detection("plasma cell",
                        0.9,
                        new Rectangle(
                                90.0/imageSize, 90.0/imageSize,
                                20.0/imageSize, 20.0/imageSize
                        )));
        // Add box with lower conf and IOU value of 0.75 (should be removed)
        allDetections.add(
                new Detection("plasma cell",
                        0.8,
                        new Rectangle(
                                90.0/imageSize, 90.0/imageSize,
                                20.0/imageSize, 15.0/imageSize
                        )));

        // Add box with lower conf and IOU value of 0.6 (should be removed)
        allDetections.add(
                new Detection("plasma cell",
                        0.7,
                        new Rectangle(
                                90.0/imageSize, 90.0/imageSize,
                                20.0/imageSize, 12.0/imageSize
                        )));

        // Add new box with conf 0.8 (should be kept)
        allDetections.add(
                new Detection("plasma cell",
                        0.8,
                        new Rectangle(
                                20.0/imageSize, 20.0/imageSize,
                                20.0/imageSize, 20.0/imageSize
                        )));
        // Add box with 0.5 conf and IOU between this and previous box of 0.5 (should be removed)
        allDetections.add(
                new Detection("plasma cell",
                        0.5,
                        new Rectangle(
                                30.0/imageSize, 20.0/imageSize,
                                10.0/imageSize, 20.0/imageSize
                        )));

        // Add box with lower conf and IOU of 0.33 (should be kept)
        allDetections.add(
                new Detection("plasma cell",
                        0.6,
                        new Rectangle(
                                30.0/imageSize, 20.0/imageSize,
                                20.0/imageSize, 20.0/imageSize
                        )));

        allDetections.sort(Comparator.comparingDouble(Detection::probability).reversed());

        ArrayList<Detection> results = yolo11Translator.NMS(allDetections);
        System.out.println(results);

        Assertions.assertEquals(4, results.size());
    }

    @Test
    public void testNMSAcrossBoxesWithIOUThreshold30(){
        Yolo11Translator yolo11Translator = new Yolo11Translator(confThreshold, 0.3f, imageSize);
        ArrayList<Detection> allDetections = new ArrayList<>();

        // Add box with 0.9 conf to list of detections (should be kept)
        allDetections.add(
                new Detection("plasma cell",
                        0.9,
                        new Rectangle(
                                90.0/imageSize, 90.0/imageSize,
                                20.0/imageSize, 20.0/imageSize
                        )));
        // Add box with lower conf and IOU value of 0.75 (should be removed)
        allDetections.add(
                new Detection("plasma cell",
                        0.8,
                        new Rectangle(
                                90.0/imageSize, 90.0/imageSize,
                                20.0/imageSize, 15.0/imageSize
                        )));

        // Add box with lower conf and IOU value of 0.6 (should be removed)
        allDetections.add(
                new Detection("plasma cell",
                        0.7,
                        new Rectangle(
                                90.0/imageSize, 90.0/imageSize,
                                20.0/imageSize, 12.0/imageSize
                        )));

        // Add new box with conf 0.8 (should be kept)
        allDetections.add(
                new Detection("plasma cell",
                        0.8,
                        new Rectangle(
                                20.0/imageSize, 20.0/imageSize,
                                20.0/imageSize, 20.0/imageSize
                        )));
        // Add box with 0.5 conf and IOU between this and previous box of 0.5 (should be removed)
        allDetections.add(
                new Detection("plasma cell",
                        0.5,
                        new Rectangle(
                                30.0/imageSize, 20.0/imageSize,
                                10.0/imageSize, 20.0/imageSize
                        )));

        // Add box with lower conf and IOU of 0.33 (should be kept)
        allDetections.add(
                new Detection("plasma cell",
                        0.6,
                        new Rectangle(
                                30.0/imageSize, 20.0/imageSize,
                                20.0/imageSize, 20.0/imageSize
                        )));

        allDetections.sort(Comparator.comparingDouble(Detection::probability).reversed());

        ArrayList<Detection> results = yolo11Translator.NMS(allDetections);
        System.out.println(results);

        Assertions.assertEquals(2, results.size());
    }
}
