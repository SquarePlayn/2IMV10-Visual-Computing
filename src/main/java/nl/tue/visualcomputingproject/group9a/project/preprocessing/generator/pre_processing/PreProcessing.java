package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing;

import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.Generator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.PointIndexData;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.StoreElement;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.Store;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform.GridTransform;
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
	
	public static <Data extends PointIndexData> void fillNullPoints(
			Store<Data> store,
			GridTransform transform,
			Function<Vector3d, Data> generator) {
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
						for (Data other : store.get(otherX, otherZ)) {
							num++;
							expDX += other.getVec().x() - transform.toCoordX(otherX);
							height += other.getVec().y();
							expDZ += other.getVec().z() - transform.toCoordZ(otherZ);
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
				store.set(x, z, new StoreElement<>(generator.apply(vec)));
			}
		}
	}
	
}
