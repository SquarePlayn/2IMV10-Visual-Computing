package nl.tue.visualcomputingproject.group9a.project.renderer.engine.model;

import de.matthiasmann.twl.utils.PNGDecoder;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.utils.Maths;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

/**
 * Helper class for generating model object instances such as VAOs and VBOs
 */
public class Loader {

	private static final Collection<Integer> vaos = new HashSet<>();
	private static final Collection<Integer> vbos = new HashSet<>();
	private static final Collection<Integer> textures = new HashSet<>();

	/**
	 * Make a RawModel in a VAO from a set of positions and indices
	 *
	 * @return RawModel created from the input
	 */
	public static RawModel loadToVAO(
			float[] positions,
			int[] indices,
			Vector2f offset,
			int texId) {
		FloatBuffer positionsBuffer = storeDataInFloatBuffer(positions);
		IntBuffer indicesBuffer = storeDataInIntBuffer(indices);
		return loadToVAO(positionsBuffer, indicesBuffer, offset, texId);
	}

	public static RawModel loadToVAO(
			FloatBuffer vertices,
			IntBuffer indices,
			Vector2f offset,
			int texId) {
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
				false,
				vaoID,
				indicesCount,
				Maths.createTransformationMatrix(new Vector3f(offset.x, 0, offset.y), 0, 0, 0, 1),
				modelVBOs,
				texId
		);
	}

	/**
	 * Unload the GPU data (VAO and VBOs) of a model
	 *
	 * @param model         Model to unload
	 * @param unloadTexture Whether to unload the texture
	 */
	public static void unloadModel(RawModel model, boolean unloadTexture) {
		model.setUnloaded(true);
		vbos.removeAll(model.getVboIds());
		model.getVboIds().forEach(GL15::glDeleteBuffers);
		vaos.remove(model.getVaoId());
		GL30.glDeleteVertexArrays(model.getVaoId());
		if (model.getTexId() >= 0 && unloadTexture) {
			textures.remove(model.getTexId());
			GL11.glDeleteTextures(model.getTexId());
		}
	}

	/**
	 * Unload the GPU data (VAO and VBOs) of a model
	 *
	 * @param model Model to unload
	 */
	public static void unloadModel(RawModel model) {
		unloadModel(model, true);
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
		for (int texture : textures) {
			GL11.glDeleteTextures(texture);
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
			int floatsOffset) {

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

	/**
	 * Load a skybox set of vertices into a renderable raw model
	 */
	public static RawModel loadSkyboxModel(float[] vertices) {
		int vao = createVAO();
		bindVAO(vao);
		int vbo = createVBO();
		storeBufferInVBO(vbo, storeDataInFloatBuffer(vertices));
		setAttributePointer(vbo, 0, 3, 3, 0);
		unbindVAO();
		return new RawModel(
				false,
				vao,
				vertices.length / 3,
				Maths.createTransformationMatrix(new Vector3f(), 0, 0, 0, 1),
				Collections.singletonList(vbo),
				-1
		);
	}

	/**
	 * Load series of images (together forming the cubemap) into a texture
	 *
	 * @param textureFiles String array of the 6 texture files of the cubemap
	 * @return int texture ID of CubeMap texture
	 */
	public static int loadCubeMapTextures(String[] textureFiles) {
		int texID = GL11.glGenTextures(); // Generate empty texture
		GL13.glActiveTexture(GL13.GL_TEXTURE0); // Activate texture unit 0
		GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, texID); // Bind texture to texture unit 0

		for (int i = 0; i < textureFiles.length; i++) {
			int faceNumber = GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i;
			String fileName = "/skybox/" + textureFiles[i] + ".png";
			InputStream is = null;

			if (faceNumber == GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_Y || faceNumber == GL13.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y) {
				try {
					BufferedImage bi = ImageIO.read(Loader.class.getResourceAsStream(fileName)); // read to bufferImage

					AffineTransform tx = AffineTransform.getScaleInstance(-1, -1); // Flip horizontally and vertically
					tx.translate(-bi.getWidth(null), -bi.getHeight(null));
					AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
					bi = op.filter(bi, null);

					ByteArrayOutputStream os = new ByteArrayOutputStream(); // From bufferedImage to InputStream
					ImageIO.write(bi, "png", os);
					is = new ByteArrayInputStream(os.toByteArray());
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				is = Loader.class.getResourceAsStream(fileName); // From path-location to InputStream
			}
			Texture data = decodeTextureFile(is); // decode InputStream

			GL11.glTexImage2D(GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL11.GL_RGBA,
					data.getWidth(), data.getHeight(), 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
					data.getBuffer());
		}
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

		textures.add(texID);
		return texID;
	}

	/**
	 * Decode an image from an InputStream into a byteBuffer and return it as a TextureData object
	 *
	 * @param in InputStream containing the image
	 * @return TextureData containing decoded image
	 */
	public static Texture decodeTextureFile(InputStream in) {
		int width = 0;
		int height = 0;
		ByteBuffer buffer = null;
		try {
			PNGDecoder decoder = new PNGDecoder(in);
			width = decoder.getWidth();
			height = decoder.getHeight();
			buffer = ByteBuffer.allocateDirect(4 * width * height); // Allocate byte buffer of size (4 * width * height)
			decoder.decode(buffer, width * 4, PNGDecoder.Format.RGBA); // decode image and load into buffer

			buffer.flip(); // Flip from write to read
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Tried to load texture , didn't work");
			System.exit(-1);
		}
		return new Texture(width, height, buffer);
	}

	public static int loadTexture(ByteBuffer image, int width, int height) {
		int texID = GL11.glGenTextures(); // Generate empty texture
		GL13.glActiveTexture(GL13.GL_TEXTURE1); // Activate texture unit 1
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL13.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL13.GL_CLAMP_TO_EDGE);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
				GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,image);
		GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);
		GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

		textures.add(texID);
		return texID;
	}
	
}
