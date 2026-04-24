package qupath.ext.template.ui.dto;

import qupath.lib.objects.PathObject;

public record comparisonBetweenPathObjects(PathObject pathObject, double difference) {
}
