package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing.RawGenerator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform.ScaleGridTransform;
import org.joml.Vector3d;

import java.util.Iterator;

/**
 * The generator used to pre-process a raw data chunk with point data.
 *
 * @param <T> The type of point data container.
 */
public abstract class Generator<ID extends ChunkId, T extends PointData> {
	/** The height threshold for the points. */
	public static final double HEIGHT_THRESHOLD = 1_000;

	/**
	 * Generates the mesh data chunk.
	 * 
	 * @param chunk The raw data chunk.
	 * 
	 * @return A data chunk containing a vertex buffer and a mesh of the processed data.
	 */
	public abstract MeshChunkData generateChunkData(Chunk<ID, ? extends T> chunk, ChunkPosition crop);

	/**
	 * Creates a generator based on the quality level.
	 * 
	 * @param quality The quality level.
	 *
	 * @param <T> The type of point data container.
	 * 
	 * @return A generator suitable for the given quality.
	 */
	public static <ID extends ChunkId, T extends PointData> Generator<ID, T> createGeneratorFor(QualityLevel quality) {
		return new TreeMLSGenerator<>();
//		return new RIMLSGenerator<>();
//		return new RawGenerator<>();
	}


	/**
	 * Computes the normal of the line {@code target -> source} which
	 * lies as close as possible to the vector {@code (0,1,0)}<sup>T</sup>
	 * (i.e. pointing upwards).
	 *
	 * @param source The source vertex.
	 * @param target The target vertex.
	 *
	 * @return The normal to the line {@code target -> source}
	 *     closest to {@code (0,1,0)}<sup>T</sup>.
	 */
	public static Vector3d upProjection(final Vector3d source, final Vector3d target) {
		// v := (0, 1, 0)^T
		// n := source - target
		// rtn := v - (v.n) / ||n||^2 * n
		double x = source.x - target.x;
		double y = source.y - target.y;
		double z = source.z - target.z;
		double dotOverDist = y / (x*x + y*y + z*z);
		Vector3d rtn = new Vector3d(
				0 - x*dotOverDist,
				1 - y*dotOverDist,
				0 - z*dotOverDist);
		// rtn := rtn / ||rtn||
		// Axiom: x^2 + y^2 + z^2 = y
		rtn.div(Math.sqrt(rtn.y));
		return rtn;
	}
	
	public static Vector3d generateWLSNormalFor(Vector3d point, Iterator<Vector3d> neighbors) {
		if (!neighbors.hasNext()) {
			return new Vector3d(0, 1, 0);
		}
		Vector3d normal = new Vector3d();
		while (neighbors.hasNext()) {
			Vector3d neighbor = neighbors.next();
			double dist = neighbor.distance(point);
			normal.add(upProjection(point, neighbor).mul(dist));
		}
		if (normal.x == 0 && normal.y == 0 && normal.z == 0) {
			normal.y = 1;
		} else {
			normal.normalize();
		}
		return normal;
	}
	
	protected static ChunkPosition refineCrop(
			ChunkPosition crop,
			ScaleGridTransform transform) {
		double dx = transform.getScaleX();
		double dz = transform.getScaleZ();
		return new ChunkPosition(
				-dx, -dz,
				crop.getWidth() + 2*dx, crop.getHeight() + 2*dz
		);
	}
	
}
