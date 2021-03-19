package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.Data;
import nl.tue.visualcomputingproject.group9a.project.common.Point;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Chunk data class for storing point-clouds.
 */
@Data
public class PointCloudChunkData
		implements PointData {
	List<Double> interleavedPoints = new ArrayList<>();
	
	public void addPoint(Point p) {
		interleavedPoints.add(p.getX());
		interleavedPoints.add(p.getY());
		interleavedPoints.add(p.getAlt());
	}
	
	public void addPoint(double x, double y, double alt) {
		interleavedPoints.add(x);
		interleavedPoints.add(y);
		interleavedPoints.add(alt);
	}

	@Override
	public int size() {
		return interleavedPoints.size()/3;
	}
	
	@Override
	public String toString() {
		return String.format("%d points", size());
	}
	
	@Override
	public Iterable<Point> getPointIterator() {
		return () -> new Iterator<Point>() {
			int counter = 0;
			
			@Override
			public boolean hasNext() {
				return counter < interleavedPoints.size();
			}
			
			@Override
			public Point next() {
				Point p = new Point(
						interleavedPoints.get(counter),
						interleavedPoints.get(counter + 1),
						interleavedPoints.get(counter + 2));
				counter += 3;
				return p;
			}
		};
	}
	
	@Override
	public Iterable<Vector3d> getVector3D() {
		return () -> new Iterator<Vector3d>() {
			int counter = 0;
			
			@Override
			public boolean hasNext() {
				return counter < interleavedPoints.size();
			}
			
			@Override
			public Vector3d next() {
				Vector3d p = new Vector3d(
						interleavedPoints.get(counter),
						interleavedPoints.get(counter + 2),
						-interleavedPoints.get(counter + 1));
				counter += 3;
				return p;
			}
		};
	}
	
	@Override
	public Iterable<Vector2d> getVector2D() {
		return () -> new Iterator<Vector2d>() {
			int counter = 0;
			
			@Override
			public boolean hasNext() {
				return counter < interleavedPoints.size();
			}
			
			@Override
			public Vector2d next() {
				Vector2d p = new Vector2d(
						interleavedPoints.get(counter),
						-interleavedPoints.get(counter + 1));
				counter += 3;
				return p;
			}
		};
	}
}
