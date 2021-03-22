package nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities;

import lombok.Getter;
import lombok.Setter;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.io.Window;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;

import java.util.Collection;
import java.util.HashSet;

import static nl.tue.visualcomputingproject.group9a.project.common.Settings.LOOK_SPEED;
import static nl.tue.visualcomputingproject.group9a.project.common.Settings.MOVE_SPEED;
import static org.lwjgl.glfw.GLFW.*;

@Getter
@Setter
public class Camera {

	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final Vector3f FORWARD = new Vector3f(0, 0, -1);

	/**
	 * The window the camera should render on
	 * TODO Use window to get keys and move the camera
	 */
	private final Window window;

	Vector3f position = Settings.INITIAL_POSITION;

	// Rotational variables
	private float pitch = 0;
	private float yaw = 0;
	private float roll = 0;

	// Other settings
	private boolean wireframe = false;
	private boolean lockHeight = true;

	private GLFWKeyCallback keyboardCallback;

	private Collection<Integer> pressedKeys = new HashSet<>();

	public Camera(Window window) {
		this.window = window;

		registerInputCallbacks();
	}

	/**
	 * Register all callbacks for input w.r.t. camera movement
	 */
	private void registerInputCallbacks() {
		keyboardCallback = new GLFWKeyCallback() {
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				if (action == GLFW_PRESS) {
					pressedKeys.add(key);

					if (key == GLFW_KEY_T) {
						wireframe = !wireframe;
					} else if (key == GLFW_KEY_R) {
						lockHeight = !lockHeight;
					}
				} else if (action == GLFW_RELEASE) {
					pressedKeys.remove(key);
				}
			}
		};
		GLFW.glfwSetKeyCallback(window.getWindow(), keyboardCallback);
	}

	/**
	 * Update the camera position, to be called once per frame
	 */
	public void updatePosition() {
		if (pressedKeys.contains(GLFW_KEY_W)) moveForward(MOVE_SPEED);
		if (pressedKeys.contains(GLFW_KEY_S)) moveForward(-MOVE_SPEED);
		if (pressedKeys.contains(GLFW_KEY_A)) moveSideways(-MOVE_SPEED);
		if (pressedKeys.contains(GLFW_KEY_D)) moveSideways(MOVE_SPEED);
		if (pressedKeys.contains(GLFW_KEY_Q)) moveUp(-MOVE_SPEED);
		if (pressedKeys.contains(GLFW_KEY_E)) moveUp(MOVE_SPEED);
		if (pressedKeys.contains(GLFW_KEY_UP)) increasePitch(LOOK_SPEED);
		if (pressedKeys.contains(GLFW_KEY_DOWN)) increasePitch(-LOOK_SPEED);
		if (pressedKeys.contains(GLFW_KEY_LEFT)) increaseYaw(-LOOK_SPEED);
		if (pressedKeys.contains(GLFW_KEY_RIGHT)) increaseYaw(LOOK_SPEED);
	}

	/**
	 * Get the direction of length 1 the camera is looking in.
	 *
	 * @return Direction the camera is looking in
	 */
	public Vector3f getForward() {
		float fPitch = (float) Math.toRadians(pitch);
		float fYaw = (float) Math.toRadians(yaw);
		float fRoll = (float) Math.toRadians(roll);


		Vector3f forward = new Vector3f(FORWARD);
		forward.rotateZ(fRoll);
		forward.rotateX(fPitch);
		forward.rotateY(-fYaw);

		return forward;
	}

	/**
	 * Get the direction going right from the camera
	 * @return Right direction of the camera
	 */
	public Vector3f getRight() {
		return getForward().cross(UP).normalize();
	}

	/**
	 * Get the upwards direction of the camera.
	 *
	 * @return Upwards direction of the camera
	 */
	public Vector3f getUp() {
		// NB: Up always straight up disregarding camera rotations
		return getForward().cross(getRight()).normalize();
	}


	/**
	 * Move the camera in the viewed direction
	 *
	 * @param dist Distance to move the camera
	 */
	public void moveForward(float dist) {
		Vector3f forward = getForward();
		if (lockHeight) {
			forward.y = 0;
			forward = forward.normalize();
		}
		position.add(forward.mul(dist));
	}

	/**
	 * Move the camera sideways to the right of the currently viewed direction
	 *
	 * @param dist Distance to move the camera
	 */
	public void moveSideways(float dist) {
		Vector3f sideways = getRight();
		if (lockHeight) {
			sideways.y = 0;
			sideways = sideways.normalize();
		}
		position.add(sideways.mul(dist));
	}

	/**
	 * Move the camera in the upwards direction (not influenced by where the camera is looking)
	 *
	 * @param dist Distance to move the camera
	 */
	public void moveUp(float dist) {
		// NB: Always straight up
		position.add(new Vector3f(UP).mul(dist));
	}

	/**
	 * Increase the pitch. Takes care of not being able to move over the bounds.
	 *
	 * @param degrees Angle in degrees to increase the pitch with
	 */
	public void increasePitch(float degrees) {
		pitch = Math.max(-90, Math.min(90, pitch + degrees));
	}

	/**
	 * Increase the yaw. Wraps around.
	 *
	 * @param degrees Angle in degrees to increase the yaw with
	 */
	public void increaseYaw(float degrees) {
		yaw = (yaw + degrees) % 360;
	}

	/**
	 * Increase the roll. Wraps around.
	 *
	 * @param degrees Angle in degrees to increase the roll with
	 */
	public void increaseRoll(float degrees) {
		roll = (roll + degrees) % 360;
	}

	/**
	 * Prepare the camera for deletion
	 */
	public void cleanup() {
		keyboardCallback.free();
		// TODO Listen to mouse events, move if moved
	}
}
