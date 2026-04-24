package qupath.ext.template.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ResourceBundle;

public class ResultsController extends VBox {
    private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.ext.template.ui.strings");
    private static final Logger logger = LoggerFactory.getLogger(ResultsController.class);

    @FXML
    Label timeSpent;

    @FXML
    Label totalAnnotations;

    @FXML
    Label redAnnotations;

    @FXML
    Label orangeAnnotations;

    @FXML
    Label yellowAnnotations;

    @FXML
    Label greenAnnotations;

    @FXML
    Label areaCoveredPercentage;

    @FXML
    Label totalAreaCovered;

    public static ResultsController createInstance() throws IOException {
        return new ResultsController();
    }

    private ResultsController() throws IOException {
        var url = ResultsController.class.getResource("resultsWindow.fxml");
        FXMLLoader loader = new FXMLLoader(url, resources);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();

        handleBindings();
    }

    private void handleBindings(){
        bindTimeSpent();
        bindAnnotations();
        bindAreaCovered();
    }

    private void bindTimeSpent() {
        timeSpent.textProperty().bind(PredictionSummary.timeSpentProperty());
    }

    private void bindAnnotations() {
        redAnnotations.textProperty().bind(PredictionSummary.redAnnotationsProperty().asString());
        orangeAnnotations.textProperty().bind(PredictionSummary.orangeAnnotationsProperty().asString());
        yellowAnnotations.textProperty().bind(PredictionSummary.yellowAnnotationsProperty().asString());
        greenAnnotations.textProperty().bind(PredictionSummary.greenAnnotationsProperty().asString());
        totalAnnotations.textProperty().bind(PredictionSummary.totalAnnotationsProperty().asString());
    }

    private void bindAreaCovered() {
        areaCoveredPercentage.textProperty().bind(PredictionSummary.areaCoveredByCurrentRunProperty());
        totalAreaCovered.textProperty().bind(PredictionSummary.totalAreaCoveredProperty());
    }
}
