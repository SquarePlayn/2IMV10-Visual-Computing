package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.Chunk;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.MeshChunkData;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.PointData;

public class MLSGenerator<T extends PointData>
		extends Generator<T> {
	
	@Override
	public MeshChunkData generateChunkData(Chunk<T> chunk) {
		return null;
	}
	
}
