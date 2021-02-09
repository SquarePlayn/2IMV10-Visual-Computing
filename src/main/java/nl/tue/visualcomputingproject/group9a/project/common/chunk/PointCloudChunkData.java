package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.Data;
import nl.tue.visualcomputingproject.group9a.project.common.Point;

import java.util.List;

/**
 * Chunk data class for storing point-clouds.
 */
@Data
public class PointCloudChunkData {
	List<Point> points;
}
