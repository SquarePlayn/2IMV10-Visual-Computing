package nl.tue.visualcomputingproject.group9a.project.preprocessing.BufferManager;

import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class InterleavedVertexFloatBufferManager
		implements VertexBufferManager {
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final ByteBuffer byteBuffer;
	private final FloatBuffer floatBuffer;
	private final float[] cache = new float[6];

	private int size = 0;
	
	public InterleavedVertexFloatBufferManager(int numVertices) {
		byteBuffer = BufferUtils.createByteBuffer(Float.BYTES * 6 * numVertices);
		floatBuffer = byteBuffer.asFloatBuffer();
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
		LOGGER.info(String.format("\n%3d: v=[%+.3f  %+.3f  %+.3f], n=[%+.3f  %+.3f  %+.3f]",
				size, vx, vy, vz, nx, ny, nz));
		cache[0] = vx;
		cache[1] = vy;
		cache[2] = vz;
		cache[3] = nx;
		cache[4] = ny;
		cache[5] = nz;
		floatBuffer.put(cache);
		return size++;
	}

	@Override
	public FloatBuffer finalizeFloatBuffer() {
		floatBuffer.flip();
		return floatBuffer;
	}
	
	@Override
	public ByteBuffer finalizeBuffer() {
		byteBuffer.limit(Float.BYTES * 6 * size);
		return byteBuffer;
	}

	@Override
	public int size() {
		return size;
	}
	
}
