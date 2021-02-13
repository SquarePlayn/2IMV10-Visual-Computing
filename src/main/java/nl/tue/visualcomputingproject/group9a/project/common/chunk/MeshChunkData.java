package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.Value;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Chunk data containing a renderable mesh.
 */
@Value
public class MeshChunkData {
	VertexBufferType vertexBufferType;
	FloatBuffer vertexBuffer;
	MeshBufferType meshBufferType;
	IntBuffer meshBuffer;
	int size;
	
	public FloatBuffer getVertexBuffer() {
		return vertexBuffer.asReadOnlyBuffer();
	}
	
	public IntBuffer getMeshBuffer() {
		return meshBuffer.asReadOnlyBuffer();
	}
	
}
