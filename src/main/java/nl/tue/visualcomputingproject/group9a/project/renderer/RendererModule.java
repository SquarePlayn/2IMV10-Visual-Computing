package nl.tue.visualcomputingproject.group9a.project.renderer;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.CachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;
import nl.tue.visualcomputingproject.group9a.project.common.event.ProcessorChunkLoadedEvent;
import nl.tue.visualcomputingproject.group9a.project.renderer.chunk_manager.ChunkManager;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Camera;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Light;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.io.Window;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.Loader;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.RawModel;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.render.Renderer;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.shaders.StaticShader;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import static nl.tue.visualcomputingproject.group9a.project.common.Settings.*;

/**
 * Class for the rendering module.
 */
@SuppressWarnings("UnstableApiUsage")
public class RendererModule extends Thread implements Module {
	/**
	 * The logger object of this class.
	 */
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	/**
	 * Message queue from the event queue.
	 */
	final ConcurrentLinkedQueue<ProcessorChunkLoadedEvent> eventQueue = new ConcurrentLinkedQueue<>();

	private EventBus eventBus;

	private Window window;
	private StaticShader shader;
	private Renderer renderer;
	private Camera camera;
	private ChunkManager chunkManager;
	private Light light;

	/**
	 * Models other than chunks that are to be rendered.
	 */
	private final Collection<RawModel> models = new ArrayList<>();

	@Override
	public void startup(EventBus eventBus, CachePolicy diskPolicy, CachePolicy memoryPolicy) {
		LOGGER.info("Rendering starting up!");
		this.eventBus = eventBus;
		eventBus.register(this);
		this.start();
	}

	@Override
	public void run() {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		// Here is your thread
		LOGGER.info("Render thread started");
		initialize();
		while (!window.closed()) {
			window.waitUntilUpdate();
			runFrame();
		}
		cleanup();

		LOGGER.info("Closing renderer");

		// TODO Fix daemon threads auto shutting down when this thread shuts down
		LOGGER.info("Killing system");
		System.exit(0);
	}

	private void initialize() {
		LOGGER.info("Working directory: " + System.getProperty("user.dir"));

		// Create instances
		window = new Window(INITIAL_WINDOW_SIZE.x, INITIAL_WINDOW_SIZE.y, WINDOW_NAME, FPS);
		shader = new StaticShader();
		renderer = new Renderer(window, shader);
		camera = new Camera(window);
		chunkManager = new ChunkManager(eventBus);
		light = new Light(new Vector3f(), LIGHT_COLOR);

		window.setBackgroundColor(new Vector3f(1.0f, 0.0f, 0.0f));
	}

	private void runFrame() {
		// Clear the frame
		window.clearScreen();

		// Attack the light to the camera
		light.setPosition(camera.getPosition());

		// Rendering of models
		shader.start();
		shader.loadLight(light);
		shader.loadTime((float) (System.nanoTime() * 1000_000_000.0));
		for (RawModel model : models) {
			renderer.render(model, shader, camera);
		}
		for (RawModel chunk : chunkManager.getModels()) {
			renderer.render(chunk, shader, camera);
		}
		shader.stop();

		// Put the new frame on the screen
		window.swapBuffers();

		// Update state
		update();
	}

	/**
	 * Update state
	 */
	private void update() {
		// Update chunks
		chunkManager.update(camera);
	}

	private void cleanup() {
		camera.cleanup();
		shader.cleanup();
		window.stop();
		Loader.cleanup();
	}

	@Subscribe
	public void receiveEvent(ProcessorChunkLoadedEvent event) {
		LOGGER.info("RECEIVED EVENT =======================");
		eventQueue.add(event);
	}
}
