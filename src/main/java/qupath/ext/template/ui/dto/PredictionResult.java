package qupath.ext.template.ui.dto;

/**
 * Record class used to hold summary details after running prediction on selected
 * annotation(s)
 * <p>
 * The class is used on prediction controller during inference. Each value is bound to the fields
 * of the class "PredictionSummary" after annotations have been processed
 * <p>
 */
public record PredictionResult(long time, int red, int orange, int yellow, int green, double areaOfCurrentSelection, double totalAreaCovered, int totalAnnotation) {
}
