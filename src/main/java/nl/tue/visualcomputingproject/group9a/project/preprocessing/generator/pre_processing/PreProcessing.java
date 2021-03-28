package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing;

import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.Generator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.PointIndexData;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.StoreElement;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.Store;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform.ScaleGridTransform;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.function.Function;

public class PreProcessing {
	/** The logger of this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
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
	
	public static <Data extends PointIndexData> int fillNullPoints(
			Store<Data> store,
			ScaleGridTransform transform,
			Function<Vector3d, Data> generator) {
		int count = 0;
		for (int x = 0; x < store.getWidth(); x++) {
			for (int z = 0; z < store.getHeight(); z++) {
				if (store.hasPoint(x, z)) {
					count += store.get(x, z).size();
					continue;
				}
				Vector3d vec = new Vector3d(transform.toCoordX(x), 0, transform.toCoordZ(z));

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
							expDX += (other.getVec().x() - transform.toCoordX(otherX)) / d;
							height += other.getVec().y();
							expDZ += (other.getVec().z() - transform.toCoordZ(otherZ)) / d;
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
