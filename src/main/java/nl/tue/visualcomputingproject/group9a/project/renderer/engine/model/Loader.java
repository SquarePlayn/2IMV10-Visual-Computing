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
import java.util.List;

/**
 * Helper class for generating model object instances such as VAOs and VBOs
 */
public class Loader {
	
	private static final List<Integer> vaos = new ArrayList<>();
	private static final List<Integer> vbos = new ArrayList<>();
	
	/**
	 * Make a RawModel in a VAO from a set of positions and indices
	 *
	 * @return RawModel created from the input
	 */
	public static RawModel loadToVAO(
		float[] positions,
		int[] indices,
		Vector2f offset) {
		FloatBuffer positionsBuffer = storeDataInFloatBuffer(positions);
		IntBuffer indicesBuffer = storeDataInIntBuffer(indices);
		return loadToVAO(positionsBuffer, indicesBuffer, offset);
	}
	
	public static RawModel loadToVAO(
		FloatBuffer vertices,
		IntBuffer indices,
		Vector2f offset) {
		// Determine how many indices there are
		int indicesCount = indices.remaining();
		
		// Create a new VAO
		int vaoID = createVAO();
		
		// Select the VAO to work on it
		bindVAO(vaoID);
		
		// Fill the VAO
		bindIndicesBuffer(indices);
		// Make a new VBO
		int vbo = createVBO();
		storeDataInAttributeList(vbo, 0, vertices, 3, 0);
		storeDataInAttributeList(vbo, 1, (FloatBuffer) null, 3, 3); // TODO: fix this
		// TODO Store more data such as normals
		
		// Unbind the VAO as we are no longer working on it
		unbindVAO();
		
		return new RawModel(
				vaoID,
				indicesCount,
				Maths.createTransformationMatrix(new Vector3f(offset.x, 0, offset.y), 0, 0, 0, 1));
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
	 * @param vaoID ID of the VAO to bind / activate
	 */
	private static void bindVAO(int vaoID) {
		GL30.glBindVertexArray(vaoID);
	}
	
	/**
	 * Unbind a VAO so it is no longer the currently used one
	 */
	private static void unbindVAO() {
		GL30.glBindVertexArray(0);
	}
	
	/**
	 * Bind an indices buffer to the VAO
	 *
	 * @param indices Indices to bind
	 */
	private static void bindIndicesBuffer(IntBuffer indices) {
		int vbo = createVBO();
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vbo);
		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);
	}
	
	/**
	 * Bind an indices buffer to the VAO
	 *
	 * @param indices Indices to bind
	 */
	private static void bindIndicesBuffer(int[] indices) {
		IntBuffer buffer = storeDataInIntBuffer(indices);
		bindIndicesBuffer(buffer);
	}
	
	/**
	 * Puts float data into a VBO, which is then put into the active VAO in position {@code attributeNumber}.
	 *
	 * @param vbo             The VBO pointer to which data is being stored.
	 * @param attributeNumber Position in the VAO to put the VBO
	 * @param data            Data to save
	 * @param dataSize        Size of the data to save
	 */
	private static void storeDataInAttributeList(int vbo, int attributeNumber, FloatBuffer data, int dataSize, int offset) {
		
		// Enable the VBO so we can work on it here
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
		
		//TODO: Refactor this method so we move buffer data to a separate call.
		if (data != null) {
			// Insert the data from the buffer
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
		}
		
		// Specify the location and organization in the VAO
		GL20.glVertexAttribPointer(
			attributeNumber, // Position in the VAO
			dataSize, // Number of data points per vertex
			GL11.GL_FLOAT, // Type of vertices
			false, // Not normalized
			Float.BYTES * 6, // Distance between vertices in the array TODO Parameterize
			Float.BYTES * offset // Offset
		);
		
		// Unbind the VBO as we are no longer working on it
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
	}
	
	/**
	 * Puts float data into a VBO, which is then put into the active VAO in position {@code attributeNumber}.
	 *
	 * @param vbo             The VBO pointer to which data is being stored.
	 * @param attributeNumber Position in the VAO to put the VBO
	 * @param data            Data to save
	 * @param dataSize        Size of the data to save
	 */
	private static void storeDataInAttributeList(int vbo, int attributeNumber, float[] data, int dataSize, int offset) {
		FloatBuffer buffer = storeDataInFloatBuffer(data);
		storeDataInAttributeList(vbo, attributeNumber, buffer, dataSize, offset);
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
