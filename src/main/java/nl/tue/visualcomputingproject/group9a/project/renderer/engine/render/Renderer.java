package nl.tue.visualcomputingproject.group9a.project.renderer.engine.render;

import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Camera;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.io.Window;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.RawModel;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.shaders.StaticShader;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.utils.Maths;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class Renderer {
	private static final float FOV = 70;
	private static final float NEAR_PLANE = 0.1f;
	private static final float FAR_PLANE = 40000;

	/**
	 * The window the renderer is rendering on
	 */
	private final Window window;

	@Getter
	private Matrix4f projectionMatrix;

	public Renderer(Window window, StaticShader shader) {
		this.window = window;

		// Create the projection matrix
		createProjectionMatrix();

		// Load the projection matrix into the shader
		shader.start();
		shader.loadProjectionMatrix(projectionMatrix);
		shader.stop();
	}

	/**
	 * Render a model to the screen
	 *
	 * @param model  Model to render
	 * @param shader Shader to render the model with
	 * @param camera Camera of the screen currently active
	 */
	public void render(RawModel model, StaticShader shader, Camera camera) {
		// Activate the VAO and VBOs
		GL30.glBindVertexArray(model.getVaoID());
		GL20.glEnableVertexAttribArray(0); // Positions
		GL20.glEnableVertexAttribArray(1); // Normals

		// Load transformation matrix into the shader
		// TODO Wrap model in entity or something to be able to move it without modifying VBOs
		Matrix4f transformationMatrix = Maths.createTransformationMatrix(
				new Vector3f(0, 0, 0), 0, 0, 0, 1
		);
		shader.loadTransformationMatrix(transformationMatrix);

		// Load view matrix into the shader
		shader.loadViewMatrix(camera);

		// Wireframe rendering if enabled
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, camera.isWireframe() ? GL11.GL_LINE : GL11.GL_FILL);

		// Draw all vertices
		GL11.glDrawElements(GL11.GL_TRIANGLES, model.getIndicesCount(), GL11.GL_UNSIGNED_INT, 0);

		// Deactivate the VAO & VBOs
		GL20.glDisableVertexAttribArray(0);
		GL30.glBindVertexArray(0);
	}

	/**
	 * Create the projection matrix for this rendering
	 */
	private void createProjectionMatrix() {
		float aspectRatio = (float) window.getWidth() / (float) window.getHeight();

		projectionMatrix = new Matrix4f().perspective(
				(float) Math.toRadians(FOV),
				aspectRatio,
				NEAR_PLANE,
				FAR_PLANE
		);
	}
}
