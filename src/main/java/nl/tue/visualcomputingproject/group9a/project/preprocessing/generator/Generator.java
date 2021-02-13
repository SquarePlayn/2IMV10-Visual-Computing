package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.Chunk;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.MeshChunkData;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.PointData;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;

public abstract class Generator<T extends PointData> {

	public abstract MeshChunkData generateChunkData(Chunk<T> chunk);
	
	public static <T extends PointData> Generator<T> createGeneratorFor(QualityLevel quality) {
		if (quality.isInterpolated()) {
			return new InterpolatedGenerator<>();
		} else {
			return new MLSGenerator<>();
		}
	}
	
}
