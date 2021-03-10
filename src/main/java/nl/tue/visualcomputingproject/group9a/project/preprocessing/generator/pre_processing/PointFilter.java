package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing;

import org.joml.Vector3d;

public interface PointFilter {
	
	Vector3d filter(Vector3d point);
	
}
