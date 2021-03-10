package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing;

import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.Generator;
import org.joml.Vector3d;

public class DeleteInvalidPointFilter
		implements PointFilter {
	
	@Override
	public Vector3d filter(Vector3d point) {
		if (point.y() > Generator.HEIGHT_THRESHOLD ||
				point.y() < -Generator.HEIGHT_THRESHOLD) {
			return null;
		} else {
			return point;
		}
	}
	
}
