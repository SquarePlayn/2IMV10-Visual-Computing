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

		// TODO Test territory
/*
		if (true) {
			Collection<ChunkPosition> newChunks = new ArrayList<>();
			final int DIST = 5;
			for (int x = -DIST; x <= DIST; x++) {
				for (int y = -DIST; y <= DIST; y++) {
					if (Math.abs(x) + Math.abs(y) > DIST) continue;
					ChunkPosition newChunk = new ChunkPosition(
							162000 + Settings.CHUNK_WIDTH * x,  // Old: 150001
							384300 + Settings.CHUNK_HEIGHT * y, // Old: 375001
							Settings.CHUNK_WIDTH,
							Settings.CHUNK_HEIGHT
					);
					newChunks.add(newChunk);
				}
			}
			eventBus.post(new RendererChunkStatusEvent(
					new ArrayList<>(),
					new ArrayList<>(),
					newChunks,
					new ArrayList<>()
			));

		} else {
			// Create a test event containing a simple square of 4 vertices / 6 indices

			float dist = -1f;
			float size = 0.5f;
			Vector3f offset = new Vector3f(10, 10, 10);

			MeshBufferManager meshManager = new MeshIntBufferManager(3, 2, false);
			MeshBufferManager.createManagerFor(
					MeshBufferType.TRIANGLES_CLOCKWISE_3_INT, 2);
			meshManager.add(0, 1, 2);
			meshManager.add(0, 3, 2);

			VertexBufferManager vertexManager = new InterleavedVertexFloatBufferManager(4);
			vertexManager.addVertex(new Vector3f(-size, size, dist).add(offset), new Vector3f());
			vertexManager.addVertex(new Vector3f(size, size, dist).add(offset), new Vector3f()); // Top right has a point
			vertexManager.addVertex(new Vector3f(size, -size, dist).add(offset), new Vector3f());
			vertexManager.addVertex(new Vector3f(-size, -size, dist).add(offset), new Vector3f());

			MeshChunkId meshChunkId = new MeshChunkId(
					new ChunkPosition(1, 1, 10, 10),
					QualityLevel.FIVE_BY_FIVE,
					VertexBufferType.INTERLEAVED_VERTEX_3_FLOAT_NORMAL_3_FLOAT,
					MeshBufferType.TRIANGLES_CLOCKWISE_3_INT
			);
			eventBus.post(new ProcessorChunkLoadedEvent(new Chunk<>(
					meshChunkId, new MeshChunkData(
					vertexManager.finalizeBuffer(),
					meshManager.finalizeBuffer()
			)
			)));
		}
 */
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

		// TODO Remainder of this function is test territory

		window.setBackgroundColor(new Vector3f(1.0f, 0.0f, 0.0f));

		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glCullFace(GL11.GL_BACK);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_LESS);
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
	}

	@Subscribe
	public void receiveEvent(ProcessorChunkLoadedEvent event) {
		LOGGER.info("RECEIVED EVENT =======================");
		eventQueue.add(event);
	}
}
