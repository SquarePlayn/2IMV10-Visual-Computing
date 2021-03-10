package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;

/**
 * The generator used to pre-process a raw data chunk with point data.
 *
 * @param <T> The type of point data container.
 */
public abstract class Generator<ID extends ChunkId, T extends PointData> {
	public static final double HEIGHT_THRESHOLD = 10_000;

	/**
	 * Generates the mesh data chunk.
	 * 
	 * @param chunk The raw data chunk.
	 * 
	 * @return A data chunk containing a vertex buffer and a mesh of the processed data.
	 */
	public abstract MeshChunkData generateChunkData(Chunk<ID, ? extends T> chunk);

	/**
	 * Creates a generator based on the quality level.
	 * 
	 * @param quality The quality level.
	 *
	 * @param <T> The type of point data container.
	 * 
	 * @return A generator suitable for the given quality.
	 */
	public static <ID extends ChunkId, T extends PointData> Generator<ID, T> createGeneratorFor(QualityLevel quality) {
		if (quality.isInterpolated()) {
			return new InterpolatedGenerator<>();
		} else {
			return new MLSGenerator<>();
		}
	}
	
}
