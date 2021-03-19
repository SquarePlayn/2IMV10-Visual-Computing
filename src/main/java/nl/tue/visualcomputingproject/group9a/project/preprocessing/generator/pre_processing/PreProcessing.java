package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing;

import nl.tue.visualcomputingproject.group9a.project.common.util.Pair;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.Generator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.PointIndex;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.PointIndexStore;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform.GridTransform;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;

public class PreProcessing {
	/** The logger of this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	public static void removeIllegalPoints(PointIndexStore points) {
		for (int x = 0; x < points.getWidth(); x++) {
			for (int z = 0; z < points.getHeight(); z++) {
				if (!points.hasPoint(x, z)) continue;
				Iterator<Pair<Vector3d, Integer>> it = points.get(x, z).iterator();
				while (it.hasNext()) {
					Pair<Vector3d, Integer> pair = it.next();
					if (pair.getFirst().y() > Generator.HEIGHT_THRESHOLD ||
							pair.getFirst().y() < -Generator.HEIGHT_THRESHOLD) {
						it.remove();
					}
				}
			}
		}
	}
	
	public static void fillNullPoints(
			PointIndexStore store,
			GridTransform transform) {
		for (int x = 0; x < store.getWidth(); x++) {
			for (int z = 0; z < store.getHeight(); z++) {
				if (store.hasPoint(x, z)) continue;
				Vector3d vec = new Vector3d(transform.toCoordX(x), 0, transform.toCoordZ(z));

				int num = 0;
				double height = 0;
				double expDX = 0;
				double expDZ = 0;
				for (int i = -1; i <= 1; i++) {
					for (int j = -1; j <= 1; j++) {
						if (i == 0 && j == 0) continue;
						int otherX = x + i;
						int otherZ = z + j;
						if (!store.hasPoint(otherX, otherZ)) continue;
						for (Pair<Vector3d, Integer> other : store.get(otherX, otherZ)) {
							num++;
							expDX += other.getFirst().x() - transform.toCoordX(otherX);
							height += other.getFirst().y();
							expDZ += other.getFirst().z() - transform.toCoordZ(otherZ);
						}
					}
				}
				if (num == 0) {
					LOGGER.error("TODO: edge case missing point!");
				} else {
					vec.add(
							expDX / num,
							height / num,
							expDZ / num
					);
				}
				store.set(x, z, new PointIndex(vec));
			}
		}
	}
	
}
