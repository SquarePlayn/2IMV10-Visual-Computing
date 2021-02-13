package nl.tue.visualcomputingproject.group9a.project.preprocessing.BufferManager;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.MeshBufferType;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;

import java.nio.IntBuffer;

public interface MeshBufferManager
		extends BufferManager {
	
	int add(int... indices);
	
	default IntBuffer finalizeIntBuffer() {
		return finalizeBuffer().asIntBuffer();
	}

	static MeshBufferManager createManagerFor(
			QualityLevel quality,
			MeshBufferType type,
			int width, int height,
			int numVertices) {
		switch (quality) {
			case FIVE_BY_FIVE:
			case HALFBYHALF:
				switch (type) {
					case TRIANGLES_CLOCKWISE_3_INT:
					case TRIANGLES_COUNTER_CLOCKWISE_3_INT:
						return new MeshIntBufferManager(
								3,
								width * height * 2,
								type.isClockwise());
					case QUADS_CLOCKWISE_4_INT:
					case QUADS_COUNTER_CLOCKWISE_4_INT:
						return new MeshIntBufferManager(
								4,
								width * height,
								type.isClockwise());
					default:
						throw new IllegalArgumentException("Invalid vertex buffer type: " + type);
				}
			case LAS:
				switch (type) {
					case TRIANGLES_CLOCKWISE_3_INT:
					case TRIANGLES_COUNTER_CLOCKWISE_3_INT:
						return new MeshIntBufferManager(
								3,
								numVertices * 2 - 5,
								type.isClockwise());
					case QUADS_CLOCKWISE_4_INT:
					case QUADS_COUNTER_CLOCKWISE_4_INT:
						return new MeshIntBufferManager(
								4,
								numVertices - 3,
								type.isClockwise());
					default:
						throw new IllegalArgumentException("Invalid vertex buffer type: " + type);
				}
			default:
				throw new IllegalArgumentException("Invalid quality level: " + quality);
		}
	}

	static MeshBufferManager createManagerFor(MeshBufferType type, int numFaces) {
		switch (type) {
			case TRIANGLES_CLOCKWISE_3_INT:
			case TRIANGLES_COUNTER_CLOCKWISE_3_INT:
				return new MeshIntBufferManager(
						3,
						numFaces,
						type.isClockwise());
			case QUADS_CLOCKWISE_4_INT:
			case QUADS_COUNTER_CLOCKWISE_4_INT:
				return new MeshIntBufferManager(
						4,
						numFaces,
						type.isClockwise());
			default:
				throw new IllegalArgumentException("Invalid vertex buffer type: " + type);
		}
	}
	
}
