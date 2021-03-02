package nl.tue.visualcomputingproject.group9a.project.renderer;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.CachePolicy;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Camera;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Light;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.io.Window;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.Loader;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.RawModel;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.render.Renderer;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.shaders.StaticShader;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
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
	final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<>();

	private Window window;
	private StaticShader shader;
	private Renderer renderer;
	private Camera camera;

	// TODO Remove test model
	private RawModel testModel;
	private Light light;

	@Override
	public void startup(EventBus eventBus, CachePolicy diskPolicy, CachePolicy memoryPolicy) {
		LOGGER.info("Rendering starting up!");
		this.start();
		eventBus.register(this);
	}

	@Override
	public void run() {
		// Here is your thread
		System.out.println("Render thread started");
		initialize();
		while (!window.closed()) {
			if (window.shouldUpdate()) {
				runFrame();
			}
			// TODO Sleep or something, don't just keep checking in the while
		}
		cleanup();


		System.out.println("Closing renderer");
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
			testModel = Loader.loadToVAO(
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
		} else {
			float dist = -1f;
			float size = 0.5f;
			testModel = Loader.loadToVAO(
					new float[]{
							-size, size, dist,
							size, size, dist,
							size, -size, dist,
							-size, -size, dist
					}, new int[]{
							0, 1, 2,
							0, 3, 2
					}
			);
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
		renderer.render(testModel, shader, camera);
		shader.stop();

		// TODO Test territory
		camera.getPosition().add(0.0f, 0.00f, 0.0f);
//		camera.setPitch(camera.getPitch() + 0.1f);
		// End of test territory

		// Put the new frame on the screen
		window.swapBuffers();
	}

	private void cleanup() {
		camera.cleanup();
		shader.cleanup();
		window.stop();
	}

	@Subscribe
	public void someEvent(String event) {
		messages.add(event);
	}
}
