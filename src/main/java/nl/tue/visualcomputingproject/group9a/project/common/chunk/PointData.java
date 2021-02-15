package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import nl.tue.visualcomputingproject.group9a.project.common.Point;

/**
 * Interface for storing point data.
 */
public interface PointData
		extends Iterable<Point> {
	
	int size();
	
}
