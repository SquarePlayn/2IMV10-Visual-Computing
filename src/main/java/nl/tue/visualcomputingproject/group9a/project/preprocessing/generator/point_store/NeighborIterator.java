package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store;


import lombok.RequiredArgsConstructor;
import nl.tue.visualcomputingproject.group9a.project.common.util.GeneratorIterator;
import org.joml.Vector3d;

import java.util.Iterator;

@RequiredArgsConstructor
public class NeighborIterator<Data extends PointIndexData>
		extends GeneratorIterator<Data> {
	private final Vector3d center;
	private final Store<Data> store;
	private final double distSquare;

	private final int maxX;
	private final int maxZ;
	private final int minZ;

	private int curX;
	private int curZ;
	private Iterator<Data> dataIt = null;

	public NeighborIterator(
			Vector3d center,
			Store<Data> store,
			int x, int z,
			double dist, double gridDist) {
		this.center = center;
		this.store = store;
		this.distSquare = dist * dist;
		int delta = (int) Math.ceil(dist / gridDist);
		maxX = x + delta;
		minZ = z - delta;
		maxZ = z + delta;
		curX = x - delta;
		curZ = minZ;
	}

	@Override
	protected Data generateNext() {
		for (; curX <= maxX; curX++) {
			for (; curZ <= maxZ; curZ++) {
				while (dataIt != null && dataIt.hasNext()) {
					Data data = dataIt.next();
					double curDistSquare = data.getVec().distanceSquared(center);
					if (curDistSquare != 0 && curDistSquare <= distSquare) {
						return data;
					}
				}
				dataIt = null;

				StoreElement<Data> curPoint = store.get(curX, curZ);
				if (curPoint == null) continue;
				dataIt = curPoint.iterator();
			}
			curZ = minZ;
		}

		done();
		return null;
	}
	
}
