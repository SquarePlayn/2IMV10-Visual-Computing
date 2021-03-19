package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.buffer_manager;

import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Buffer manager class for managing a separated float typed vertex buffer.
 */
public class SeparatedVertexFloatBufferManager
		implements VertexBufferManager {

	/** The byte buffer backing the {@link #vertexBuffer} and {@link #normalBuffer}. */
	private final ByteBuffer byteBuffer;
	/** The float buffer used to write the vertex data to. */
	private final FloatBuffer vertexBuffer;
	/** The float buffer used to write the normal data to. */
	private final FloatBuffer normalBuffer;
	/** The cache used to temporarily write data to. */
	private final float[] cache = new float[3];

	/** The current amount of elements in the buffer. */
	private int size = 0;

	/**
	 * Creates a new vertex buffer manager.
	 * Notice that adding more than {@code numVertices} elements will cause
	 * a {@link java.nio.BufferOverflowException}.
	 *
	 * @param numVertices The maximum number of elements in the buffer.
	 */
	public SeparatedVertexFloatBufferManager(int numVertices) {
		byteBuffer = BufferUtils.createByteBuffer(Float.BYTES * 6 * numVertices);
		vertexBuffer = byteBuffer.asFloatBuffer();
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
		vertexBuffer.limit(6 * size);
		vertexBuffer.position(0);
		return vertexBuffer;
	}

	@Override
	public ByteBuffer finalizeBuffer() {
		byteBuffer.limit(Float.BYTES * 6 * size);
		byteBuffer.position(0);
		return byteBuffer;
	}

	@Override
	public int size() {
		return size;
	}
	
}
