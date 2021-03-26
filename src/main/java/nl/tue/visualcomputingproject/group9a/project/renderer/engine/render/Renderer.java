package nl.tue.visualcomputingproject.group9a.project.renderer.engine.render;

import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Camera;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.Skybox;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.RawModel;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.shaders.SkyboxShader;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.shaders.StaticShader;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class Renderer {
	/**
	 * Render a model to the screen
	 *
	 * @param model  Model to render
	 * @param shader Shader to render the model with
	 * @param camera Camera of the screen currently active
	 */
	public static void renderModel(RawModel model, StaticShader shader, Camera camera) {
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
		
		// Activate the texture
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		if (model.getTexId() >= 0) {
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, model.getTexId());
			shader.setTextureUnit(0);
			shader.setTextureAvailable(true);
		} else {
			shader.setTextureAvailable(false);
		}

		// Draw all vertices
		GL11.glDrawElements(GL11.GL_TRIANGLES, model.getIndicesCount(), GL11.GL_UNSIGNED_INT, 0);

		// Deactivate the VAO & VBOs
		GL20.glDisableVertexAttribArray(0);
		GL30.glBindVertexArray(0);
	}

	/**
	 * Render a skybox to the screen
	 *
	 * @param skybox Skybox to render
	 * @param shader Shader to render the skybox with
	 * @param camera Camera of the screen currently
	 */
	public static void renderSkybox(Skybox skybox, SkyboxShader shader, Camera camera) {
		// Start and setup shader
		shader.loadViewMatrix(camera);

		// Load the model
		GL30.glBindVertexArray(skybox.getModel().getVaoId());
		// Enable the positions attribute
		GL20.glEnableVertexAttribArray(0);
		// Activate the texture
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, skybox.getTexture());

		// Mark the vertices as triangles in order to draw
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, skybox.getModel().getIndicesCount());

		// Unbind everything
		GL20.glDisableVertexAttribArray(0);
		GL30.glBindVertexArray(0);
	}
}
