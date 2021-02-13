package nl.tue.visualcomputingproject.group9a.project.common.cache;

import org.lwjgl.BufferUtils;

import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Factory class for serialization and deserialization of a target class {@code T}. <br>
 * <br>
 * If file separators are used during serialization, then the files will be stored
 * exactly at that path when used in the {@link CacheManager}.
 * When deserialized, it will provide the exact same string as was produced during
 * serialization.
 * 
 * @param <T> The target class.
 */
public interface CacheFactory<T> {
	/** The buffer size used for the byte buffers. */
	int BUFFER_SIZE = 1024;

	/**
	 * Serializes the given target object to the output stream.
	 * 
	 * @param os The stream to output the data to.
	 * @param obj The object to serialize.
	 * 
	 * @throws IOException If some IO exception occurs.
	 */
	void serialize(OutputStream os, T obj)
			throws IOException;

	/**
	 * Deserializes the given target object to the output stream.
	 *
	 * @param is The stream to read the data from.
	 *
	 * @throws IOException If some IO exception occurs.
	 * @throws EOFException If there is not enough data.
	 */
	T deserialize(InputStream is)
			throws IOException;

	/**
	 * Serializes a {@code long} to the output stream.
	 *
	 * @param os The stream to output the data to.
	 * @param l  The {@code long} to serialize.
	 * 
	 * @throws IOException If some IO exception occurs.
	 */
	static void writeLong(OutputStream os, long l)
			throws IOException {
		byte[] data = new byte[] {
				(byte) (l >> 56),
				(byte) ((l >> 48) & 0xFF),
				(byte) ((l >> 40) & 0xFF),
				(byte) ((l >> 32) & 0xFF),
				(byte) ((l >> 24) & 0xFF),
				(byte) ((l >> 16) & 0xFF),
				(byte) ((l >> 8) & 0xFF),
				(byte) (l & 0xFF)
		};
		os.write(data);
	}

	/**
	 * Serializes a {@code double} to the output stream.
	 *
	 * @param os The stream to output the data to.
	 * @param d  The {@code double} to serialize.
	 *
	 * @throws IOException If some IO exception occurs.
	 */
	static void writeDouble(OutputStream os, double d)
			throws IOException {
		writeLong(os, Double.doubleToLongBits(d));
	}

	/**
	 * Serializes an {@code int} to the output stream.
	 *
	 * @param os The stream to output the data to.
	 * @param i  The {@code int} to serialize.
	 *
	 * @throws IOException If some IO exception occurs.
	 */
	static void writeInt(OutputStream os, int i)
			throws IOException {
		byte[] data = new byte[] {
				(byte) (i >> 24),
				(byte) ((i >> 16) & 0xFF),
				(byte) ((i >> 8) & 0xFF),
				(byte) (i & 0xFF)
		};
		os.write(data);
	}

	/**
	 * Serializes a {@code float} to the output stream.
	 *
	 * @param os The stream to output the data to.
	 * @param f  The {@code float} to serialize.
	 *
	 * @throws IOException If some IO exception occurs.
	 */
	static void writeFloat(OutputStream os, float f)
			throws IOException {
		writeInt(os, Float.floatToIntBits(f));
	}

	/**
	 * Serializes a {@link ByteBuffer} to the output stream.
	 * It serializes all data between the current position and the limit
	 * of the buffer.
	 *
	 * @param os      The stream to output the data to.
	 * @param buffer  The {@link ByteBuffer} to serialize.
	 *
	 * @throws IOException If some IO exception occurs.
	 */
	static void writeByteBuffer(OutputStream os, ByteBuffer buffer)
			throws IOException {
		writeInt(os, buffer.limit() - buffer.position());
		if (buffer.hasArray()) {
			os.write(buffer.array());
			
		} else {
			if (buffer.limit() < BUFFER_SIZE) {
				byte[] buf = new byte[buffer.limit()];
				buffer.get(buf);
				os.write(buf);
				
			} else {
				byte[] buf = new byte[BUFFER_SIZE];
				while (buffer.hasRemaining()) {
					int read = Math.min(buf.length, buffer.remaining());
					buffer.get(buf);
					os.write(buf, 0, read);
				}
			}
		}
	}

	/**
	 * Serializes a generic {@link Buffer} to the output stream.
	 *
	 * @param os     The stream to output the data to.
	 * @param buffer The buffer to serialize.
	 *
	 * @throws IOException If some IO exception occurs.
	 * @throws UnsupportedOperationException If the buffer is not a {@link ByteBuffer}.
	 */
	static void writeBuffer(OutputStream os, Buffer buffer)
			throws IOException {
		if (buffer instanceof ByteBuffer) {
			writeByteBuffer(os, (ByteBuffer) buffer);
		} else {
			// TODO
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Deserializes a {@code long} from the input stream.
	 *
	 * @param is The stream to read the data from.
	 * @return A deserialized {@code long}.
	 * 
	 * @throws IOException If some IO exception occurs.
	 * @throws EOFException If there is not enough data.
	 */
	static long readLong(InputStream is)
			throws IOException {
		byte[] data = new byte[8];
		int read = is.read(data);
		if (read != 4) {
			throw new EOFException(4, read);
		}
		return ((long) data[0] << 56) |
				((long) data[1] << 48) |
				((long) data[2] << 40) |
				((long) data[3] << 32) |
				(data[4] << 24) |
				(data[5] << 16) |
				(data[6] << 8) |
				data[7];
	}

	/**
	 * Deserializes a {@code double} from the input stream.
	 *
	 * @param is The stream to read the data from.
	 * @return A deserialized {@code double}.
	 *
	 * @throws IOException If some IO exception occurs.
	 * @throws EOFException If there is not enough data.
	 */
	static double readDouble(InputStream is)
			throws IOException {
		return Double.longBitsToDouble(readLong(is));
	}

	/**
	 * Deserializes a {@code int} from the input stream.
	 *
	 * @param is The stream to read the data from.
	 * @return A deserialized {@code int}.
	 *
	 * @throws IOException If some IO exception occurs.
	 * @throws EOFException If there is not enough data.
	 */
	static int readInt(InputStream is)
			throws IOException {
		byte[] data = new byte[4];
		int read = is.read(data);
		if (read != 4) {
			throw new EOFException(4, read);
		}
		return (data[0] << 24) |
				(data[1] << 16) |
				(data[2] << 8) |
				data[3];
	}

	/**
	 * Deserializes a {@code float} from the input stream.
	 *
	 * @param is The stream to read the data from.
	 * @return A deserialized {@code float}.
	 *
	 * @throws IOException If some IO exception occurs.
	 * @throws EOFException If there is not enough data.
	 */
	static float readFloat(InputStream is)
			throws IOException {
		return Float.intBitsToFloat(readInt(is));
	}

	/**
	 * Deserializes a {@link ByteBuffer} from the input stream.
	 * The size of the buffer is the same difference between the position and
	 * limit of the serialized buffer with the corresponding elements in between.
	 *
	 * @param is The stream to read the data from.
	 * @return A deserialized {@link ByteBuffer}.
	 *
	 * @throws IOException If some IO exception occurs.
	 * @throws EOFException If there is not enough data.
	 */
	static ByteBuffer readBuffer(InputStream is)
			throws IOException {
		int size = readInt(is);
		ByteBuffer buffer = BufferUtils.createByteBuffer(size);
		if (buffer.hasArray()) {
			// If the buffer is backed by an array, you can directly
			// write into the buffer.
			int read = is.read(buffer.array());
			if (read != size) {
				throw new EOFException(size, read);
			}
			
		} else {
			// If the buffer is not backed by an array, we have to
			// use an intermediate buffer.
			if (size < BUFFER_SIZE) {
				// If the buffer size if very small, then allocate a
				// custom sized array and do everything in one go.
				byte[] buf = new byte[size];
				int read = is.read(buf);
				if (read != size) {
					throw new EOFException(size, read);
				}
				buffer.put(buf);
				
			} else {
				// If the buffer size is large, copy the data in parts.
				byte[] buf = new byte[BUFFER_SIZE];
				int read = 0;
				do {
					int newRead = is.read(buf);
					if (newRead == -1 || (read += newRead) > size) {
						throw new EOFException(size, read);
					}
					buffer.put(buf, 0, newRead);
				} while (read < size);
			}
		}
		
		// Set the buffer to reading mode.
		buffer.flip();
		return buffer;
	}
	
}
