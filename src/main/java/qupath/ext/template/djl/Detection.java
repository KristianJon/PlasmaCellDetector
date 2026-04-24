package qupath.ext.template.djl;

import ai.djl.modality.cv.output.Rectangle;

/**
 * record class for storing detections, used in the custom translator (Yolo11Translator)
 *
 * @param className
 * @param probability
 * @param boundingBox
 */
public record Detection(String className, double probability, Rectangle boundingBox) {
}
