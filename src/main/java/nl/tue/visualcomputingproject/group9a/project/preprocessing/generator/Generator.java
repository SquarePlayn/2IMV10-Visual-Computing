package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.Chunk;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.MeshChunkData;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.PointData;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;

/**
 * The generator used to pre-process a raw data chunk with point data.
 *
 * @param <T> The type of point data container.
 */
public abstract class Generator<T extends PointData> {

	/**
	 * Generates the mesh data chunk.
	 * 
	 * @param chunk The raw data chunk.
	 * 
	 * @return A data chunk containing a vertex buffer and a mesh of the processed data.
	 */
	public abstract MeshChunkData generateChunkData(Chunk<? extends T> chunk);

	/**
	 * Creates a generator based on the quality level.
	 * 
	 * @param quality The quality level.
	 *
	 * @param <T> The type of point data container.
	 * 
	 * @return A generator suitable for the given quality.
	 */
	public static <T extends PointData> Generator<T> createGeneratorFor(QualityLevel quality) {
		if (quality.isInterpolated()) {
			return new InterpolatedGenerator<>();
		} else {
			return new MLSGenerator<>();
		}
	}
	
}
