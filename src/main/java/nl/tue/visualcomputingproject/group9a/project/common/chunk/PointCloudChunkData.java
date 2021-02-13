package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.Value;
import nl.tue.visualcomputingproject.group9a.project.common.Point;

import java.util.Iterator;
import java.util.List;

/**
 * Chunk data class for storing point-clouds.
 */
@Value
public class PointCloudChunkData
		implements PointData {
	List<Point> points;

	@Override
	public Iterator<Point> iterator() {
		return points.iterator();
	}

	@Override
	public int size() {
		return points.size();
	}
	
}
