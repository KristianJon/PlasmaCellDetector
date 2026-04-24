package qupath.ext.template.services;

import qupath.ext.template.ui.dto.comparisonBetweenPathObjects;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.objects.PathObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ActiveLearningService {
    private static final List<PathObject> allPredictions = new ArrayList<>();
    private ActiveLearningService(){}

    public static List<PathObject> fetchPredictionsMadeByModel(boolean reviewMode){
        if(reviewMode){
            fetchUnprocessedPredictions();
        } else{
            fetchProcessedPredictions();
        }
        return sortPredictionsBasedOnNearestNeighbors();
    }

    private static void fetchProcessedPredictions() {
        allPredictions.clear();
        for(PathObject pathObject : QuPathGUI.getInstance().getImageData().getHierarchy().getAnnotationObjects()){
            double prob = pathObject.getMeasurementList().get("Probability");
            if(!(Double.isNaN(prob))){
                String verified = pathObject.getClassification();
                if(Objects.equals(verified, "plasma cell")){
                    allPredictions.add(pathObject);
                }
            }
        }
        sortPredictionsMadeByModel();
    }

    private static void fetchUnprocessedPredictions() {
        allPredictions.clear();
        for(PathObject obj : QuPathGUI.getInstance().getImageData().getHierarchy().getAnnotationObjects()){
            String verified = obj.getClassification();
            if(Objects.equals(verified, "Verified plasma cell") ||
                    Objects.equals(verified, "Not plasma cell")) {
                allPredictions.add(obj);
            }
        }
        sortPredictionsMadeByModel();
    }

    private static void sortPredictionsMadeByModel(){
        allPredictions.sort(Comparator.comparingDouble(a -> Double.parseDouble(a.getMetadata().get("Probability"))));
    }

    private static List<PathObject> sortPredictionsBasedOnNearestNeighbors() {
        return findNearestNeighborsToPathObject();
    }

    private static ArrayList<PathObject> findNearestNeighborsToPathObject() {
        ArrayList<PathObject> finalVersion = new ArrayList<>();
        while(!allPredictions.isEmpty()){
            PathObject lowestConfidenceObject = allPredictions.getFirst();
            ArrayList<comparisonBetweenPathObjects> comparisonWithLowestConfidenceObject = getComparisonBetweenPathObjects(lowestConfidenceObject);
            comparisonWithLowestConfidenceObject.sort(Comparator.comparingDouble(comparisonBetweenPathObjects::difference));
            allPredictions.remove(lowestConfidenceObject);
            int iterationStop = comparisonWithLowestConfidenceObject.size() < 25 ? comparisonWithLowestConfidenceObject.size() : 24;
            finalVersion.add(lowestConfidenceObject);
            for(int i = 0; i < iterationStop; i++){
                allPredictions.remove(comparisonWithLowestConfidenceObject.get(i).pathObject());
                finalVersion.add(comparisonWithLowestConfidenceObject.get(i).pathObject());
            }
        }
        return finalVersion;
    }

    private static ArrayList<comparisonBetweenPathObjects> getComparisonBetweenPathObjects(PathObject lowestConfidenceObject) {
        ArrayList<comparisonBetweenPathObjects> comparisonWithLowestConfidenceObject = new ArrayList<>();
        for(PathObject pathObject : allPredictions){
            if(pathObject == lowestConfidenceObject) continue;
            double differenceBetweenXCoordinates = Math.abs(lowestConfidenceObject.getROI().getBoundsX() - pathObject.getROI().getBoundsX());
            double differenceBetweenYCoordinates = Math.abs(lowestConfidenceObject.getROI().getBoundsY() - pathObject.getROI().getBoundsY());
            double averageBetweenXandYCoordinates = (differenceBetweenXCoordinates + differenceBetweenYCoordinates)/2;
            comparisonWithLowestConfidenceObject.add(new comparisonBetweenPathObjects(pathObject, averageBetweenXandYCoordinates));
        }
        return comparisonWithLowestConfidenceObject;
    }
}
