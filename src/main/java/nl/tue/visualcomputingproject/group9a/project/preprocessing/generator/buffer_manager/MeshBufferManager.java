package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.buffer_manager;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.MeshBufferType;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Interface for buffer managers storing mesh data.
 */
public interface MeshBufferManager
		extends BufferManager {

	/**
	 * Adds some indices to the buffer.
	 * All indices form a single face.
	 * 
	 * @param indices The indices to add.
	 */
	void add(int... indices);

	/**
	 * Finalizes the buffer for reading and returns it as an {@link IntBuffer}.
	 * Both this method as well as {@link #finalizeBuffer()} should
	 * in total be called once.
	 * Calling either of them more than once or both will result in undefined behaviour.
	 * 
	 * @deprecated A {@link ByteBuffer} can always be converted to an {@link IntBuffer}
	 *     without major loss of performance, but not the other way around.
	 * 
	 * @return The finalized mesh buffer ready for reading.
	 * 
	 * @see #finalizeBuffer()
	 */
	@Deprecated
	default IntBuffer finalizeIntBuffer() {
		return finalizeBuffer().asIntBuffer();
	}

	/**
	 * {@inheritDoc}
	 * <br>
	 * Calling this function after {@link #finalizeIntBuffer()} has been called
	 * will result in undefined behaviour as well.
	 * 
	 * @see #finalizeIntBuffer()
	 */
	ByteBuffer finalizeBuffer();

	/**
	 * Creates a mesh buffer manager using the given information.
	 * 
	 * @param quality     The quality of the mesh.
	 * @param type        The type of the mesh.
	 * @param width       The width of the data array. Only used for interpolated data.
	 * @param height      The height of the data array. Only used for interpolated data.
	 * @param numVertices The number of vertices in the dataset. Only used for raw data.
	 *    
	 * @return A buffer manager suitable for storing the data with the right capacity.
	 */
	static MeshBufferManager createManagerFor(
			QualityLevel quality,
			MeshBufferType type,
			int width, int height,
			int numVertices) {
		switch (quality) {
			case FIVE_BY_FIVE:
			case HALF_BY_HALF:
				switch (type) {
					case TRIANGLES_CLOCKWISE_3_INT:
					case TRIANGLES_COUNTER_CLOCKWISE_3_INT:
						return new MeshIntBufferManager(
								3,
								(width-1) * (height-1) * 2,
								type.isClockwise());
					case QUADS_CLOCKWISE_4_INT:
					case QUADS_COUNTER_CLOCKWISE_4_INT:
						return new MeshIntBufferManager(
								4,
								(width-1) * (height-1),
								type.isClockwise());
					default:
						throw new IllegalArgumentException("Invalid vertex buffer type: " + type);
				}
			case LAS:
				switch (type) {
					case TRIANGLES_CLOCKWISE_3_INT:
					case TRIANGLES_COUNTER_CLOCKWISE_3_INT:
						return new MeshIntBufferManager(
								3,
								numVertices * 2 - 5,
								type.isClockwise());
					case QUADS_CLOCKWISE_4_INT:
					case QUADS_COUNTER_CLOCKWISE_4_INT:
						return new MeshIntBufferManager(
								4,
								numVertices - 3,
								type.isClockwise());
					default:
						throw new IllegalArgumentException("Invalid vertex buffer type: " + type);
				}
			default:
				throw new IllegalArgumentException("Invalid quality level: " + quality);
		}
	}

	/**
	 * Creates a mesh buffer manager solely based on the mesh type and number of faces.
	 * 
	 * @param type     The type of the mesh.
	 * @param numFaces The number of faces to reserve data for.
	 *    
	 * @return A suitable mesh buffer with the space to store the given number of faces.
	 */
	static MeshBufferManager createManagerFor(MeshBufferType type, int numFaces) {
		switch (type) {
			case TRIANGLES_CLOCKWISE_3_INT:
			case TRIANGLES_COUNTER_CLOCKWISE_3_INT:
				return new MeshIntBufferManager(
						3,
						numFaces,
						type.isClockwise());
			case QUADS_CLOCKWISE_4_INT:
			case QUADS_COUNTER_CLOCKWISE_4_INT:
				return new MeshIntBufferManager(
						4,
						numFaces,
						type.isClockwise());
			default:
				throw new IllegalArgumentException("Invalid vertex buffer type: " + type);
		}
	}
	
}
