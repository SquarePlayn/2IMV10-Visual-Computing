package nl.tue.visualcomputingproject.group9a.project.renderer.engine.shaders;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;

public abstract class ShaderProgram {

	private int programID;
	private int vertexShaderID;
	private int fragmentShaderID;

	// Float buffer used for loading 4x4 matrices into the shader's uniform variables
	private static FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

	public ShaderProgram(String vertexFile, String fragmentFile) {
		// Load the shaders
		vertexShaderID = loadShader(vertexFile, GL20.GL_VERTEX_SHADER);
		fragmentShaderID = loadShader(fragmentFile, GL20.GL_FRAGMENT_SHADER);

		// Attach them
		programID = GL20.glCreateProgram();
		GL20.glAttachShader(programID, vertexShaderID);
		GL20.glAttachShader(programID, fragmentShaderID);
		bindAttributes();
		GL20.glLinkProgram(programID);

		// Validate it all went okay
		GL20.glValidateProgram(programID);

		// Ensure all uniform locations are loaded
		getAllUniformLocations();
	}

	/**
	 * TODO Description
	 */
	protected abstract void getAllUniformLocations();

	/**
	 * TODO Description
	 */
	protected abstract void bindAttributes();

	/**
	 * Get the location ID of a uniform variable to bind to the vertex shader
	 *
	 * @param uniformName The name of the variable in the shader
	 * @return The location ID of the shader variable with the name {@code uniformName}.
	 */
	protected int getUniformLocation(String uniformName) {
		return GL20.glGetUniformLocation(programID, uniformName);
	}

	/**
	 * Start this shader program
	 */
	public void start() {
		GL20.glUseProgram(programID);
	}

	/**
	 * Stop this shader program (but don't remove it)
	 */
	public void stop() {
		GL20.glUseProgram(0);
	}

	/**
	 * Remove the shaders and the shader program itself
	 */
	public void cleanup() {
		// Make sure the program is stopped
		stop();

		// Unbind program and shaders
		GL20.glDetachShader(programID, vertexShaderID);
		GL20.glDetachShader(programID, fragmentShaderID);
		GL20.glDeleteProgram(vertexShaderID);
		GL20.glDeleteProgram(fragmentShaderID);
		GL20.glDeleteProgram(programID);
	}

	/**
	 * Bind an attribute to a variable name for the shader
	 *
	 * @param attribute    Attribute ID to bind
	 * @param variableName Name of the shader variable to bind the attibute to
	 */
	protected void bindAttribute(int attribute, String variableName) {
		GL20.glBindAttribLocation(programID, attribute, variableName);
	}

	/**
	 * Load a float into a uniform variable in the shader
	 *
	 * @param location Location ID of the uniform variable to load into
	 * @param value    Value to load into the uniform variable
	 */
	protected void loadFloat(int location, float value) {
		GL20.glUniform1f(location, value);
	}

	/**
	 * Load an integer into a uniform variable in the shader
	 *
	 * @param location Location ID of the uniform variable to load into
	 * @param value    Value to load into the uniform variable
	 */
	protected void loadInt(int location, int value) {
		GL20.glUniform1i(location, value);
	}

	/**
	 * Load a boolean into a uniform variable in the shader
	 *
	 * @param location Location ID of the uniform variable to load into
	 * @param value    Value to load into the uniform variable
	 */
	protected void loadBoolean(int location, boolean value) {
		GL20.glUniform1f(location, value ? 1 : 0);
	}

	/**
	 * Load a Vector3f into a uniform variable in the shader
	 *
	 * @param location Location ID of the uniform variable to load into
	 * @param vector   Vector to load into the uniform variable
	 */
	protected void loadVector3f(int location, Vector3f vector) {
		GL20.glUniform3f(location, vector.x, vector.y, vector.z);
	}

	/**
	 * Load a Matrix4f into a uniform variable in the shader
	 *
	 * @param location Location ID of the uniform variable to load into
	 * @param matrix   Matrix to load into the uniform variable
	 */
	protected void loadMatrix4f(int location, Matrix4f matrix) {
		// Load the matrix data into the temporary matrixBuffer
		matrix.get(matrixBuffer);

		// Send the matrix buffer to the shader
		GL20.glUniformMatrix4fv(location, false, matrixBuffer);
	}

	/**
	 * Load a shader
	 * NB: This function comes directly from the ThinMatrix LWJGL3.0 tutorial
	 *
	 * @param file shader to load
	 * @param type type of shader
	 * @return shader ID of the loaded shader
	 */
	private static int loadShader(String file, int type) {
		StringBuilder shaderSource = new StringBuilder();

		try {
			InputStream inputStream = ShaderProgram.class.getResourceAsStream(file);
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			String line;

			while ((line = reader.readLine()) != null) {
				shaderSource.append(line).append("\n");
			}
			reader.close();
		} catch (IOException exception) {
			System.err.println("Could not read file " + file);
			exception.printStackTrace();
			System.exit(-1);
		}

		int shaderID = GL20.glCreateShader(type);
		GL20.glShaderSource(shaderID, shaderSource);
		GL20.glCompileShader(shaderID);
		if (GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
			System.out.println(GL20.glGetShaderInfoLog(shaderID, 500));
			System.err.println("Could not compile the shader.");
			System.exit(-1);
		}
		return shaderID;
	}
}
