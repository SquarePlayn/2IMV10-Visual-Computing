package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store;

import nl.tue.visualcomputingproject.group9a.project.common.util.Pair;
import org.joml.Vector3d;

import java.util.Iterator;

public interface PointIndexStore {
	
	int getWidth();
	
	int getHeight();

	PointIndex get(int x, int z);
	
	void set(int x, int z, PointIndex point);
	
	default Iterator<Pair<Vector3d, Integer>> points(int x, int z) {
		PointIndex vec = get(x, z);
		if (vec == null) return null;
		return vec.iterator();
	}
	
	default Pair<Vector3d, Integer> getClosest(int x, int z, Vector3d point) {
		PointIndex vec = get(x, z);
		if (vec == null) return null;
		Pair<Vector3d, Integer> closest = null;
		double curDist = 0;
		for (Pair<Vector3d, Integer> pair : vec) {
			double dist = pair.getFirst().distanceSquared(point);
			if (closest == null || dist < curDist) {
				closest = pair;
				curDist = dist;
			}
		}
		return closest;
	}
	
	default boolean hasPoint(int x, int z) {
		return get(x, z) != null;
	}
	
}
