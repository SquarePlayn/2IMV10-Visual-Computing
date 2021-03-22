package nl.tue.visualcomputingproject.group9a.project.renderer.engine.render;

import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Camera;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.io.Window;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.RawModel;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.shaders.StaticShader;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import static nl.tue.visualcomputingproject.group9a.project.common.Settings.*;

public class Renderer {
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
		GL30.glBindVertexArray(model.getVaoId());
		GL20.glEnableVertexAttribArray(0); // Positions
		GL20.glEnableVertexAttribArray(1); // Normals

		// Load transformation matrix into the shader
		Matrix4f transformationMatrix = model.getModelMatrix();
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
