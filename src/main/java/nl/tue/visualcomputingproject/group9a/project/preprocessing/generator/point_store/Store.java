package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store;

import lombok.AllArgsConstructor;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;
import nl.tue.visualcomputingproject.group9a.project.common.util.FunctionIterator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.Generator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.buffer_manager.VertexBufferManager;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing.DeleteInvalidPointFilter;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing.PointFilter;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform.ScaleGridTransform;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public interface Store<Data extends PointIndexData>
		extends Iterable<StoreElement<Data>> {
	/** The local distance used to approximate the normals with. */
	int NORMAL_DIST = 1;
	
	@FunctionalInterface
	interface StoreFunction<Data extends PointIndexData> {
		void consume(int x, int z, StoreElement<Data> element);
	}
	
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
				getTransform().getScaleX(),
				true
		);
	}
	
	default int addPoints(
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
			if (hasPoint(x, z)) {
				get(x, z).add(generator.apply(point));
			} else {
				set(x, z, new StoreElement<>(generator.apply(point)));
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
	
	default void addToVertexManagerGenWLSNormals(
			final VertexBufferManager vertexManager,
			ChunkPosition crop) {
		forEachInCrop(crop, (x, z, elem) -> {
			for (PointIndexData point : elem) {
				List<Vector3d> neighbors = new ArrayList<>();
				for (int dz = -NORMAL_DIST; dz <= NORMAL_DIST; dz++) {
					for (int dx = -NORMAL_DIST; dx <= NORMAL_DIST; dx++) {
						if (dx == 0 && dz == 0) continue;
						int x2 = x + dx;
						int z2 = z + dz;
						if (!hasPoint(x2, z2)) continue;
						for (PointIndexData point2 : get(x2, z2)) {
							neighbors.add(point2.getVec());
						}
					}
				}
				Vector3d normal = Generator.generateWLSNormalFor(point.getVec(), neighbors.iterator());
				point.setIndex(vertexManager.addVertex(point.getVec(), normal));
			}
		});
	}
	
	default void forEach(StoreFunction<Data> function) {
		for (int z = 0; z < getHeight(); z++) {
			for (int x = 0; x < getWidth(); x++) {
				function.consume(x, z, get(x, z));
			}
		}
	}
	
	default void forEachInCrop(
			ChunkPosition crop,
			StoreFunction<Data> function) {
		int beginX = Math.max(0, getTransform().toGridX(crop.getX()));
		int endX = Math.min(getWidth(), getTransform().toGridX(crop.getX() + crop.getWidth()));
		int beginZ = Math.max(0, getTransform().toGridZ(crop.getY()));
		int endZ = Math.min(getHeight(), getTransform().toGridZ(crop.getY() + crop.getHeight()));
		for (int z = beginZ; z <= endZ; z++) {
			for (int x = beginX; x <= endX; x++) {
				if (!hasPoint(x, z)) continue;
				function.consume(x, z, get(x, z));
			}
		}
	}
	
	static <Data extends PointNormalIndexData> void addToVertexManager(
			Store<Data> store,
			ChunkPosition crop,
			final VertexBufferManager vertexManager) {
		store.forEachInCrop(crop, (x, z, elem) -> {
			if (elem == null) return;
			for (Data data : elem) {
				data.setIndex(vertexManager.addVertex(data.getVec(), data.getNormal()));
			}
		});
	}
	
	
	default int count() {
		@AllArgsConstructor
		class Container {
			int i;
		}
		Container c = new Container(0);
		forEach((x, z, elem) -> {
			c.i++;
		});
		return c.i;
	}
	
	default int countCropped(ChunkPosition crop) {
		@AllArgsConstructor
		class Container {
			int i;
		}
		Container c = new Container(0);
		forEachInCrop(crop, (x, z, elem) -> {
			c.i++;
		});
		return c.i;
	}
	
}
