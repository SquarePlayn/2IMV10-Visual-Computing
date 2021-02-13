package nl.tue.visualcomputingproject.group9a.project.common;

import lombok.Value;
import org.joml.Vector2d;
import org.joml.Vector3d;

/**
 * Defines a point in 3D space from the map sheet data.
 */
@Value
public class Point {
	double lat, lon, alt;
	
	public Vector3d asVec3d() {
		return new Vector3d(lat, alt, lon);
	}
	
	public Vector2d getVec2d() {
		return new Vector2d(lat, lon);
	}
	
	public static Point from3dVec(Vector3d vec) {
		return new Point(vec.x, vec.z, vec.y);
	}
	
}
