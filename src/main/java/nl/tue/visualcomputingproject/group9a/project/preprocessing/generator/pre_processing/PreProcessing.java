package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing;

import nl.tue.visualcomputingproject.group9a.project.common.util.Pair;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.Generator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.NeighborIterator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.PointIndexData;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.StoreElement;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.Store;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.function.Function;

public class PreProcessing {
	/** The logger of this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	/** The maximum height difference for the tree-smoothing algorithm. */
	private final static double MAX_HEIGHT_DIFF = 0.25;
	
	public static <Data extends PointIndexData> void removeIllegalPoints(Store<Data> points) {
		for (int x = 0; x < points.getWidth(); x++) {
			for (int z = 0; z < points.getHeight(); z++) {
				if (!points.hasPoint(x, z)) continue;
				Iterator<Data> it = points.iteratorOf(x, z);
				while (it.hasNext()) {
					Data data = it.next();
					if (data.getVec().y() > Generator.HEIGHT_THRESHOLD ||
							data.getVec().y() < -Generator.HEIGHT_THRESHOLD) {
						it.remove();
					}
				}
			}
		}
	}
	
	private static <Data extends PointIndexData> int findInDirection(
			Store<Data> store,
			int srcX, int srcZ,
			int dirX, int dirZ) {
		if (dirX == 0 && dirZ == 0) return -1;
		int x = srcX;
		int z = srcZ;
		for (int d = 1; store.isInBounds(x += dirX, z += dirZ); d++) {
			if (store.hasPoint(x, z)) {
				return d;
			}
		}
		return -1;
	}
	
	private static <Data extends PointIndexData> void smoothTreeWLS(
			Store<Data> srcStore,
			Store<Data> trgStore,
			IntervalList il,
			Function<Vector3d, Data> generator,
			int x, int z,
			double dist) {
		StoreElement<Data> elem = srcStore.get(x, z);
		Vector3d vec = new Vector3d(elem.get(0).getVec());
		Iterator<Data> neighbors = new NeighborIterator<>(
				vec,
				srcStore,
				x, z,
				srcStore.getTransform().getScaleX() * dist,
				srcStore.getTransform().getScaleX(),
				true
		);
			
		Pair<Double, Double> pair = il.getTopInterval();
		vec.y = (pair.getFirst() + pair.getSecond()) / 2;

		double sumHW = vec.y;
		double sumW = 1;
		while (neighbors.hasNext()) {
			Vector3d neighborV = neighbors.next().getVec();
			if (neighborV.y < pair.getFirst()) continue;
			double w = 1.0 / neighborV.distanceSquared(vec);
			sumW += w;
			sumHW += neighborV.y * w;
		}
		
		vec.y = sumHW / sumW;
		trgStore.set(x, z, new StoreElement<>(generator.apply(vec)));
	}
	
	public static <Data extends PointIndexData> int treeSmoothing(
			Store<Data> srcStore,
			Store<Data> trgStore,
			Function<Vector3d, Data> generator) {
		int numVertices = 0;
		for (int z = 0; z < srcStore.getHeight(); z++) {
			for (int x = 0; x < srcStore.getWidth(); x++) {
				if (!srcStore.hasPoint(x, z)) {
					continue;
				}
				
				StoreElement<Data> elem = srcStore.get(x, z);
				Data data = elem.get(0);
				Iterator<Data> neighbors = new NeighborIterator<>(
						data.getVec(),
						srcStore,
						x, z,
						srcStore.getTransform().getScaleX() * 2.5,
						srcStore.getTransform().getScaleX(),
						true
				);
				
				IntervalList il = new IntervalList();
				il.addInterval(data.getVec().y - MAX_HEIGHT_DIFF / 2, data.getVec().y + MAX_HEIGHT_DIFF / 2);
				while (neighbors.hasNext()) {
					Data neighbor = neighbors.next();
					double height = neighbor.getVec().y();
					il.addInterval(height - MAX_HEIGHT_DIFF / 2, height + MAX_HEIGHT_DIFF / 2);
				}
				if (il.size() <= 2) {
					continue;
				}
				int numGaps = il.countGaps();
				
				if (numGaps > il.size() / 2.0) {
					// Classified as tree
					smoothTreeWLS(srcStore, trgStore, il, generator, x, z, 2.5);
					numVertices++;
					
				} else {
					// Classified as non-tree.
					numVertices += elem.size();
					trgStore.set(x, z, elem);
				}
			}
		}
		return numVertices;
	}
	
	public static <Data extends PointIndexData> int fillNullPoints(
			Store<Data> store,
			Function<Vector3d, Data> generator) {
		int count = 0;
		for (int x = 0; x < store.getWidth(); x++) {
			for (int z = 0; z < store.getHeight(); z++) {
				if (store.hasPoint(x, z)) {
					count += store.get(x, z).size();
					continue;
				}
				Vector3d vec = new Vector3d(
						store.getTransform().toCoordX(x),
						0,
						store.getTransform().toCoordZ(z)
				);

				int num = 0;
				double height = 0;
				double expDX = 0;
				double expDZ = 0;
				for (int dirX = -1; dirX <= 1; dirX++) {
					for (int dirZ = -1; dirZ <= 1; dirZ++) {
						int d = findInDirection(store, x, z, dirX, dirZ);
						if (d <= 0) continue;
						int otherX = x + d*dirX;
						int otherZ = z + d*dirZ;
						StoreElement<Data> elem = store.get(otherX, otherZ);
						if (elem == null) {
							LOGGER.error("Should not occur!");
							continue;
						}
						for (Data other : elem) {
							num++;
							expDX += (other.getVec().x() - store.getTransform().toCoordX(otherX)) / d;
							height += other.getVec().y();
							expDZ += (other.getVec().z() - store.getTransform().toCoordZ(otherZ)) / d;
						}
					}
				}
				
				if (num == 0) {
					LOGGER.warn("No point in the star formation found! Using 0 instead for (" + x + ", " + z + ")");
				} else {
					vec.add(
							expDX / num,
							height / num,
							expDZ / num
					);
				}
				store.set(x, z, new StoreElement<>(generator.apply(vec)));
				count++;
			}
		}
		return count;
	}
	
}
