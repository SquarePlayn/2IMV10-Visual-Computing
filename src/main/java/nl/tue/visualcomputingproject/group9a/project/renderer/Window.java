package nl.tue.visualcomputingproject.group9a.project.renderer;

import lombok.Setter;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class Window {
	private int width, height;
	private String title;
	private long window;
	private Input input = new Input();

	private boolean isResized = true;

	private boolean isFullscreen = false;

	GLFWWindowSizeCallback sizeCallback;

	@Setter
	private float backgroundR, backgroundG, backgroundB;

	public Window(int width, int height, String title) {
		this.width = width;
		this.height = height;
		this.title = title;
	}

	public void create() {
		if (!GLFW.glfwInit()) {
			System.err.println("Erorr: Couldn't initialize GLFW");
			System.exit(-1);
		}

		long monitor = isFullscreen ? GLFW.glfwGetPrimaryMonitor() : 0;
		window = GLFW.glfwCreateWindow(width, height, title, monitor, 0);

		if (window == 0) {
			System.err.println("Error: Window could not be created");
			System.exit(-1);
		}

		// Select this window as the subject for GL
		GLFW.glfwMakeContextCurrent(window);

		// Make OpenGL drawing functionality available on the window
		GL.createCapabilities();

		// Set FPS
		GLFW.glfwSwapInterval(1);

		// Set callbacks
		createCallbacks();

		GLFW.glfwShowWindow(window);
	}

	private void createCallbacks() {
		sizeCallback = new GLFWWindowSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				Window.this.width = width;
				Window.this.height = height;
				isResized = true;
			}
		};

		GLFW.glfwSetWindowSizeCallback(window, sizeCallback);
		GLFW.glfwSetKeyCallback(window, input.getKeyboardCallback());
		GLFW.glfwSetMouseButtonCallback(window, input.getMouseButtonCallback());
		GLFW.glfwSetCursorPosCallback(window, input.getCursorPosCallback());
	}

	public void destroy() {
		input.destroy();
	}

	public boolean closed() {
		return GLFW.glfwWindowShouldClose(window);
	}

	public void update() {
		if (isResized) {
			// Set the viewport size.
			GL11.glViewport(0, 0, width, height);
			isResized = false;

			if (isFullscreen) {
				GLFW.glfwSetWindowMonitor(window, GLFW.glfwGetPrimaryMonitor(), 0, 0, width, height, GLFW.GLFW_DONT_CARE);
			} else {
				GLFWVidMode videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
				GLFW.glfwSetWindowMonitor(window, 0, (videoMode.width() - width) / 2, (videoMode.height() - height) / 2, width, height, GLFW.GLFW_DONT_CARE);
			}
		}
		GL11.glClearColor(backgroundR, backgroundG, backgroundB, 1.0f);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

		GLFW.glfwPollEvents();
	}

	public void setFullscreen(boolean fullscreen) {
		isResized = isFullscreen != fullscreen;
		isFullscreen = fullscreen;
	}

	public void swapBuffers() {
		GLFW.glfwSwapBuffers(window);
	}

	public void setBackgroundColor(float r, float g, float b) {
		backgroundR = r;
		backgroundG = g;
		backgroundB = b;
	}
}
