package nl.tue.visualcomputingproject.group9a.project.renderer.engine.shaders;

import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Camera;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Light;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.io.Window;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.utils.Maths;
import org.joml.Matrix4f;

public class StaticShader extends ShaderProgram {

	private static final String VERTEX_FILE = "/shaders/vertexShader.vert";
	private static final String FRAGMENT_FILE = "/shaders/fragmentShader.frag";

	private int locationTransformationMatrix;
	private int locationViewMatrix;
	private int locationProjectionMatrix;
	private int locationLightPosition;
	private int locationLightColor;
	private int locationCameraPosition;
	private int locationTime;
	private int locationTextureSampler;
	private int locationTextureAvailable;
	private int locationBorder;

	public StaticShader() {
		super(VERTEX_FILE, FRAGMENT_FILE);
	}

	@Override
	protected void bindAttributes() {
		super.bindAttribute(0, "position");
		super.bindAttribute(1, "normal");
		// TODO Add properties like color and normals
	}

	@Override
	protected void getAllUniformLocations() {
		locationTransformationMatrix = super.getUniformLocation("transformationMatrix");
		locationViewMatrix = super.getUniformLocation("viewMatrix");
		locationProjectionMatrix = super.getUniformLocation("projectionMatrix");
		locationLightPosition = super.getUniformLocation("lightPosition");
		locationLightColor = super.getUniformLocation("lightColor");
		locationCameraPosition = super.getUniformLocation("cameraPosition");
		locationTime = super.getUniformLocation("time");
		locationTextureSampler = super.getUniformLocation("textureSampler");
		locationTextureAvailable = super.getUniformLocation("textureAvailable");
		locationBorder = super.getUniformLocation("border");
	}

	public void loadTransformationMatrix(Matrix4f matrix) {
		super.loadMatrix4f(locationTransformationMatrix, matrix);
	}

	public void loadViewMatrix(Camera camera) {
		super.loadMatrix4f(locationViewMatrix, Maths.createViewMatrix(camera));
		super.loadVector3f(locationCameraPosition, camera.getPosition());
	}

	public void loadProjectionMatrix(Window window, Camera camera) {
		super.loadMatrix4f(locationProjectionMatrix, window.getProjectionMatrix(camera));
	}

	public void loadTime(float time) {
		super.loadFloat(locationTime, time);
	}
	
	public void loadBorder() {
		super.loadFloat(locationBorder, (float) Settings.CHUNK_TILE_BORDER);
	}

	public void loadLight(Light light) {
		super.loadVector3f(locationLightPosition, light.getPosition());
		super.loadVector3f(locationLightColor, light.getColor());
	}

	public void setTextureSampler(int unit) {
		super.loadInt(locationTextureSampler, unit);
	}

	public void setTextureAvailable(boolean avail) {
		super.loadBoolean(locationTextureAvailable, avail);
	}
}
