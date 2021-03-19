package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.Chunk;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkId;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.MeshChunkData;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.PointData;

public class WLSGenerator<ID extends ChunkId, T extends PointData>
		extends Generator<ID, T> {
	@Override
	public MeshChunkData generateChunkData(Chunk<ID, ? extends T> chunk) {
		return null;
	}
}
