package nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities;

import lombok.Getter;
import lombok.Setter;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.io.Window;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;

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

	private GLFWKeyCallback keyboardCallback;

	public Camera(Window window) {
		this.window = window;

		registerInputCallbacks();
	}

	/**
	 * Register all callbacks for input w.r.t. camera movement
	 */
	private void registerInputCallbacks() {
		GLFW.glfwSetKeyCallback(window.getWindow(), new GLFWKeyCallback() {
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
					// TODO Move constants elsewhere
					float dist = 10f;
					float degr = 10f;

					switch (key) {
						case GLFW.GLFW_KEY_W:
							moveForward(dist);
							break;
						case GLFW.GLFW_KEY_S:
							moveForward(-dist);
							break;
						case GLFW.GLFW_KEY_D:
							moveSideways(dist);
							break;
						case GLFW.GLFW_KEY_A:
							moveSideways(-dist);
							break;
						case GLFW.GLFW_KEY_E:
							moveUp(dist);
							break;
						case GLFW.GLFW_KEY_Q:
							moveUp(-dist);
							break;
						case GLFW.GLFW_KEY_UP:
							increasePitch(degr);
							break;
						case GLFW.GLFW_KEY_DOWN:
							increasePitch(-degr);
							break;
						case GLFW.GLFW_KEY_RIGHT:
							increaseYaw(degr);
							break;
						case GLFW.GLFW_KEY_LEFT:
							increaseYaw(-degr);
							break;
						case GLFW.GLFW_KEY_RIGHT_BRACKET:
							increaseRoll(degr);
							break;
						case GLFW.GLFW_KEY_LEFT_BRACKET:
							increaseRoll(-degr);
							break;
						case GLFW.GLFW_KEY_T:
							wireframe = !wireframe;
							break;
					}
				}

			}
		});
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
		return getForward().cross(getUp()).normalize();
	}

	/**
	 * Get the upwards direction of the camera.
	 *
	 * @return Upwards direction of the camera
	 */
	public Vector3f getUp() {
		// NB: Up always straight up disregarding camera rotations
		return new Vector3f(UP);
	}


	/**
	 * Move the camera in the viewed direction
	 *
	 * @param dist Distance to move the camera
	 */
	public void moveForward(float dist) {
		position.add(getForward().mul(dist));
	}

	/**
	 * Move the camera sideways to the right of the currently viewed direction
	 *
	 * @param dist Distance to move the camera
	 */
	public void moveSideways(float dist) {
		position.add(getRight().mul(dist));
	}

	/**
	 * Move the camera in the upwards direction (not influenced by where the camera is looking)
	 *
	 * @param dist Distance to move the camera
	 */
	public void moveUp(float dist) {
		position.add(getUp().mul(dist));
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
