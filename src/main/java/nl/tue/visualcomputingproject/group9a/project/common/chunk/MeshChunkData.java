package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.*;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Chunk data containing a renderable mesh.
 */
@Value
public class MeshChunkData {
	/**
	 * The type of the vertex buffer.
	 */
	@NonNull
	VertexBufferType vertexBufferType;
	/**
	 * The type of the mesh buffer.
	 */
	@NonNull
	MeshBufferType meshBufferType;
	/**
	 * The number of elements in the vertex buffer.
	 */
	int size;
	/**
	 * The buffer storing the vertex data.
	 */
	@NonNull
	@Getter(AccessLevel.NONE)
	ByteBuffer vertexBuffer;
	/**
	 * The buffer storing the mesh data.
	 */
	@NonNull
	@Getter(AccessLevel.NONE)
	ByteBuffer meshBuffer;

	/**
	 * Creates a new data object.
	 *
	 * @param vertexBufferType The type of the vertex buffer.
	 * @param meshBufferType   The type of the mesh buffer.
	 * @param size             The number of elements in the vertex buffer.
	 * @param vertexBuffer     The buffer storing the vertex data. Will only be used as a read-only buffer.
	 * @param meshBuffer       The buffer storing the mesh data. WIll only be used as a read-only buffer.
	 */
	public MeshChunkData(
			VertexBufferType vertexBufferType,
			MeshBufferType meshBufferType,
			int size,
			ByteBuffer vertexBuffer,
			ByteBuffer meshBuffer) {
		this.vertexBufferType = vertexBufferType;
		this.meshBufferType = meshBufferType;
		this.size = size;
		if (vertexBuffer.isReadOnly()) {
			this.vertexBuffer = vertexBuffer;
		} else {
			this.vertexBuffer = vertexBuffer.asReadOnlyBuffer();
		}
		if (meshBuffer.isReadOnly()) {
			this.meshBuffer = meshBuffer;
		} else {
			this.meshBuffer = meshBuffer.asReadOnlyBuffer();
		}
	}

	/**
	 * @return The buffer storing the vertex data as a {@link FloatBuffer}.
	 */
	public FloatBuffer getVertexBuffer() {
		return vertexBuffer.asFloatBuffer();
	}

	/**
	 * @return The buffer storing the mesh data as a {@link IntBuffer}.
	 */
	public IntBuffer getMeshBuffer() {
		return meshBuffer.asIntBuffer();
	}

	/**
	 * Factory class for serializing and deserializing a {@link MeshChunkData} object.
	 */
	private static class MeshChunkDataCacheFactory
			implements CacheFactory<MeshChunkData> {
		
		@Override
		public void serialize(OutputStream os, MeshChunkData mcd)
				throws IOException {
			CacheFactory.writeInt(os, mcd.vertexBufferType.ordinal());
			CacheFactory.writeInt(os, mcd.meshBufferType.ordinal());
			CacheFactory.writeInt(os, mcd.size);
			CacheFactory.writeByteBuffer(os, mcd.vertexBuffer);
			CacheFactory.writeByteBuffer(os, mcd.meshBuffer);
		}

		@Override
		public MeshChunkData deserialize(InputStream is)
				throws IOException {
			VertexBufferType vertexBufferType = VertexBufferType.values()[CacheFactory.readInt(is)];
			MeshBufferType meshBufferType = MeshBufferType.values()[CacheFactory.readInt(is)];
			int size = CacheFactory.readInt(is);
			ByteBuffer vertexBuffer = CacheFactory.readBuffer(is);
			ByteBuffer meshBuffer = CacheFactory.readBuffer(is);
			return new MeshChunkData(
					vertexBufferType,
					meshBufferType,
					size,
					vertexBuffer,
					meshBuffer);
		}

	}

	/**
	 * @return A {@link CacheFactory} used to serialize and deserialize a {@link MeshChunkData} object.
	 */
	public static CacheFactory<MeshChunkData> createCacheFactory() {
		return new MeshChunkDataCacheFactory();
	}
	
}
