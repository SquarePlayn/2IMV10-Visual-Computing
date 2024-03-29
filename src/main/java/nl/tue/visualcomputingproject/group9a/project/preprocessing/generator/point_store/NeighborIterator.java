package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store;


import nl.tue.visualcomputingproject.group9a.project.common.util.GeneratorIterator;
import org.joml.Vector3d;

import java.util.Iterator;

public class NeighborIterator<Data extends PointIndexData>
		extends GeneratorIterator<Data> {
	private final Vector3d center;
	private final Store<Data> store;
	private final double distSquare;
	private final boolean in2D;

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
			double dist, double gridDist,
			boolean in2D) {
		this.center = center;
		this.store = store;
		this.distSquare = dist * dist;
		this.in2D = in2D;
		int delta = (int) Math.ceil(dist / gridDist);
		maxX = x + delta;
		minZ = z - delta;
		maxZ = z + delta;
		curX = x - delta;
		curZ = minZ;
	}
	
	private double calcLength2D2(Vector3d v) {
		return v.x*v.x + v.z*v.z;
	}
	
	private double calcDist2(Vector3d v1, Vector3d v2) {
		if (in2D) {
			double x = v1.x - v2.x;
			double z = v1.z - v2.z;
			return x * x + z * z;
		} else {
			return v1.distanceSquared(v2);
		}
	}

	@Override
	protected Data generateNext() {
		for (; curX <= maxX; curX++) {
			for (; curZ <= maxZ; curZ++) {
				while (dataIt != null && dataIt.hasNext()) {
					Data data = dataIt.next();
					double curDistSquare = calcDist2(data.getVec(), center);
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
