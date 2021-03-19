package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.buffer_manager;

import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class MeshIntBufferManager
		implements MeshBufferManager {
	/** The logger object of this class. */
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	/** The byte buffer backing {@link #intBuffer}. */
	private final ByteBuffer byteBuffer;
	/** The buffer used to add the data to. */
	private final IntBuffer intBuffer;
	/** The number of vertices in a face. */
	private final int vertexCount;
	/** Whether to reverse the vertices in the faces. */
	private final boolean reversed;

	/** The current amount of faces in the buffer. */
	private int size;

	/**
	 * Creates a new mesh buffer.
	 * Notice that adding more than {@code numFaces} faces will cause
	 * a {@link java.nio.BufferOverflowException}.
	 * 
	 * @param vertexCount The number of vertices in a face.
	 * @param numFaces    The number of faces to allocate data for.
	 * @param reversed    Whether to reverse the vertices in the faces.
	 */
	public MeshIntBufferManager(int vertexCount, int numFaces, boolean reversed) {
		byteBuffer = BufferUtils.createByteBuffer(Integer.BYTES * vertexCount * numFaces);
		intBuffer = byteBuffer.asIntBuffer();
		this.vertexCount = vertexCount;
		this.reversed = reversed;
	}

	@Override
	public void add(int... indices) {
		if (indices == null || indices.length != vertexCount) {
			throw new IllegalArgumentException("Invalid number of indices!");
		}
//		LOGGER.info(String.format("\n%3d", size) + ": " + Arrays.toString(indices));
		if (reversed) {
			for (int i = 0; i < indices.length / 2; i++) {
				int j = indices.length - i - 1;
				int tmp = indices[i];
				indices[i] = indices[j];
				indices[j] = tmp;
			}
		}

		intBuffer.put(indices);

		size++;
	}

	@Override
	public IntBuffer finalizeIntBuffer() {
		intBuffer.position(0);
		return intBuffer;
	}

	@Override
	public ByteBuffer finalizeBuffer() {
		byteBuffer.limit(Integer.BYTES * vertexCount * size);
		byteBuffer.position(0);
		return byteBuffer;
	}


	@Override
	public int size() {
		return size;
	}
	
	
}
