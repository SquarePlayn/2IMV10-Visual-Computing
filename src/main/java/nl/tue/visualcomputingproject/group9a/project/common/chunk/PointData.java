package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import nl.tue.visualcomputingproject.group9a.project.common.Point;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.Iterator;

/**
 * Interface for storing point data.
 */
public interface PointData {
	
	int size();
	
	Iterable<Point> getPointIterator();
	Iterable<Vector3d> getVector3D();
	Iterable<Vector2d> getVector2D();
	
}
