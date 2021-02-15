package nl.tue.visualcomputingproject.group9a.project.preprocessing.BufferManager;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.VertexBufferType;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * Interface for buffer managers storing vertex data.
 */
public interface VertexBufferManager
		extends BufferManager {

	/**
	 * Adds the given vertex and normal with {@code double} precision to the buffer.
	 * 
	 * @param v The vertex to add.
	 * @param n The normal to add.
	 *    
	 * @return The index of the vertex and normal pair in the buffer.
	 */
	default int addVertex(Vector3d v, Vector3d n) {
		return addVertex(v.x, v.y, v.z, n.x, n.y, n.z);
	}

	/**
	 * Adds the given vertex and normal with {@code float} precision to the buffer.
	 *
	 * @param v The vertex to add.
	 * @param n The normal to add.
	 *
	 * @return The index of the vertex and normal pair in the buffer.
	 */
	default int addVertex(Vector3f v, Vector3f n) {
		return addVertex(v.x, v.y, v.z, n.x, n.y, n.z);
	}

	/**
	 * Adds the given vertex and normal with {@code double} precision to the buffer.
	 *
	 * @param vx The x-coordinate of the vertex.
	 * @param vy The y-coordinate of the vertex.
	 * @param vz The z-coordinate of the vertex.
	 * @param nx The x-coordinate of the normal.
	 * @param ny The y-coordinate of the normal.
	 * @param nz The z-coordinate of the normal.
	 *
	 * @return The index of the vertex and normal pair in the buffer.
	 */
	int addVertex(double vx, double vy, double vz,
				   double nx, double ny, double nz);

	/**
	 * Adds the given vertex and normal with {@code float} precision to the buffer.
	 *
	 * @param vx The x-coordinate of the vertex.
	 * @param vy The y-coordinate of the vertex.
	 * @param vz The z-coordinate of the vertex.
	 * @param nx The x-coordinate of the normal.
	 * @param ny The y-coordinate of the normal.
	 * @param nz The z-coordinate of the normal.
	 *
	 * @return The index of the vertex and normal pair in the buffer.
	 */
	int addVertex(float vx, float vy, float vz,
				   float nx, float ny, float nz);
	
	/**
	 * Finalizes the buffer for reading and returns it as an {@link FloatBuffer}.
	 * Both this method as well as {@link #finalizeBuffer()} should
	 * in total be called once.
	 * Calling either of them more than once or both will result in undefined behaviour.
	 * 
	 * @deprecated A {@link ByteBuffer} can always be converted to a {@link FloatBuffer}
	 *     without major loss of performance, but not the other way around.
	 *
	 * @return The finalized vertex buffer ready for reading.
	 *
	 * @see #finalizeBuffer()
	 */
	@Deprecated
	default FloatBuffer finalizeFloatBuffer() {
		return finalizeBuffer().asFloatBuffer();
	}

	/**
	 * {@inheritDoc}
	 * <br>
	 * Calling this function after {@link #finalizeFloatBuffer()} has been called
	 * will result in undefined behaviour as well.
	 *
	 * @see #finalizeFloatBuffer()
	 */
	ByteBuffer finalizeBuffer();

	static VertexBufferManager createManagerFor(VertexBufferType type, int numVertices) {
		switch (type) {
			case VERTEX_3_FLOAT_NORMAL_3_FLOAT:
				return new SeparatedVertexFloatBufferManager(numVertices);
			case INTERLEAVED_VERTEX_3_FLOAT_NORMAL_3_FLOAT:
				return new InterleavedVertexFloatBufferManager(numVertices);
			default:
				throw new IllegalArgumentException("Invalid vertex buffer type: " + type);
		}
	}
	
}
