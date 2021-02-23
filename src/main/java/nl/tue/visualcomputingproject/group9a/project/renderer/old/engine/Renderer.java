package nl.tue.visualcomputingproject.group9a.project.renderer.old.engine;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

public class Renderer {

	public void renderMesh(Mesh mesh) {
		// Bind VAO
		GL30.glBindVertexArray(mesh.getVao());
		GL30.glEnableVertexAttribArray(0);

		// Bind indices
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, mesh.getIbo());

		// Draw
		GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.getIndices().length, GL11.GL_FLOAT, 0);

		//  Unbind all
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL30.glDisableVertexAttribArray(0);
		GL30.glBindVertexArray(0);
	}

}
