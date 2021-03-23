package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing.DeleteInvalidPointFilter;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing.PointFilter;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform.GridTransform;
import org.joml.Vector3d;

import java.util.Iterator;
import java.util.function.Function;

public interface Store<Data extends PointIndexData> {
	/** The filter used to filter out illegal points. */
	PointFilter FILTER = new DeleteInvalidPointFilter();
	
	int getWidth();
	
	int getHeight();

	StoreElement<Data> get(int x, int z);
	
	void set(int x, int z, StoreElement<Data> point);
	
	default Iterator<Data> iteratorOf(int x, int z) {
		StoreElement<Data> vec = get(x, z);
		if (vec == null) return null;
		return vec.iterator();
	}
	
	default boolean hasPoint(int x, int z) {
		return get(x, z) != null;
	}
	
	static <Data extends PointIndexData> Store<Data> generateFrom(
			ChunkPosition pos,
			GridTransform transform,
			Vector3d offset,
			Iterable<Vector3d> data,
			Function<Vector3d, Data> generator) {
		Store<Data> store = new ArrayStore<>(
				transform.toGridX(pos.getWidth()) + 1,
				transform.toGridZ(pos.getHeight()) + 1
		);

		// Insert and sort the data.
		for (Vector3d point : data) {
			point.sub(offset);
			point = FILTER.filter(point);
			if (point == null) continue;
			int x = transform.toGridX(point.x());
			int z = transform.toGridZ(point.z());
			if (store.hasPoint(x, z)) {
				store.get(x, z).add(generator.apply(point));
			} else {
				store.set(x, z, new StoreElement<>(generator.apply(point)));
			}
		}
		
		return store;
	}
	
}
