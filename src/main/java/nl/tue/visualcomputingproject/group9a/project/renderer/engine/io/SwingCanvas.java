package nl.tue.visualcomputingproject.group9a.project.renderer.engine.io;

import com.google.common.eventbus.EventBus;
import lombok.Getter;
import lombok.Setter;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.renderer.chunk_manager.ChunkManager;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Camera;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Light;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.Loader;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.RawModel;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.Skybox;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.render.Renderer;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.shaders.SkyboxShader;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.shaders.StaticShader;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;

import static nl.tue.visualcomputingproject.group9a.project.common.Settings.*;

public class SwingCanvas extends AWTGLCanvas {
	
	/** The logger object of this class. */
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	private final EventBus eventBus;
	
	private StaticShader shader;
	private SkyboxShader skyboxShader;
	private Camera camera;
	private ChunkManager chunkManager;
	private Light light;
	private Skybox skybox;
	
	@Getter
	@Setter
	private Vector3f backgroundColor = new Vector3f(1.0f, 0.0f, 0.0f);
	
	
	/** Models other than chunks that are to be rendered. */
	private final Collection<RawModel> models = new ArrayList<>();
	
	public SwingCanvas(GLData data, EventBus eventBus) {
		super(data);
		LOGGER.info("Working directory: " + System.getProperty("user.dir"));
		
		this.eventBus = eventBus;
	}
	
	@Override
	public void initGL() {
		LOGGER.info("OpenGL version: " + effective.majorVersion + "." + effective.minorVersion + " (Profile: " + effective.profile + ")");
		GL.createCapabilities();
		
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glCullFace(GL11.GL_BACK);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_LESS);
		
		// Create instances
		chunkManager = new ChunkManager(eventBus);
		shader = new StaticShader();
		skyboxShader = new SkyboxShader();
		camera = new Camera(chunkManager);
		light = new Light(new Vector3f(), LIGHT_COLOR);
		skybox = new Skybox(Settings.SKYBOX_TEXTURE_FILES);
		
		this.addKeyListener(camera);
	}
	
	private void cleanup() {
		camera.cleanup();
		shader.cleanup();
		Loader.cleanup();
	}
	
	private void clearScreen() {
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL42.glClearColor(backgroundColor.x, backgroundColor.y, backgroundColor.z, 1.0f);
		GL42.glClear(GL42.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glViewport(0, 0, getWidth(), getHeight());
	}
	
	@Override
	public void paintGL() {
		// Clear the frame
		clearScreen();
		
		// Update the camera position
		camera.updatePosition();
		
		// Attack the light to the camera
		light.setPosition(camera.getPosition());
		
		// Recalculate the projection matrix if needed
		shader.start();
		shader.loadProjectionMatrix(this, camera);
		shader.stop();
		skyboxShader.start();
		skyboxShader.loadProjectionMatrix(this, camera);
		skyboxShader.stop();
		
		// Rendering of models
		shader.start();
		shader.loadLight(light);
		shader.loadTime((float) (System.nanoTime() * 1000_000_000.0));
		for (RawModel model : models) {
			Renderer.renderModel(model, shader, camera);
		}
		for (RawModel chunk : chunkManager.getModels()) {
			Renderer.renderModel(chunk, shader, camera);
		}
		shader.stop();
		
		// Rendering of the skybox
		skyboxShader.start();
		Renderer.renderSkybox(skybox, skyboxShader, camera);
		skyboxShader.stop();
		
		// Update chunks
		chunkManager.update(camera);
		
		this.swapBuffers();
		this.repaint();
	}
	
	/**
	 * Calculate the projection matrix for this window
	 */
	public Matrix4f getProjectionMatrix(Camera camera) {
		return new Matrix4f().perspective(
			(float) Math.toRadians(camera.getFov()),
			(float) getWidth() / (float) getHeight(),
			NEAR_PLANE,
			FAR_PLANE
		);
	}
}
