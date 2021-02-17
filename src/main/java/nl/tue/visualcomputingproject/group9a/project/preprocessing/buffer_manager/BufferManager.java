package nl.tue.visualcomputingproject.group9a.project.preprocessing.buffer_manager;

import java.nio.ByteBuffer;

/**
 * Interface for classes managing a {@link ByteBuffer}.
 */
public interface BufferManager {

	/**
	 * Finalizes the buffer for reading and returns it.
	 * This method should only be called once.
	 * Calling it more than once will result in undefined behaviour.
	 * No data can be added to the buffer after this method has been called.
	 * 
	 * @return The finalized buffer ready for reading.
	 */
	ByteBuffer finalizeBuffer();
	
	/**
	 * The number of elements in the buffer.
	 */
	int size();
	
}
