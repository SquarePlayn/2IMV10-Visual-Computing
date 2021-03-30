package nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities;

import lombok.Getter;
import lombok.Setter;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.renderer.chunk_manager.ChunkManager;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.io.Window;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import static nl.tue.visualcomputingproject.group9a.project.common.Settings.*;
import static org.lwjgl.glfw.GLFW.*;

@Getter
@Setter
public class Camera {

	/**
	 * The logger object of this class.
	 */
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final Vector3f FORWARD = new Vector3f(0, 0, -1);

	/**
	 * The window the camera should render on
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
	private float lastTerrainHeight = 40;

	public Camera(Window window, ChunkManager chunkManager) {
		this.window = window;
		this.chunkManager = chunkManager;

		registerInputCallbacks();
	}
	
	public Camera(Camera copy) {
		window = copy.window;
		chunkManager = copy.chunkManager;
		position = copy.position;
		pitch = copy.pitch;
		yaw = copy.yaw;
		roll = copy.roll;
		wireframe = copy.wireframe;
		lockHeight = copy.lockHeight;
		walking = copy.walking;
		fov = copy.fov;
		keyboardCallback = copy.keyboardCallback;
		pressedKeys = new HashSet<>(copy.pressedKeys);
		lastTerrainHeight = copy.lastTerrainHeight;
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
					} else if (key == GLFW_KEY_P) {
						System.out.println("Camera position: " + position);
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
	 * Move the camera 3-dimensionally
	 * @param translation Distance to move the camera
	 */
	public void addToPosition(Vector3f translation) {
		if (translation.isFinite()) {
			position.add(translation);
		} else {
			LOGGER.info("Prevented adding invalid translation to camera position.");
		}
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
			forward.normalize();
		}
		addToPosition(forward.mul(dist));
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
		addToPosition(sideways.mul(dist));
	}

	/**
	 * Move the camera in the upwards direction (not influenced by where the camera is looking)
	 *
	 * @param dist Distance to move the camera
	 */
	public void moveUp(float dist) {
		// NB: Always straight up
		addToPosition(new Vector3f(UP).mul(dist));
	}

	/**
	 * Get the speed the camera should go forwards/sideways at.
	 */
	private float getMoveSpeed() {
		Optional<Float> curHeight = chunkManager.getHeight(position.x, position.z);
		curHeight.ifPresent(height -> lastTerrainHeight = height);
		float height = position.y - lastTerrainHeight;
		float gmsp = GROUND_MOVE_SPEED_PERCENTAGE;
		return (Math.max(0, height / 200) * (1 - gmsp) + gmsp) * MOVE_SPEED;
	}

	/**
	 * Increase the pitch. Takes care of not being able to move over the bounds, nor exactly on the bounds.
	 *
	 * @param degrees Angle in degrees to increase the pitch with
	 */
	public void increasePitch(float degrees) {
		pitch = Math.max(-89.9f, Math.min(89.9f, pitch + degrees));
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
