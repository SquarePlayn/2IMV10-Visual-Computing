package nl.tue.visualcomputingproject.group9a.project.renderer.engine.shaders;

import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Camera;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.io.Window;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.utils.Maths;
import org.joml.Matrix4f;

public class SkyboxShader extends ShaderProgram {

	private static final String VERTEX_FILE = "/shaders/skyboxVertexShader.vert";
	private static final String FRAGMENT_FILE = "/shaders/skyboxFragmentShader.frag";

	private int locationViewMatrix;
	private int locationProjectionMatrix;

	public SkyboxShader() {
		super(VERTEX_FILE, FRAGMENT_FILE);
	}

	@Override
	protected void getAllUniformLocations() {
		locationViewMatrix = super.getUniformLocation("viewMatrix");
		locationProjectionMatrix = super.getUniformLocation("projectionMatrix");
	}

	@Override
	protected void bindAttributes() {
		super.bindAttribute(0, "position");
	}

	/**
	 * Load the view matrix into the shader.
	 */
	public void loadViewMatrix(Camera camera) {
		Matrix4f matrix = Maths.createViewMatrix(camera);
		matrix.m30(0); // Make sure the skybox does not move, only rotate
		matrix.m31(0);
		matrix.m32(0);
		super.loadMatrix4f(locationViewMatrix, matrix);
	}

	/**
	 * Load the projection matrix into the shader.
	 */
	public void loadProjectionMatrix(Window window) {
		super.loadMatrix4f(locationProjectionMatrix, window.getProjectionMatrix());
	}
}
