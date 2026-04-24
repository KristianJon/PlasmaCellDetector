package qupath.ext.template.services;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import qupath.ext.template.stageManager.StageManager;

public class StopWatchService {
    private static StopWatchService instance;
    private int seconds;
    private final Timeline timeline;

    private StopWatchService(){
        KeyFrame frame = new KeyFrame(Duration.seconds(1), event -> {
            seconds++;
            updateTimerLabel();
        });

        timeline = new Timeline(frame);
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void updateTimerLabel(){
        int hours = seconds/(60*60);
        int minutes = (seconds % 3600)/60;
        int sec = seconds % 60;
        StageManager.getInstance().getActiveLearningController()
                .getTimerLabel().setText(String.format("%02d:%02d:%02d", hours, minutes, sec));
    }

    public void startTimer(){
        timeline.play();
    }

    public void pauseTimer(){
        timeline.pause();
    }

    public void resetTimer(){
        timeline.stop();
        seconds = 0;
        updateTimerLabel();
    }

    public int getSeconds(){
        return seconds;
    }

    public static StopWatchService getInstance(){
        if(instance == null){
            instance = new StopWatchService();
        }

        return instance;
    }
}
