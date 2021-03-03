package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.Chunk;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkId;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.MeshChunkData;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.PointData;

/**
 * A trivial {@link Generator} implementation suitable for interpolating
 * raw data using Moving Least Squares (MLS).
 * TODO: to be implemented.
 *
 * @param <T> The type of point data container.
 */
public class MLSGenerator<ID extends ChunkId, T extends PointData>
		extends Generator<ID, T> {
	
	@Override
	public MeshChunkData generateChunkData(Chunk<ID, ? extends T> chunk) {
		throw new UnsupportedOperationException("WIP");
	}
	
}
