package nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities;

import lombok.Getter;
import lombok.Setter;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.renderer.chunk_manager.ChunkManager;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.io.Window;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import static nl.tue.visualcomputingproject.group9a.project.common.Settings.*;
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

	/**
	 * The chunk manager used to fetch terrain heights
	 */
	private final ChunkManager chunkManager;

	Vector3f position = Settings.INITIAL_POSITION;

	// Rotational variables
	private float pitch = 0;
	private float yaw = 0;
	private float roll = 0;

	// Other settings
	private boolean wireframe = false;
	private boolean lockHeight = true;
	private boolean walking = false;
	private float fov = FOV;

	private GLFWKeyCallback keyboardCallback;

	private Collection<Integer> pressedKeys = new HashSet<>();

	public Camera(Window window, ChunkManager chunkManager) {
		this.window = window;
		this.chunkManager = chunkManager;

		registerInputCallbacks();
	}

	/**
	 * Register all callbacks for input w.r.t. camera movement
	 */
	private void registerInputCallbacks() {
		keyboardCallback = new GLFWKeyCallback() {
			@Override
			public void invoke(long w, int key, int scancode, int action, int mods) {
				if (action == GLFW_PRESS) {
					pressedKeys.add(key);

					if (key == GLFW_KEY_T) {
						wireframe = !wireframe;
					} else if (key == GLFW_KEY_R) {
						lockHeight = !lockHeight;
					} else if (key == GLFW_KEY_F) {
						walking = !walking;
					}


					if (key == GLFW_KEY_TAB) {
						fov = FOV / ZOOM_FACTOR;
						window.setResized(true);
					}
				} else if (action == GLFW_RELEASE) {
					pressedKeys.remove(key);

					if (key == GLFW_KEY_TAB) {
						fov = FOV;
						window.setResized(true);
					}
				}
			}
		};
		GLFW.glfwSetKeyCallback(window.getWindow(), keyboardCallback);
	}

	/**
	 * Update the camera position, to be called once per frame
	 */
	public void updatePosition() {
		if (pressedKeys.contains(GLFW_KEY_W)) moveForward(getMoveSpeed());
		if (pressedKeys.contains(GLFW_KEY_S)) moveForward(-getMoveSpeed());
		if (pressedKeys.contains(GLFW_KEY_A)) moveSideways(-getMoveSpeed());
		if (pressedKeys.contains(GLFW_KEY_D)) moveSideways(getMoveSpeed());
		if (pressedKeys.contains(GLFW_KEY_Q)) moveUp(-getMoveSpeed());
		if (pressedKeys.contains(GLFW_KEY_E)) moveUp(getMoveSpeed());
		if (pressedKeys.contains(GLFW_KEY_UP)) increasePitch(LOOK_SPEED);
		if (pressedKeys.contains(GLFW_KEY_DOWN)) increasePitch(-LOOK_SPEED);
		if (pressedKeys.contains(GLFW_KEY_LEFT)) increaseYaw(-LOOK_SPEED);
		if (pressedKeys.contains(GLFW_KEY_RIGHT)) increaseYaw(LOOK_SPEED);

		if (walking) {
			Optional<Float> terrainHeight = chunkManager.getHeight(position.x, position.z);
			terrainHeight.ifPresent(height -> position.y = height + WALK_HEIGHT);
		}
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
	 *
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
	 * Get the speed the camera should go forwards/sideways at.
	 */
	private float getMoveSpeed() {
		float height = position.y - getTerrainHeight();
		float gmsp = GROUND_MOVE_SPEED_PERCENTAGE;
		return (Math.max(0, height / 200) * (1 - gmsp) + gmsp) * MOVE_SPEED;
	}

	/**
	 * Get the height of the terrain at the current position
	 */
	private float getTerrainHeight() {
		// TODO
		return 40;
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
	 * Increase the pitch. Takes care of not being able to move over the bounds, nor exactly on the bounds.
	 *
	 * @param degrees Angle in degrees to increase the pitch with
	 */
	public void increasePitch(float degrees) {
		pitch = Math.max(-89.99f, Math.min(89.99f, pitch + degrees));
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
