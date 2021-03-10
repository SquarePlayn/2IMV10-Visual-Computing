package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing;

import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.Generator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.PointIndex;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.PointIndexStore;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform.GridTransform;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class PreProcessing {
	/** The logger of this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	public static void removeIllegalPoints(PointIndexStore points) {
		for (int x = 0; x < points.getWidth(); x++) {
			for (int z = 0; z < points.getHeight(); z++) {
				if (!points.hasPoint(x, z)) continue;
				if (points.getPoint(x, z).y() > Generator.HEIGHT_THRESHOLD ||
						points.getPoint(x, z).y() < -Generator.HEIGHT_THRESHOLD) {
					points.set(x, z, null);
				}
			}
		}
	}
	
	public static void fillNullPoints(
			PointIndexStore points,
			GridTransform transform) {
		for (int x = 0; x < points.getWidth(); x++) {
			for (int z = 0; z < points.getHeight(); z++) {
				if (points.hasPoint(x, z)) continue;
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
						if (points.hasPoint(otherX, otherZ)) {
							Vector3d other = points.getPoint(otherX, otherZ);
							num++;
							expDX += other.x() - transform.toCoordX(otherX);
							height += other.y();
							expDZ += other.z() - transform.toCoordZ(otherZ);
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
				points.set(x, z, new PointIndex(vec));
			}
		}
	}
	
}
