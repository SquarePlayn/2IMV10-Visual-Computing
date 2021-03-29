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
	/** Half of the delta coordinates of a circle of size 2 around the center point.
	 *  The other half can be found by negating the values. */
	private static final int[][] DX_DZ_LOCAL_NEIGHBORHOOD = new int[][] {
			{1, 0}, {1, 1}, {0, 1}, {-1, 1},
			{2, -1}, {2, 0}, {2, 1}, {1, 2}, {0, 2}, {-1, 2}
	};
	
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
			double w = 1.0 / neighborV.distance(vec);
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
					smoothTreeWLS(srcStore, trgStore, il, generator, x, z, 3.5);
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
	
	private static <Data extends PointIndexData> void checkIsTree(
			Store<Data> store,
			boolean[][] isTree,
			int x,
			int z) {
		int trees = 0;
		int nonTrees = 0;
		int neutral = 0;
		for (int[] dxdz : DX_DZ_LOCAL_NEIGHBORHOOD) {
			int x1 = x + dxdz[0];
			int z1 = z + dxdz[1];
			int x2 = x - dxdz[0];
			int z2 = z - dxdz[1];
			
			boolean has1 = store.hasPoint(x1, z1);
			boolean has2 = store.hasPoint(x2, z2);
			if (has1 && has2) {
				if (isTree[x1][z1] == isTree[x2][z2]) {
					if (isTree[x1][z1]) trees += 3;
					else nonTrees += 3;
				} else neutral += 2;
				
			} else if (has1) {
				if (isTree[x1][z1]) trees++;
				else nonTrees++;
				
			} else if (has2) {
				if (isTree[x2][z2]) trees++;
				else nonTrees++;
			}
		}
		
		if (isTree[x][z]) {
			if (nonTrees > 1.5 * (trees + neutral)) {
				isTree[x][z] = false;
			}
		} else {
			if (trees > 1.5 * (nonTrees + neutral)) {
				isTree[x][z] = true;
			}
		}
	}

	public static <Data extends PointIndexData> int treeSmoothing2(
			Store<Data> srcStore,
			Store<Data> trgStore,
			Function<Vector3d, Data> generator) {
		boolean[][] isTree = new boolean[srcStore.getWidth()][srcStore.getHeight()];
		double[][] low = new double[srcStore.getWidth()][srcStore.getHeight()];
		double[][] max = new double[srcStore.getWidth()][srcStore.getHeight()];
		// Fill initial values.
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
				Pair<Double, Double> pair = il.getTopInterval();
				low[x][z] = pair.getFirst();
				max[x][z] = pair.getSecond();
				if (il.size() <= 2) {
					continue;
				}
				int numGaps = il.countGaps();

				if (numGaps > il.size() / 2.0) {
					// Classified as tree
					isTree[x][z] = true;
				}
			}
		}

		// Improve tree classification by looking at group formation.
		for (int z = 0; z < srcStore.getHeight(); z++) {
			for (int x = 0; x < srcStore.getWidth(); x++) {
				checkIsTree(srcStore, isTree, x, z);
			}
		}

		int numVertices = 0;
		for (int z = 0; z < srcStore.getHeight(); z++) {
			for (int x = 0; x < srcStore.getWidth(); x++) {
				StoreElement<Data> elem = srcStore.get(x, z);
				if (!isTree[x][z]) {
					trgStore.set(x, z, elem);
					numVertices += elem.size();
					continue;
				}
				Data data = elem.get(0);
				Vector3d vec = new Vector3d(elem.get(0).getVec());
				double minH, maxH;
				{ // Approximate initial height value.
					double sumMinW = low[x][z];
					double sumMaxW = max[x][z];
					double sumW = 1;
					for (int[] dxdz : DX_DZ_LOCAL_NEIGHBORHOOD) {
						int x1 = x + dxdz[0];
						int z1 = z + dxdz[1];
						int x2 = x - dxdz[0];
						int z2 = z - dxdz[1];
						double w = 1.0 / (dxdz[0] * dxdz[0] + dxdz[1] * dxdz[1]);
						if (srcStore.hasPoint(x1, z1)) {
							sumMinW += low[x1][z1] * w;
							sumMaxW += max[x1][z1] * w;
							sumW += w;
						}
						if (srcStore.hasPoint(x2, z2)) {
							sumMinW += low[x2][z2] * w;
							sumMaxW += max[x2][z2] * w;
							sumW += w;
						}
					}
					minH = sumMinW / sumW;
					maxH = sumMaxW / sumW;
					vec.y = (minH + maxH) / 2.0;
				}
				{ // Move value to stable position.
					Iterator<Data> neighbors = new NeighborIterator<>(
							data.getVec(),
							srcStore,
							x, z,
							srcStore.getTransform().getScaleX() * 2.5,
							srcStore.getTransform().getScaleX(),
							true
					);
					double sumHW = vec.y;
					double sumW = 1;
					while (neighbors.hasNext()) {
						Vector3d neighborV = neighbors.next().getVec();
						if (neighborV.y < minH) continue;
						double w = 1.0 / neighborV.distance(vec);
						sumW += w;
						sumHW += neighborV.y * w;
					}
					vec.y = sumHW / sumW;
				}
				numVertices++;
				trgStore.set(x, z, new StoreElement<>(generator.apply(vec)));
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
