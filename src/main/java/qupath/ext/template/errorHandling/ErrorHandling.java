package qupath.ext.template.errorHandling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.template.stageManager.StageManager;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.objects.PathObject;

import java.util.ResourceBundle;

/**
 * This class performs checks and creates appropriate dialog boxes in QuPath if anything goes wrong. For instance,
 * if a user tries to start active learning without a project loaded, an error will be raises
 */
public class ErrorHandling {

    /**
     * Logger for ErrorHandling class, this is used to display both information and error in QuPath's log
     */
    private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.ext.template.ui.strings");

    /**
     * Logger for DemoExtension class, this is used to display both information and error in QuPath's log
     */
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandling.class);

    /**
     * Private constructor for to stop instantiation
     */
    private ErrorHandling(){}

    /**
     * Checks if an image is opened (double-clicked) in QuPath, will use predefined error message
     * with QuPath's Dialogs library
     *
     * @return boolean (true if image is present, false otherwise)
     */
    public static boolean isImagePresent(){
        if(QuPathGUI.getInstance().getImageData() == null){
            Dialogs.showErrorMessage(resources.getString("error.image"), resources.getString("error.image.description"));
            return false;
        }
        return true;
    }

    /**
     * Checks if selection is made, this is required before inference can be performed. Uses QuPath's custom Dialogs
     * library to inform user
     *
     * @return boolean (true if selection is present, false otherwise)
     */
    public static boolean isSelectionPresent(){
        if(QuPathGUI.getInstance().getImageData().getHierarchy().getSelectionModel().getSelectedObjects().isEmpty()){
            Dialogs.showErrorMessage(resources.getString("error.annotations"), resources.getString("error.annotations.description"));
            return false;
        }
        return true;
    }

    /**
     * Checks if predictions are available in the current project, it will also have to check if image is present
     * before annotations can be iterated through. Uses QuPath's custom Dialogs library to inform user
     *
     * @return boolean (true if prediction is present, false otherwise)
     */
    public static boolean isPredictionsPresent(){
        if(!isImagePresent()) return false;

        for(PathObject pathObject : QuPathGUI.getInstance().getImageData().getHierarchy().getAnnotationObjects()){
            double prob = pathObject.getMeasurementList().get("Probability");
            if(!(Double.isNaN(prob))){
                return true;
            }
        }
        Dialogs.showErrorMessage("Active learning workflow", "There are no predictions in current project");
        return false;
    }

    /**
     * Checks if predictions are available in the current project, it will also have to check if image is present
     * before annotations can be iterated through. Uses QuPath's custom Dialogs library to inform user
     *
     * @return boolean (true if prediction is present, false otherwise)
     */
    public static boolean isImageLessThanChosenDimensions(PathObject annotation, int imageSize){
        if((annotation.getROI().getBoundsHeight() < imageSize) || (annotation.getROI().getBoundsWidth() < imageSize)){
            Dialogs.showErrorMessage("Error with annotation size", "Height or width is less than " + imageSize);
            return true;
        }
        return false;
    }

    /**
     * Checks if the received path object is a model prediction by searching through the measurement list and finding
     * the property "Probability". This will only be available from predictions made through this extension
     *
     * @param pathObject Annotation (PathObject) needed to be checked
     * @return boolean (true if path object is model prediction, false otherwise)
     */
    public static boolean isPathObjectModelPrediction(PathObject pathObject){
        double prob = pathObject.getMeasurementList().get("Probability");
        if(!(Double.isNaN(prob))){
            return true;
        }
        logger.info("Selected path object is not a model prediction, continuing...");
        return false;
    }

    /**
     * Checks if the received path object has a classification of "verified plasma cell" or "not plasma cell", in that
     * case an expert has reviewed them
     *
     * @param pathObject Annotation (PathObject) needed to be checked
     * @return boolean (true if path object is processed, false otherwise)
     */
    public static boolean isPathObjectAlreadyProcessed(PathObject pathObject){
        String classification = pathObject.getClassification();
        if(classification.equals("Verified plasma cell") || classification.equals("Not plasma cell")){
            logger.info("Selected path object already processed");
            return true;
        }
        logger.info("Selected path object is not processed");
        return false;
    }
}