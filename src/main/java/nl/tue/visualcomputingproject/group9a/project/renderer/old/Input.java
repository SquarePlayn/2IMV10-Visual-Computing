package nl.tue.visualcomputingproject.group9a.project.renderer.old;

import lombok.Getter;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;

@Getter
public class Input {
	private final static boolean[] keys = new boolean[GLFW.GLFW_KEY_LAST];
	private final static boolean[] mouseButtons = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST];
	private static double mouseX, mouseY;

	private GLFWKeyCallback keyboardCallback;
	private GLFWMouseButtonCallback mouseButtonCallback;
	private GLFWCursorPosCallback cursorPosCallback;

	public Input() {
		keyboardCallback = new GLFWKeyCallback() {
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				if (action == GLFW.GLFW_PRESS) {
					keys[key] = true;
				} else if (action == GLFW.GLFW_RELEASE) {
					keys[key] = false;
				}
			}
		};

		mouseButtonCallback = new GLFWMouseButtonCallback() {
			@Override
			public void invoke(long window, int button, int action, int mods) {
				if (action == GLFW.GLFW_PRESS) {
					mouseButtons[button] = true;
				} else if (action == GLFW.GLFW_RELEASE) {
					mouseButtons[button] = false;
				}
			}
		};

		cursorPosCallback = new GLFWCursorPosCallback() {
			@Override
			public void invoke(long window, double xpos, double ypos) {
				mouseX = xpos;
				mouseY = ypos;
			}
		};
	}

	public void destroy() {
		keyboardCallback.free();
		mouseButtonCallback.free();
		cursorPosCallback.free();
	}

	public static boolean isKeyDown(int key) {
		return keys[key];
	}

	public static boolean isButtonDown(int button) {
		return mouseButtons[button];
	}

	public static double getMouseX() {
		return mouseX;
	}

	public static double getMouseY() {
		return mouseY;
	}

}
