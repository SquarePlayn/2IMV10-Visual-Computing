package nl.tue.visualcomputingproject.group9a.project.renderer.engine.io;

import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL42;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static nl.tue.visualcomputingproject.group9a.project.common.Settings.*;

public class Window {
	/** The logger of this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Getter
	private long window;

	@Getter
	private int width, height;
	private String title;
	private double fps;

	@Getter
	@Setter
	private Vector3f backgroundColor;

	private double time;
	private double processedTime = 0;

	@Getter
	@Setter
	private boolean resized = false;


	public Window(int width, int height, String title, double fps) {
		this.width = width;
		this.height = height;
		this.title = title;
		this.fps = fps;

		// TODO Add sky-box
		this.backgroundColor = new Vector3f(0.0f, 0.0f, 0.0f);

		create();
	}

	/**
	 * Create the actual window
	 */
	private void create() {
		if (!GLFW.glfwInit()) {
			System.err.println("Error: Could not initialize GLFW");
			System.exit(-1);
		}

		// Set some window settings and create the window
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE); // Make window invisible until it's actually loaded
		GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE); // Resizable
		window = GLFW.glfwCreateWindow(width, height, title, 0, 0); // Last 2 are for fullscreen and multi-monitor

		if (window == 0) {
			System.err.println("Error: Window could not be created");
			System.exit(-1);
		}

		// Tell which window to render on
		GLFW.glfwMakeContextCurrent(window);

		// Give window capabilities to actually have something rendered on it
		GL.createCapabilities();

		// Set some rendering tactics
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glCullFace(GL11.GL_BACK);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_LESS);

		// Create window center on the main screen
		GLFWVidMode videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
		GLFW.glfwSetWindowPos(window, (videoMode.width() - width) / 2, (videoMode.height() - height) / 2);

		// Actually display / activate the window
		GLFW.glfwShowWindow(window);

		// Set the initial time
		time = getTimeSeconds();

		registerCallbacks();
	}

	/**
	 * Creation of any GLFW listeners
	 */
	private void registerCallbacks() {
		GLFW.glfwSetWindowSizeCallback(this.window, new GLFWWindowSizeCallback() {
			@Override
			public void invoke(long window, int w, int h) {
				width = w;
				height = h;

				// Set the entire viewport for rendering
				GL11.glViewport(0, 0, width, height);

				// Mark that the window was resized
				resized = true;
			}
		});
	}

	/**
	 * Clear the previous frame from the screen in the window
	 */
	public void clearScreen() {
		// Clear the screen to the background color
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL42.glClearColor(backgroundColor.x, backgroundColor.y, backgroundColor.z, 1.0f);
		GL42.glClear(GL42.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GLFW.glfwPollEvents();
	}

	/**
	 * Display the next frame
	 */
	public void swapBuffers() {
		GLFW.glfwSwapBuffers(window);
	}

	/**
	 * Hold execution until a new frame should be done.
	 */
	public void waitUntilUpdate() {
		// Check how much time has passed since the last check
		double nextTime = getTimeSeconds();
		double passedTime = nextTime - time;
		time = nextTime;

		// Add this time to the total time waited
		processedTime += passedTime;

		// If waiting for longer than the frame time, subtract the time.
		double timePerFrame = 1.0 / fps;
		if (processedTime < timePerFrame) {
			try {
				Thread.sleep((long) (1000 * (timePerFrame - processedTime)));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// TODO Make frames continuous with frame skipping
		processedTime %= timePerFrame;
	}

	/**
	 * Calculate the projection matrix for this window
	 */
	public Matrix4f getProjectionMatrix() {
		return new Matrix4f().perspective(
				(float) Math.toRadians(FOV),
				(float) width / (float) height,
				NEAR_PLANE,
				FAR_PLANE
		);
	}

	/**
	 * Get the current nano time in seconds.
	 *
	 * @return Current time in seconds
	 */
	private static double getTimeSeconds() {
		return System.nanoTime() / 1_000_000_000.0;
	}

	/**
	 * Check whether the window should be closed, such as when the close button is pressed
	 *
	 * @return True if the window should be closed
	 */
	public boolean closed() {
		return GLFW.glfwWindowShouldClose(window);
	}

	/**
	 * Stop the window by force closing it
	 */
	public void stop() {
		GLFW.glfwTerminate();
	}
}
