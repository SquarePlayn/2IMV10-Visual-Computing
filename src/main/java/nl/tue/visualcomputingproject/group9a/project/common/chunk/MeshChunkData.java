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
	@NonNull
	VertexBufferType vertexBufferType;
	@NonNull
	MeshBufferType meshBufferType;
	int size;
	@NonNull
	@Getter(AccessLevel.NONE)
	ByteBuffer vertexBuffer;
	@NonNull
	@Getter(AccessLevel.NONE)
	ByteBuffer meshBuffer;
	
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
	
	public FloatBuffer getVertexBuffer() {
		return vertexBuffer.asFloatBuffer();
	}
	
	public IntBuffer getMeshBuffer() {
		return meshBuffer.asIntBuffer();
	}
	
	public static CacheFactory<MeshChunkData> createCacheFactory() {
		return new CacheFactory<MeshChunkData>() {
			@Override
			public void serialize(OutputStream out, MeshChunkData mcd)
					throws IOException {
				CacheFactory.writeInt(out, mcd.vertexBufferType.ordinal());
				CacheFactory.writeInt(out, mcd.meshBufferType.ordinal());
				CacheFactory.writeInt(out, mcd.size);
				CacheFactory.writeByteBuffer(out, mcd.vertexBuffer);
				CacheFactory.writeByteBuffer(out, mcd.meshBuffer);
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
		};
	}
	
	
}
