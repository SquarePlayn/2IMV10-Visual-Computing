package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store;

import org.joml.Vector3d;

public interface PointIndexStore {
	
	int getWidth();
	
	int getHeight();

	PointIndex get(int x, int z);
	
	void set(int x, int z, PointIndex point);
	
	default Vector3d getPoint(int x, int z) {
		PointIndex vec = get(x, z);
		if (vec == null) return null;
		return vec.getPoint();
	}
	
	default Integer getIndex(int x, int z) {
		PointIndex vec = get(x, z);
		if (vec == null) return null;
		return vec.getIndex();
	}
	
	default void setIndex(int x, int z, int index) {
		PointIndex pi = get(x, z);
		if (pi == null) throw new IllegalArgumentException();
		pi.setIndex(index);
	}
	
	default boolean hasPoint(int x, int z) {
		return get(x, z) != null;
	}
	
}
