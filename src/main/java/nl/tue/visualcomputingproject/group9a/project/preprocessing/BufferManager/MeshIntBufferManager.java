package nl.tue.visualcomputingproject.group9a.project.preprocessing.BufferManager;

import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

public class MeshIntBufferManager
		implements MeshBufferManager {
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	private final ByteBuffer byteBuffer;
	private final IntBuffer intBuffer;
	private final int vertexCount;
	private final boolean reversed;

	private int size;
	
	public MeshIntBufferManager(int vertexCount, int numFaces, boolean reversed) {
		byteBuffer = BufferUtils.createByteBuffer(Integer.BYTES * vertexCount * numFaces);
		intBuffer = byteBuffer.asIntBuffer();
		this.vertexCount = vertexCount;
		this.reversed = reversed;
	}

	@Override
	public int add(int... indices) {
		if (indices == null ||
				indices.length != vertexCount ) {
			throw new IllegalArgumentException("Invalid number of indices!");
		}
		LOGGER.info(String.format("\n%3d", size) + ": " + Arrays.toString(indices));
		if (reversed) {
			for (int i = 0; i < indices.length / 2; i++) {
				int j = indices.length - i - 1;
				int tmp = indices[i];
				indices[i] = indices[j];
				indices[j] = tmp;
			}
		}
		intBuffer.put(indices);
		return size++;
	}

	@Override
	public IntBuffer finalizeIntBuffer() {
		intBuffer.flip();
		return intBuffer;
	}

	@Override
	public ByteBuffer finalizeBuffer() {
		byteBuffer.limit(Integer.BYTES * vertexCount * size);
		return byteBuffer;
	}


	@Override
	public int size() {
		return size;
	}
	
	
}
