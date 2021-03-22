package nl.tue.visualcomputingproject.group9a.project.renderer.engine.model;

import nl.tue.visualcomputingproject.group9a.project.renderer.engine.utils.Maths;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Helper class for generating model object instances such as VAOs and VBOs
 */
public class Loader {

	private static final Collection<Integer> vaos = new HashSet<>();
	private static final Collection<Integer> vbos = new HashSet<>();

	/**
	 * Make a RawModel in a VAO from a set of positions and indices
	 *
	 * @return RawModel created from the input
	 */
	public static RawModel loadToVAO(
			float[] positions,
			int[] indices,
			Vector2f offset
	) {
		FloatBuffer positionsBuffer = storeDataInFloatBuffer(positions);
		IntBuffer indicesBuffer = storeDataInIntBuffer(indices);
		return loadToVAO(positionsBuffer, indicesBuffer, offset);
	}

	public static RawModel loadToVAO(
			FloatBuffer vertices,
			IntBuffer indices,
			Vector2f offset
	) {
		// Determine how many indices there are
		int indicesCount = indices.remaining();

		// Create a new VAO
		int vaoID = createVAO();

		// Select the VAO to work on it
		bindVAO(vaoID);

		// Keep track of the generated vbos
		Collection<Integer> modelVBOs = new ArrayList<>();

		// Set the indices (in a vbo, but bound to the VAO in a special way)
		int indicesVBO = bindIndicesBuffer(indices);
		modelVBOs.add(indicesVBO);

		// Store other data in a VBO
		int vertexVBO = createVBO();
		modelVBOs.add(vertexVBO);
		storeBufferInVBO(vertexVBO, vertices);

		// Configure the attribute lists to point to the types of vertex data
		setAttributePointer(vertexVBO, 0, 3, 6, 0); // Positions
		setAttributePointer(vertexVBO, 1, 3, 6, 3); // Normals

		// Unbind the VAO as we are no longer working on it
		unbindVAO();

		return new RawModel(
				vaoID,
				indicesCount,
				Maths.createTransformationMatrix(new Vector3f(offset.x, 0, offset.y), 0, 0, 0, 1),
				modelVBOs
		);
	}

	public static void unloadModel(RawModel model) {
		// TODO Free VAO and VBOs of model
	}

	/**
	 * Delete all VAOs and VBOs
	 */
	public static void cleanup() {
		for (int vao : vaos) {
			GL30.glDeleteVertexArrays(vao);
		}
		for (int vbo : vbos) {
			GL15.glDeleteBuffers(vbo);
		}
	}

	/**
	 * Create a new VAO buffer
	 *
	 * @return ID of the newly created VAO buffer
	 */
	private static int createVAO() {
		int vaoID = GL30.glGenVertexArrays();
		vaos.add(vaoID);
		return vaoID;
	}

	/**
	 * Create a new VBO buffer
	 *
	 * @return ID of the newly created VBO buffer
	 */
	private static int createVBO() {
		int vboID = GL15.glGenBuffers();
		vbos.add(vboID);
		return vboID;
	}

	/**
	 * Bind a VAO to be the currently used one
	 *
	 * @param vaoId ID of the VAO to bind / activate
	 */
	private static void bindVAO(int vaoId) {
		GL30.glBindVertexArray(vaoId);
	}

	/**
	 * Unbind a VAO so it is no longer the currently used one
	 */
	private static void unbindVAO() {
		GL30.glBindVertexArray(0);
	}

	/**
	 * Bind a VBO to be the currently used one
	 *
	 * @param vboId ID of the VBO to bind / activate
	 */
	private static void bindVBO(int vboId) {
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
	}

	/**
	 * Unbind the VBO so it is no longer the currently used one
	 */
	private static void unbindVBO() {
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
	}

	/**
	 * Store data from a float buffer into a VBO
	 *
	 * @param vbo  ID of the VBO to store the data in
	 * @param data Data to store in the VBO
	 */
	private static void storeBufferInVBO(int vbo, FloatBuffer data) {
		bindVBO(vbo);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
		unbindVBO();
	}

	/**
	 * Bind an indices buffer to the VAO
	 *
	 * @param indices Indices to bind
	 * @return vboId of the vbo holding indices
	 */
	private static int bindIndicesBuffer(IntBuffer indices) {
		int vbo = createVBO();
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vbo);
		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);
		return vbo;
	}

	/**
	 * Attach data from a VBO to the VAO in attribute position {@code attributeNumber}.
	 * <p>
	 * NB: If multiple times of data are present in the VBO, data is assumed to be interleaved per vertex.
	 *
	 * @param vbo                  ID of the VBO to take the data from.
	 * @param attributeNumber      Attribute number to bind this data to in the VAO.
	 * @param floatsPerVertex      Number of float values this attribute has per vertex.
	 * @param totalFloatsPerVertex Number of float values (for any attribute) in the VBO per vertex.
	 * @param floatsOffset         Number of floats after position 0 to start from.
	 */
	private static void setAttributePointer(
			int vbo,
			int attributeNumber,
			int floatsPerVertex,
			int totalFloatsPerVertex,
			int floatsOffset
	) {

		// Enable the VBO so we can work on it here
		bindVBO(vbo);

		// Specify the location and organization in the VAO
		GL20.glVertexAttribPointer(
				attributeNumber, // Position in the VAO
				floatsPerVertex, // Number of data points per vertex
				GL11.GL_FLOAT, // Type of vertices
				false, // Not normalized
				Float.BYTES * totalFloatsPerVertex, // Distance between vertices in the array
				Float.BYTES * floatsOffset // Offset
		);

		// Unbind the VBO as we are no longer working on it
		unbindVBO();
	}

	/**
	 * Transform an array of integers to an int buffer
	 *
	 * @param data Data to convert
	 * @return IntBuffer instance containing the same integers as {@code data}
	 */
	private static IntBuffer storeDataInIntBuffer(int[] data) {
		IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
		buffer.put(data);
		buffer.flip();
		return buffer;
	}

	/**
	 * Transform an array of floats to a float buffer
	 *
	 * @param data Data to convert
	 * @return FloatBuffer instance containing the same floats as {@code data}
	 */
	private static FloatBuffer storeDataInFloatBuffer(float[] data) {
		FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
		buffer.put(data);
		buffer.flip();
		return buffer;
	}

}
