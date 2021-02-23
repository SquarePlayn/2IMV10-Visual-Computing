package nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities;

import lombok.Getter;
import lombok.Setter;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.io.Window;
import org.joml.Vector3f;

public class Camera {

	/**
	 * The window the camera should render on
	 * TODO Use window to get keys and move the camera
	 */
	private final Window window;

	@Getter
	@Setter
	Vector3f position = new Vector3f(0, 0, 0);

	// Rotational & zoom variables
	// TODO redo this
	@Getter @Setter
	private float pitch = 90;
	@Getter @Setter
	private float yaw = 0;
	@Getter @Setter
	private float roll = 0;

	public Camera(Window window) {
		this.window = window;
	}

	/**
	 * Move the camera's position and/or rotation based on pressed keys
	 */
	public void move() {
		// TODO Listen to mouse events, move if moved
	}
}
