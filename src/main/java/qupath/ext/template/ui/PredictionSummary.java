package qupath.ext.template.ui;

import javafx.beans.property.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PredictionSummary {
    private static final Logger logger = LoggerFactory.getLogger(PredictionSummary.class);

    private static final LongProperty timeUsageRawValue = new SimpleLongProperty();

    private static final StringProperty timeSpent = new SimpleStringProperty();

    private static final IntegerProperty totalAnnotations = new SimpleIntegerProperty();

    private static final IntegerProperty redAnnotations = new SimpleIntegerProperty();

    private static final IntegerProperty orangeAnnotations = new SimpleIntegerProperty();

    private static final IntegerProperty yellowAnnotations = new SimpleIntegerProperty();

    private static final IntegerProperty greenAnnotations = new SimpleIntegerProperty();

    private static final StringProperty areaCoveredByCurrentRun = new SimpleStringProperty();

    private static final StringProperty totalAreaCovered = new SimpleStringProperty();

    private PredictionSummary(){}

    public static void initializeFields(){
        timeUsageRawValue.set(0);
        totalAnnotations.set(0);
        redAnnotations.set(0);
        orangeAnnotations.set(0);
        yellowAnnotations.set(0);
        greenAnnotations.set(0);
        logger.info("All fields initialized to 0");
    }

    public static void setTimeUsageRawValue(long executionTime){
        timeUsageRawValue.set(executionTime);
        handleStringFormatting(executionTime);
//        logger.info("Time spent set to: {}", executionTime * 0.000000001);
    }

    private static void handleStringFormatting(long executionTime) {
        double timeInSeconds = executionTime * 0.000000001;

        if(timeInSeconds <= 60){
            timeSpent.set(String.format("%.2f s", timeInSeconds));
            //timeSpent.set(timeInSeconds + " s");
//                timeSpent.textProperty().bind(PredictionSummary.timeSpentProperty().multiply(0.000000001)
//                        .asString("Time spent: %.3f s"));
        }
        else if(timeInSeconds > 60 && timeInSeconds <= 3600){
            int minutes = (int) Math.floor(timeInSeconds/60);
            int seconds = (int) Math.floor(timeInSeconds - (minutes * 60));
//            timeSpent.set(minutes + " m, " + seconds + " s");
            timeSpent.set(String.format("%d m %d s", minutes, seconds));
        }
        else{
            int hours = (int) Math.floor(timeInSeconds/3600);
            int minutes = (int) Math.floor(timeInSeconds - (hours * 3600));
            int seconds = (int) Math.floor(timeInSeconds - (minutes * 60));
//            timeSpent.set(hours + " h, " + minutes + " m, " + seconds + " s");
            timeSpent.set(String.format("%d h %d m %d s", hours, minutes, seconds));
        }
    }

    public static void setRedAnnotations(int red){
        redAnnotations.set(red);
    }

    public static void setOrangeAnnotations(int orange){
        orangeAnnotations.set(orange);
    }

    public static void setYellowAnnotations(int yellow){
        yellowAnnotations.set(yellow);
    }

    public static void setGreenAnnotations(int green){
        greenAnnotations.set(green);
    }

    public static void setAreaCoveredByCurrentRun(double areaCovered){
        areaCoveredByCurrentRun.set(String.format("%.2f %%", areaCovered));
    }

    public static void setTotalAnnotations(int totalNumberOfAnnotations){
        totalAnnotations.set(totalNumberOfAnnotations);
    }

//    public static void incrementRedAnnotations(){
//        redAnnotations.set(redAnnotations.get() + 1);
//        incrementTotalAnnotations();
//        logger.info("Increased red annotations to: {}", getRedAnnotations());
//    }
//
//    public static void incrementOrangeAnnotations(){
//        orangeAnnotations.set(orangeAnnotations.get() + 1);
//        incrementTotalAnnotations();
//        logger.info("Increased orange annotations to: {}", getOrangeAnnotations());
//    }
//
//    public static void incrementYellowAnnotations(){
//        yellowAnnotations.set(yellowAnnotations.get() + 1);
//        incrementTotalAnnotations();
//        logger.info("Increased yellow annotations to: {}", getYellowAnnotations());
//    }
//
//    public static void incrementGreenAnnotations(){
//        greenAnnotations.set(greenAnnotations.get() + 1);
//        incrementTotalAnnotations();
//        logger.info("Increased green annotations to: {}", getGreenAnnotations());
//    }
//
//    private static void incrementTotalAnnotations(){
//        totalAnnotations.set(totalAnnotations.get() + 1);
//    }

    public static String getTotalAreaCovered(){
        return totalAreaCovered.get();
    }

    public static StringProperty totalAreaCoveredProperty(){
        return totalAreaCovered;
    }

    public static void setTotalAreaCovered(double areaCovered){
        totalAreaCovered.set(String.format("%.2f %%", areaCovered));
    }

    public static long getTimeUsageRawValue() {
        return timeUsageRawValue.get();
    }

    public static LongProperty timeUsageRawValueProperty() {
        return timeUsageRawValue;
    }

    public static String getTimeSpent() {
        return timeSpent.get();
    }

    public static StringProperty timeSpentProperty() {
        return timeSpent;
    }

    public static int getTotalAnnotations() {
        return totalAnnotations.get();
    }

    public static IntegerProperty totalAnnotationsProperty() {
        return totalAnnotations;
    }

    public static int getRedAnnotations() {
        return redAnnotations.get();
    }

    public static IntegerProperty redAnnotationsProperty() {
        return redAnnotations;
    }

    public static int getOrangeAnnotations() {
        return orangeAnnotations.get();
    }

    public static IntegerProperty orangeAnnotationsProperty() {
        return orangeAnnotations;
    }

    public static int getYellowAnnotations() {
        return yellowAnnotations.get();
    }

    public static IntegerProperty yellowAnnotationsProperty() {
        return yellowAnnotations;
    }

    public static int getGreenAnnotations() {
        return greenAnnotations.get();
    }

    public static IntegerProperty greenAnnotationsProperty() {
        return greenAnnotations;
    }

    public static String getAreaCoveredByCurrentRun() {
        return areaCoveredByCurrentRun.get();
    }

    public static StringProperty areaCoveredByCurrentRunProperty() {
        return areaCoveredByCurrentRun;
    }
}
