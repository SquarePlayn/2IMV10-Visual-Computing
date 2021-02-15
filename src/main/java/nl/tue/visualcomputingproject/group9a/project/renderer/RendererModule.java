package nl.tue.visualcomputingproject.group9a.project.renderer;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Class for the rendering module.
 */
@SuppressWarnings("UnstableApiUsage")
public class RendererModule
		extends Thread
		implements Module {
	/** The logger object of this class. */
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	/** Message queue from the event queue. */ 
	final ConcurrentLinkedQueue<String> messages = new ConcurrentLinkedQueue<>();
	
	
	@Override
	public void startup(EventBus eventBus) {
		LOGGER.info("Rendering starting up!");
		this.start();
		eventBus.register(this);
	}
	
	@Override
	public void run() {
		// Here is your thread

		// TODO This is WIP and should probably move elsewhere
		System.out.println("Render thread started");
		Window window = new Window(800, 600, "2IMV10 Visual Computing");
		window.create();

		while (!window.closed()) {
			// Pre-loop
			window.update();

			// Main loop
			if (Input.isKeyDown(GLFW.GLFW_KEY_ESCAPE)) {
				break;
			}

			if (Input.isButtonDown(GLFW.GLFW_MOUSE_BUTTON_LEFT)) {
				System.out.println("Mouse pressed, current pos: (" + Input.getMouseX() + ", " + Input.getMouseY() + ")." );
			}

			// Post-loop
			window.swapBuffers();
		}

		window.destroy();

		System.out.println("Closing renderer");
	}
	
	@Subscribe
	public void someEvent(String event) {
		messages.add(event);
	}
}
