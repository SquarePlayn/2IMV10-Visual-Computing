package nl.tue.visualcomputingproject.group9a.project.renderer;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.CachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;
import nl.tue.visualcomputingproject.group9a.project.common.event.ProcessorChunkLoadedEvent;
import nl.tue.visualcomputingproject.group9a.project.common.event.RendererChunkStatusEvent;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.buffer_manager.*;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Camera;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Light;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.io.Window;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.Loader;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.RawModel;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.render.Renderer;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.shaders.StaticShader;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
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

	// TODO Remove test model
	private Light light;
	private final Collection<RawModel> models = new ArrayList<>();

	@Override
	public void startup(EventBus eventBus, CachePolicy diskPolicy, CachePolicy memoryPolicy) {
		LOGGER.info("Rendering starting up!");
		this.eventBus = eventBus;
		eventBus.register(this);
		this.start();

		// TODO Test territory

		if (true) {
			Collection<ChunkPosition> newChunks = new ArrayList<>();
			for (int ix = 0; ix < 2; ix++) {
				ChunkPosition newChunk = new ChunkPosition(
						150001 + 300 * ix, 375001, 300, 300
				);
				newChunks.add(newChunk);
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
	}

	@Override
	public void run() {
		// Here is your thread
		LOGGER.info("Render thread started");
		initialize();
		while (!window.closed()) {
			if (window.shouldUpdate()) {
				runFrame();
			}
			// TODO Sleep or something, don't just keep checking in the while
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


		// TODO Remainder of this function is test territory

		window.setBackgroundColor(new Vector3f(1.0f, 0.0f, 0.0f));

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
					}
			);
			models.add(testModel);
		} else {
			// Create a test square at the start
			float dist = -1f;
			float size = 0.5f;
			RawModel testModel = Loader.loadToVAO(
					new float[]{
							-size, size, dist, // Position
							0, 1, 0, // Normal
							size, size, dist,
							0, 1, 0,
							size, -size, dist,
							0, 1, 0,
							-size, -size, dist,
							0, 1, 0
					}, new int[]{ // Indices
							0, 1, 2,
							0, 3, 2
					}
			);
			models.add(testModel);
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
		shader.loadTime((float) (System.nanoTime() * 1000000000.0));
		for (RawModel model : models) {
			renderer.render(model, shader, camera);
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
		if (!eventQueue.isEmpty()) {
			// TODO Handle events
			ProcessorChunkLoadedEvent event = eventQueue.poll();
			LOGGER.info("Extracted an event =============================================");
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

			// Seems correct
			System.out.println("Received ints/vertices " + indexBuffer.remaining() + "/" + vertexBuffer.remaining());

			// Somehow prints super small values
			for (int i = 0; i < 10; i++) {
				System.out.println(i + " - " + vertexBuffer.get(i));
			}

			// Somehow only prints zeros
			System.out.println("= Indices");
			for (int i = 0; i < 3 * 4 && i < indexBuffer.remaining(); i += 3) {
				System.out.println(indexBuffer.get(i) + " -> " + indexBuffer.get(i + 1) + " -> " + indexBuffer.get(i + 2));
			}

			// Print again just to test interleaved transformation, but that's not applicable in current test
			System.out.println("Read position (" + vertexBuffer.get(0) + ", " + vertexBuffer.get(1) + ", " + vertexBuffer.get(2) + ") with " + vertexBuffer.remaining() + " remaining.");
			for (int i = 0; i < 10; i++) {
				System.out.println(i + " - " + vertexBuffer.get(i));
			}

			// Set the camera to the position of the model
			camera.setPosition(new Vector3f(vertexBuffer.get(0), vertexBuffer.get(1), vertexBuffer.get(2)));

			// Load all to model so it can be rendered
			RawModel model = Loader.loadToVAO(vertexBuffer, indexBuffer, indexBuffer.remaining());
			models.clear(); // TODO Smarter switching than just replacing
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
