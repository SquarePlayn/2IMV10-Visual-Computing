package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.*;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheableObject;
import nl.tue.visualcomputingproject.group9a.project.common.cache.ObjectSerializer;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Chunk data containing a renderable mesh.
 */
@Value
public class MeshChunkData
		implements CacheableObject {
	/** The buffer storing the vertex data. */
	@NonNull
	@Getter(AccessLevel.NONE)
	ByteBuffer vertexBuffer;
	/** The buffer storing the mesh data. */
	@NonNull
	@Getter(AccessLevel.NONE)
	ByteBuffer meshBuffer;

	/**
	 * Creates a new data object.
	 * 
	 * @param vertexBuffer     The buffer storing the vertex data. Will only be used as a read-only buffer.
	 * @param meshBuffer       The buffer storing the mesh data. WIll only be used as a read-only buffer.
	 */
	public MeshChunkData(
			ByteBuffer vertexBuffer,
			ByteBuffer meshBuffer) {
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

	@Override
	public long memorySize() {
		return vertexBuffer.capacity() + meshBuffer.capacity() + 2*8;
	}

	/**
	 * Factory class for serializing and deserializing a {@link MeshChunkData} object.
	 */
	private static class MeshChunkDataSerializer
			implements ObjectSerializer<MeshChunkData> {
		
		@Override
		public void serialize(OutputStream os, MeshChunkData mcd)
				throws IOException {
			ObjectSerializer.writeByteBuffer(os, mcd.vertexBuffer);
			ObjectSerializer.writeByteBuffer(os, mcd.meshBuffer);
		}

		@Override
		public MeshChunkData deserialize(InputStream is)
				throws IOException {
			ByteBuffer vertexBuffer = ObjectSerializer.readBuffer(is);
			ByteBuffer meshBuffer = ObjectSerializer.readBuffer(is);
			return new MeshChunkData(
					vertexBuffer,
					meshBuffer);
		}
		
	}

	/**
	 * @return A {@link ObjectSerializer} used to serialize and deserialize a {@link MeshChunkData} object.
	 */
	public static ObjectSerializer<MeshChunkData> createCacheFactory() {
		return new MeshChunkDataSerializer();
	}
	
}
