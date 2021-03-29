package nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities;

import lombok.Getter;
import lombok.Setter;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.renderer.chunk_manager.ChunkManager;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.io.Window;
import org.joml.Vector3f;
import org.liquidengine.legui.input.KeyCode;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import static nl.tue.visualcomputingproject.group9a.project.common.Settings.*;
import static org.lwjgl.glfw.GLFW.*;

@Getter
@Setter
public class Camera implements KeyListener {

	/**
	 * The logger object of this class.
	 */
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final Vector3f UP = new Vector3f(0, 1, 0);
	private static final Vector3f FORWARD = new Vector3f(0, 0, -1);
	
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

	public Camera(ChunkManager chunkManager) {
		this.chunkManager = chunkManager;
	}

	/**
	 * Update the camera position, to be called once per frame
	 */
	public void updatePosition() {
		if (pressedKeys.contains(KeyEvent.VK_W)) moveForward(getMoveSpeed());
		if (pressedKeys.contains(KeyEvent.VK_S)) moveForward(-getMoveSpeed());
		if (pressedKeys.contains(KeyEvent.VK_A)) moveSideways(-getMoveSpeed());
		if (pressedKeys.contains(KeyEvent.VK_D)) moveSideways(getMoveSpeed());
		if (pressedKeys.contains(KeyEvent.VK_Q)) moveUp(-getMoveSpeed());
		if (pressedKeys.contains(KeyEvent.VK_E)) moveUp(getMoveSpeed());
		if (pressedKeys.contains(KeyEvent.VK_UP)) increasePitch(LOOK_SPEED);
		if (pressedKeys.contains(KeyEvent.VK_DOWN)) increasePitch(-LOOK_SPEED);
		if (pressedKeys.contains(KeyEvent.VK_LEFT)) increaseYaw(-LOOK_SPEED);
		if (pressedKeys.contains(KeyEvent.VK_RIGHT)) increaseYaw(LOOK_SPEED);

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
	
	@Override
	public void keyTyped(KeyEvent keyEvent) {
	
	}
	
	@Override
	public void keyPressed(KeyEvent keyEvent) {
		int key = keyEvent.getKeyCode();
		pressedKeys.add(key);
		
		if (key == KeyEvent.VK_T) {
			wireframe = !wireframe;
		} else if (key == KeyEvent.VK_R) {
			lockHeight = !lockHeight;
		} else if (key == KeyEvent.VK_F) {
			walking = !walking;
		} else if (key == KeyEvent.VK_P) {
			System.out.println("Camera position: " + position);
		}
		
		
		if (key == KeyEvent.VK_TAB) {
			fov = FOV / ZOOM_FACTOR;
			//window.setResized(true);
		}
	}
	
	@Override
	public void keyReleased(KeyEvent keyEvent) {
		int key = keyEvent.getKeyCode();
		pressedKeys.remove(key);
		
		if (key == KeyEvent.VK_TAB) {
			fov = FOV;
			//window.setResized(true);
		}
	}
}
