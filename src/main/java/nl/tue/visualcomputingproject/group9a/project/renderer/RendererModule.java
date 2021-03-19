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

	// TODO Remove test model
	private Light light;
	private final Collection<RawModel> models = new ArrayList<>();
	private boolean firstCameraRelocation = true;

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
//			if (window.shouldUpdate()) {
//				runFrame();
//			}
			// TODO Sleep or something, don't just keep checking in the while (done)
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
		window = new Window(1000, 800, "A test window", 30);
		shader = new StaticShader();
		renderer = new Renderer(window, shader);
		camera = new Camera(window);
		chunkManager = new ChunkManager(eventBus);


		// TODO Remainder of this function is test territory

		window.setBackgroundColor(new Vector3f(1.0f, 0.0f, 0.0f));

		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glCullFace(GL11.GL_BACK);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_LESS);
		

		light = new Light(new Vector3f(1, 1, 1), new Vector3f(1, 1, 1));

		if (false) {
			float height = 1f;
			float size = 0.5f;
			RawModel testModel = Loader.loadToVAO(
					new float[]{
							-size * 2, height, size,
							size * 2, height, size,
							size * 2, height, -size,
							-size * 2, height, -size
					}, new int[]{
							0, 1, 2,
							0, 3, 2
					}, new Vector2f(0, 0)
			);
			models.add(testModel);
		} else {
			// Create a test square at the start
			float dist = -1f;
			float size = 0.5f;
			float[][] arrows = new float[][]{
					new float[]{30, 1, 1},
					new float[]{1, 20, 1},
					new float[]{1, 1, 10},
			};
			for (float[] xyz : arrows) {
				RawModel arrow = Loader.loadToVAO(
						// Create box from (0,0,0) to (x,y,z)
						new float[]{
								0, 0, 0, // Position
								0, 1, 0, // Normal

								0, 0, xyz[2],
								0, 1, 0, // Normal

								xyz[0], 0, xyz[2],
								0, 1, 0, // Normal

								xyz[0], 0, 0,
								0, 1, 0, // Normal

								0, xyz[1], 0,
								0, 1, 0, // Normal

								0, xyz[1], xyz[2],
								0, 1, 0, // Normal

								xyz[0], xyz[1], xyz[2],
								0, 1, 0, // Normal

								xyz[0], xyz[1], 0,
								0, 1, 0, // Normal
						}, new int[]{ // Indices
								// Bottom (Y-0)
								0, 2, 1,
								0, 2, 3,
								// Top (Y-1)
								4, 6, 5,
								4, 6, 7,
								// X-0
								0, 5, 4,
								0, 5, 1,
								// X-1
								3, 6, 2,
								3, 6, 7,
								// Z-0
								0, 7, 3,
								0, 7, 4,
								// Z-1
								1, 6, 2,
								1, 6, 5,
						}, new Vector2f(0, 0)
				);
				models.add(arrow);
			}
		}
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

		if (!eventQueue.isEmpty() && false) {
			// TODO Handle events
			ProcessorChunkLoadedEvent event = eventQueue.poll();
			LOGGER.info("Extracted an event: " + event.toString());
			Chunk<MeshChunkId, MeshChunkData> chunk = event.getChunk();
			MeshChunkId chunkId = chunk.getChunkId();
			QualityLevel qualityLevel = chunkId.getQuality();
			MeshBufferType meshType = chunkId.getMeshType();
			VertexBufferType vertexType = chunkId.getVertexType();
			LOGGER.info("It's of quality: " + qualityLevel.toString());
			LOGGER.info("It's of mesh type: " + meshType.toString());
			LOGGER.info("It's of vertex type: " + vertexType.toString());
			MeshChunkData meshChunkData = chunk.getData();
			IntBuffer indexBuffer = meshChunkData.getMeshBuffer();
			FloatBuffer vertexBuffer = meshChunkData.getVertexBuffer();
			Vector2f offset = meshChunkData.getOffset();

			// Set the camera to the position of the model
			// TODO: smarter camera handling
			if (firstCameraRelocation) {
				firstCameraRelocation = false;
				camera.setPosition(new Vector3f(
						vertexBuffer.get(0),
						vertexBuffer.get(1) + 100,
						vertexBuffer.get(2)
				));
			}

			// Load all to model so it can be rendered
			RawModel model = Loader.loadToVAO(vertexBuffer, indexBuffer, offset);
//			models.clear(); // TODO Smarter switching than just replacing
			models.add(model);
		}
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
