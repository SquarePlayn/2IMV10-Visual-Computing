package nl.tue.visualcomputingproject.group9a.project.preprocessing.BufferManager;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.VertexBufferType;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.nio.Buffer;
import java.nio.FloatBuffer;

public interface VertexBufferManager
		extends BufferManager {

	default int addVertex(Vector3d v, Vector3d n) {
		return addVertex(v.x, v.y, v.z, n.x, n.y, n.z);
	}

	default int addVertex(Vector3f v, Vector3f n) {
		return addVertex(v.x, v.y, v.z, n.x, n.y, n.z);
	}

	int addVertex(double vx, double vy, double vz,
				   double nx, double ny, double nz);

	int addVertex(float vx, float vy, float vz,
				   float nx, float ny, float nz);


	FloatBuffer finalizeFloatBuffer();
	
	@Override
	default Buffer finalizeBuffer() {
		return finalizeFloatBuffer();
	}

	static VertexBufferManager createManagerFor(VertexBufferType type, int numVertices) {
		switch (type) {
			case VERTEX_3_FLOAT_NORMAL_3_FLOAT:
				return new SeparatedVertexFloatBufferManager(numVertices);
			case INTERLEAVED_VERTEX_3_FLOAT_NORMAL_3_FLOAT:
				return new InterleavedVertexFloatBufferManager(numVertices);
			default:
				throw new IllegalArgumentException("Invalid vertex buffer type: " + type);
		}
	}
	
}
