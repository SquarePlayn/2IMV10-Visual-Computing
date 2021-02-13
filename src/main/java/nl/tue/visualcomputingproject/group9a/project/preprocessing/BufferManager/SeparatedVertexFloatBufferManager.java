package nl.tue.visualcomputingproject.group9a.project.preprocessing.BufferManager;

import org.lwjgl.BufferUtils;

import java.nio.Buffer;
import java.nio.FloatBuffer;

public class SeparatedVertexFloatBufferManager
		implements VertexBufferManager {
	private final FloatBuffer vertexBuffer;
	private final FloatBuffer normalBuffer;
	private final float[] cache = new float[3];
	
	private int size = 0;
	
	public SeparatedVertexFloatBufferManager(int numVertices) {
		vertexBuffer = (BufferUtils.createFloatBuffer(6 * numVertices));
		normalBuffer = vertexBuffer.slice();
		normalBuffer.position(3 * numVertices);
	}

	@Override
	public int addVertex(double vx, double vy, double vz,
						  double nx, double ny, double nz) {
		return addVertex((float) vx, (float) vy, (float) vz,
				(float) nx, (float) ny, (float) nz);
	}

	@Override
	public int addVertex(float vx, float vy, float vz,
						  float nx, float ny, float nz) {
		cache[0] = vx;
		cache[1] = vy;
		cache[2] = vz;
		vertexBuffer.put(cache);

		cache[0] = nx;
		cache[1] = ny;
		cache[2] = nz;
		normalBuffer.put(cache);
		return size++;
	}

	@Override
	public FloatBuffer finalizeFloatBuffer() {
		normalBuffer.flip();
		return normalBuffer;
	}

	@Override
	public int size() {
		return size;
	}
	
}
