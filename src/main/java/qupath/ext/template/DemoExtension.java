package qupath.ext.template;

import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.template.errorHandling.ErrorHandling;
import qupath.ext.template.stageManager.StageManager;
import qupath.ext.template.ui.ActiveLearningController;
import qupath.ext.template.ui.PredictionController;
import qupath.fx.dialogs.Dialogs;
import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.viewer.QuPathViewer;
import qupath.lib.gui.viewer.QuPathViewerListener;
import qupath.lib.images.ImageData;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.classes.PathClass;
import qupath.lib.objects.hierarchy.events.PathObjectSelectionListener;
import qupath.lib.objects.hierarchy.events.PathObjectSelectionModel;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.ResourceBundle;


/**
 * This is a demo to provide a template for creating a new QuPath extension.
 * <p>
 * It doesn't do much - it just shows how to add a menu item and a preference.
 * See the code and comments below for more info.
 * <p>
 * <b>Important!</b> For your extension to work in QuPath, you need to make sure the name &amp; package
 * of this class is consistent with the file
 * <pre>
 *     /resources/META-INF/services/qupath.lib.gui.extensions.QuPathExtension
 * </pre>
 */
public class DemoExtension implements QuPathExtension {
	/**
	 * A resource bundle containing all the text used by the extension. This may be useful for translation to other languages.
	 * Note that this is optional, and you can define the text within the code and FXML files that you use.
	 */
	private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.ext.template.ui.strings");

	/**
	 * Logger for DemoExtension class, this is used to display both information and error in QuPath's log
	 */
	private static final Logger logger = LoggerFactory.getLogger(DemoExtension.class);

	/**
	 * Name for extension
	 */
	private static final String EXTENSION_NAME = resources.getString("name");

	/**
	 * Short description, used under 'Extensions > Installed extensions'
	 */
	private static final String EXTENSION_DESCRIPTION = resources.getString("description");

	/**
	 * QuPath version that the extension is designed to work with.
	 * This allows QuPath to inform the user if it seems to be incompatible.
	 */
	private static final Version EXTENSION_QUPATH_VERSION = Version.parse("v0.6.0");

	/**
	 * Flag whether the extension is already installed (might not be needed... but we'll do it anyway)
	 */
	private boolean isInstalled = false;

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

	@Override
	public void installExtension(QuPathGUI qupath) {
		if (isInstalled) {
			logger.debug("{} is already installed", getName());
			return;
		}
		isInstalled = true;
		addMenuItem(qupath);
        createClassificationsInQuPath();
        registerMouseClicksOnPredictionBoxes();
	}

    public static void createClassificationsInQuPath() {
        PathClass verifiedPlasmaCellClass = PathClass.getInstance("Verified plasma cell");

        PathClass notPlasmaCellClass = PathClass.getInstance("Not plasma cell");

        var existingPathClasses = QuPathGUI.getInstance().getAvailablePathClasses();

        if(!existingPathClasses.contains(verifiedPlasmaCellClass)){
            existingPathClasses.add(verifiedPlasmaCellClass);
            logger.info("Class 'verified plasma cell' was not persisted");
        }

        if(!existingPathClasses.contains(notPlasmaCellClass)){
            existingPathClasses.add(notPlasmaCellClass);
            logger.info("Class 'not plasma cell' was not persisted");
        }
    }


    /**
	 * Demo showing how a new command can be added to a QuPath menu.
	 * @param qupath The QuPath GUI
	 */
	private void addMenuItem(QuPathGUI qupath) {
		var menu = qupath.getMenu("Extensions>" + EXTENSION_NAME, true);
        MenuItem activeLearningItem = new MenuItem("Predictions and active learning");
		//MenuItem predictionItem = new MenuItem("Prediction");
        //predictionItem.setOnAction(e -> StageManager.getInstance().createPredictionStage());
        activeLearningItem.setOnAction(e -> StageManager.getInstance().createActiveLearningStage());
//		predictionItem.setOnAction(e -> createPredictionStage());
//        activeLearningItem.setOnAction(e -> createActiveLearningStage());
		menu.getItems().addAll(activeLearningItem);
	}

    private void registerMouseClicksOnPredictionBoxes() {
//        PathObjectSelectionModel selectionModel = QuPathGUI.getInstance().getImageData().getHierarchy().getSelectionModel();
//
//        selectionModel.addPathObjectSelectionListener(new PathObjectSelectionListener() {
//            @Override
//            public void selectedPathObjectChanged(PathObject pathObjectSelected, PathObject previousObject, Collection<PathObject> allSelected) {
//                logger.info("Newly selected object from the main window: {}", pathObjectSelected.getID());
//            }
//        });
//        QuPathGUI.getInstance().getViewer().addViewerListener(new QuPathViewerListener() {
//
//            @Override
//            public void imageDataChanged(QuPathViewer viewer, ImageData<BufferedImage> imageDataOld, ImageData<BufferedImage> imageDataNew) {
//
//            }
//
//            @Override
//            public void visibleRegionChanged(QuPathViewer viewer, Shape shape) {
//
//            }
//
//            @Override
//            public void selectedObjectChanged(QuPathViewer viewer, PathObject pathObjectSelected) {
//                if(!(pathObjectSelected == null)) logger.info("Newly selected object from main window: {}", pathObjectSelected.getID());
//            }
//
//            @Override
//            public void viewerClosed(QuPathViewer viewer) {
//
//            }
//        });
    }

    @Override
	public String getName() {
		return EXTENSION_NAME;
	}

	@Override
	public String getDescription() {
		return EXTENSION_DESCRIPTION;
	}
	
	@Override
	public Version getQuPathVersion() {
		return EXTENSION_QUPATH_VERSION;
	}
}