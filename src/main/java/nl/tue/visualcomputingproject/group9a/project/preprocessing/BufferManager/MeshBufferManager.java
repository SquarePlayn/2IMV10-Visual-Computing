package nl.tue.visualcomputingproject.group9a.project.preprocessing.BufferManager;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.MeshBufferType;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;

import java.nio.Buffer;
import java.nio.IntBuffer;

public interface MeshBufferManager
		extends BufferManager {
	
	int add(int... indices);
	
	IntBuffer finalizeIntBuffer();

	@Override
	default Buffer finalizeBuffer() {
		return finalizeIntBuffer();
	}

	static MeshBufferManager createManagerFor(
			QualityLevel quality,
			MeshBufferType type,
			int width, int height,
			int numVertices) {
		switch (quality) {
			case FIVEBYFIVE:
			case HALFBYHALF:
				switch (type) {
					case TRIANGLES_CLOCKWISE_3_INT:
					case TRIANGLES_COUNTER_CLOCKWISE_3_INT:
						return new MeshIntBufferManager(
								width * height * 2,
								3,
								type.isClockwise());
					case QUADS_CLOCKWISE_4_INT:
					case QUADS_COUNTER_CLOCKWISE_4_INT:
						return new MeshIntBufferManager(
								width * height,
								4,
								type.isClockwise());
					default:
						throw new IllegalArgumentException("Invalid vertex buffer type: " + type);
				}
			case LAS:
				switch (type) {
					case TRIANGLES_CLOCKWISE_3_INT:
					case TRIANGLES_COUNTER_CLOCKWISE_3_INT:
						return new MeshIntBufferManager(
								numVertices * 2 - 5,
								3,
								type.isClockwise());
					case QUADS_CLOCKWISE_4_INT:
					case QUADS_COUNTER_CLOCKWISE_4_INT:
						return new MeshIntBufferManager(
								numVertices - 3,
								4,
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
						numFaces,
						3,
						type.isClockwise());
			case QUADS_CLOCKWISE_4_INT:
			case QUADS_COUNTER_CLOCKWISE_4_INT:
				return new MeshIntBufferManager(
						numFaces,
						4,
						type.isClockwise());
			default:
				throw new IllegalArgumentException("Invalid vertex buffer type: " + type);
		}
	}
	
}
