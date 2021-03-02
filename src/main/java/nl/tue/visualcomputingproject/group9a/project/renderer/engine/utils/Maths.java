package nl.tue.visualcomputingproject.group9a.project.renderer.engine.utils;

import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Camera;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Maths {

	/**
	 * Create a transformation matrix, handling transformation, rotation and scale
	 *
	 * @param translation Positional translation
	 * @param rx          Rotation along X-axis in degrees
	 * @param ry          Rotation along Y-axis in degrees
	 * @param rz          Rotation along Z-axis in degrees
	 * @param scale       Scale / size
	 * @return Transformation matrix
	 */
	public static Matrix4f createTransformationMatrix(Vector3f translation, float rx, float ry, float rz, float scale) {
		Matrix4f matrix = new Matrix4f();

		// Start with the identity matrix
		matrix.identity();

		// Translate according to translation
		matrix.translate(translation);

		// Rotate according to rotations
		matrix.rotateX((float) Math.toRadians(rx), matrix);
		matrix.rotateY((float) Math.toRadians(ry), matrix);
		matrix.rotateZ((float) Math.toRadians(rz), matrix);

		// Scale according to scale
		matrix.scale(scale);

		return matrix;
	}

	/**
	 * Create the view matrix corresponding to the state of a camera
	 *
	 * @param camera Camera to create the viewmatrix for
	 * @return Viewmatrix of the {@code camera}
	 */
	public static Matrix4f createViewMatrix(Camera camera) {
		Matrix4f matrix = new Matrix4f();

		// Start with the identity
		matrix.identity();

		// Rotate according to the camera rotations
		matrix.rotateX((float) Math.toRadians(-camera.getPitch()), matrix);
		matrix.rotateY((float) Math.toRadians(camera.getYaw()), matrix);
		matrix.rotateZ((float) Math.toRadians(camera.getRoll()), matrix);

		// Translate the view matrix. NB: Backwards
		matrix.translate(new Vector3f(camera.getPosition()).mul(-1));

		return matrix;
	}
}
