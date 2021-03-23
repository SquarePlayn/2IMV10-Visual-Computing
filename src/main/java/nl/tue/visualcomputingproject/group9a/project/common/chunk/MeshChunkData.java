package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.*;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheableObject;
import nl.tue.visualcomputingproject.group9a.project.common.cache.ObjectSerializer;
import org.joml.Vector2f;

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
	/** The offset of this chunk. */
	@NonNull
	Vector2f offset;
	
	/**
	 * Creates a new data object.
	 * 
	 * @param vertexBuffer The buffer storing the vertex data. Will only be used as a read-only buffer.
	 * @param meshBuffer   The buffer storing the mesh data. WIll only be used as a read-only buffer.
	 * @param offset       The offset of this chunk.
	 */
	public MeshChunkData(
			@NonNull ByteBuffer vertexBuffer,
			@NonNull ByteBuffer meshBuffer,
			@NonNull Vector2f offset) {
		this.vertexBuffer = vertexBuffer;
		this.meshBuffer = meshBuffer;
		this.offset = offset;
		// TODO Check if we want to make them readonly here
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
	 * @return The offset of this chunk.
	 */
	public Vector2f getOffset() {
		return offset;
	}

	@Override
	public long memorySize() {
		return vertexBuffer.capacity() + meshBuffer.capacity() + 2*Float.BYTES;
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
			ObjectSerializer.writeFloat(os, mcd.offset.x());
			ObjectSerializer.writeFloat(os, mcd.offset.y());
		}

		@Override
		public MeshChunkData deserialize(InputStream is)
				throws IOException {
			ByteBuffer vertexBuffer = ObjectSerializer.readBuffer(is);
			ByteBuffer meshBuffer = ObjectSerializer.readBuffer(is);
			Vector2f offset = new Vector2f(
					ObjectSerializer.readFloat(is),
					ObjectSerializer.readFloat(is)
			);
					
			return new MeshChunkData(
					vertexBuffer,
					meshBuffer,
					offset
			);
		}
		
	}

	/**
	 * @return A {@link ObjectSerializer} used to serialize and deserialize a {@link MeshChunkData} object.
	 */
	public static ObjectSerializer<MeshChunkData> createSerializer() {
		return new MeshChunkDataSerializer();
	}
	
}
