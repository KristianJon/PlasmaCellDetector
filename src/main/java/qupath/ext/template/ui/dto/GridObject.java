package qupath.ext.template.ui.dto;

import javafx.scene.layout.StackPane;
import qupath.lib.objects.PathObject;

/**
 * record class for easy referencing of grid nodes
 *
 * @param pane reference to the stack pane used to fill out grid cell
 * @param pathObject reference to the actual prediction annotation
 */
public record GridObject(StackPane pane, PathObject pathObject) {
}
