package nl.tue.visualcomputingproject.group9a.project.common.cache;

import org.lwjgl.BufferUtils;

import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public interface CacheFactory<T> {
	int BLOCK_SIZE = 1024;
	
	void serialize(OutputStream out, T obj)
			throws IOException;
	
	T deserialize(InputStream is)
			throws IOException;
	
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
	
	static void writeDouble(OutputStream os, double d)
			throws IOException {
		writeLong(os, Double.doubleToLongBits(d));
	}
	
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
	
	static void writeFloat(OutputStream os, float f)
			throws IOException {
		writeInt(os, Float.floatToIntBits(f));
	}
	
	static void writeByteBuffer(OutputStream os, ByteBuffer buffer)
			throws IOException {
		writeInt(os, buffer.limit());
		if (buffer.hasArray()) {
			os.write(buffer.array());
			
		} else {
			if (buffer.limit() < BLOCK_SIZE) {
				byte[] buf = new byte[buffer.limit()];
				buffer.get(buf);
				os.write(buf);
				
			} else {
				byte[] buf = new byte[BLOCK_SIZE];
				while (buffer.hasRemaining()) {
					int read = Math.min(buf.length, buffer.remaining());
					buffer.get(buf);
					os.write(buf, 0, read);
				}
			}
		}
	}

	static void writeBuffer(OutputStream os, Buffer buffer)
			throws IOException {
		if (buffer instanceof ByteBuffer) {
			writeByteBuffer(os, (ByteBuffer) buffer);
		} else {
			// TODO
			throw new UnsupportedOperationException();
		}
	}
	
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
	
	static double readDouble(InputStream is)
			throws IOException {
		return Double.longBitsToDouble(readLong(is));
	}
	
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
	
	static float readFloat(InputStream is)
			throws IOException {
		return Float.intBitsToFloat(readInt(is));
	}
	
	static ByteBuffer readBuffer(InputStream is)
			throws IOException {
		int size = readInt(is);
		ByteBuffer buffer = BufferUtils.createByteBuffer(size);
		if (buffer.hasArray()) {
			int read = is.read(buffer.array());
			if (read != size) {
				throw new EOFException(size, read);
			}
			
		} else {
			if (size < BLOCK_SIZE) {
				byte[] buf = new byte[size];
				int read = is.read(buf);
				if (read != size) {
					throw new EOFException(size, read);
				}
				buffer.put(buf);
				
			} else {
				byte[] buf = new byte[BLOCK_SIZE];
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
		return buffer;
	}
	
}
