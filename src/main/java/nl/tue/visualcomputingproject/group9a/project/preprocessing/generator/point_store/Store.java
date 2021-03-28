package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store;

import nl.tue.visualcomputingproject.group9a.project.common.util.FunctionIterator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.Generator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.buffer_manager.VertexBufferManager;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing.DeleteInvalidPointFilter;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing.PointFilter;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform.ScaleGridTransform;
import org.joml.Vector3d;

import java.util.Iterator;
import java.util.function.Function;

public interface Store<Data extends PointIndexData>
		extends Iterable<StoreElement<Data>> {
	/** The filter used to filter out illegal points. */
	PointFilter FILTER = new DeleteInvalidPointFilter();
	
	int getWidth();
	
	int getHeight();
	
	ScaleGridTransform getTransform();

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
	
	default boolean isInBounds(int x, int z) {
		return 0 <= x && x < getWidth() &&
				0 <= z && z < getHeight();
	}
	
	default Iterator<Data> neighborIteratorOf(
			Vector3d point,
			int x,
			int z,
			double dist) {
		return new NeighborIterator<>(
				point,
				this,
				x, z,
				dist,
				getTransform().getScaleX()
		);
	}
	
	default int addPoints(
			Store<Data> store,
			Vector3d offset,
			Iterable<Vector3d> data,
			Function<Vector3d, Data> generator) {
		int count = 0;
		for (Vector3d point : data) {
			point.sub(offset);
			point = FILTER.filter(point);
			if (point == null) continue;
			int x = getTransform().toGridX(point.x());
			int z = getTransform().toGridZ(point.z());
			count++;
			if (store.hasPoint(x, z)) {
				store.get(x, z).add(generator.apply(point));
			} else {
				store.set(x, z, new StoreElement<>(generator.apply(point)));
			}
		}
		return count;
	}
	
	static void genWLSNormals(Store<? extends PointNormalIndexData> store, double dist) {
		for (int z = 0; z < store.getHeight(); z++) {
			for (int x = 0; x < store.getWidth(); x++) {
				StoreElement<? extends PointNormalIndexData> elem = store.get(x, z);
				for (PointNormalIndexData data : elem) {
					Iterator<Vector3d> it = new FunctionIterator<>(
							store.neighborIteratorOf(data.getVec(), x, z, dist),
							PointNormalIndexData::getVec
					);
					data.setNormal(Generator.generateWLSNormalFor(data.getVec(), it));
				}
			}
		}
	}
	
	static void addToVertexManager(
			Store<? extends PointNormalIndexData> store,
			VertexBufferManager vertexManager) {
		for (StoreElement<? extends PointNormalIndexData> elem : store) {
			for (PointNormalIndexData data : elem) {
				data.setIndex(vertexManager.addVertex(data.getVec(), data.getNormal()));
			}
		}
	}
	
}
