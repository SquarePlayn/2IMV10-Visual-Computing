package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator;

import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.buffer_manager.MeshBufferManager;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.buffer_manager.VertexBufferManager;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.mesh.FullMeshGenerator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.ArrayPointIndexStore;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.PointIndex;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.PointIndexStore;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing.DeleteInvalidPointFilter;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing.PointFilter;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing.PreProcessing;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform.GridTransform;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * A trivial {@link Generator} implementation suitable for data interpolated on a grid.
 * 
 * @param <T> The type of point data container.
 */
public class InterpolatedGenerator<ID extends ChunkId, T extends PointData>
		extends Generator<ID, T> {
	/** The logger of this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	/** The local distance used to approximate the normals with. */
	private static final int DIST = 1;
	/** The filter used to filter out illegal points. */
	private static final PointFilter filter = new DeleteInvalidPointFilter();


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
	private static Vector3d upProjection(final Vector3d source, final Vector3d target) {
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
	
	@Override
	public MeshChunkData generateChunkData(Chunk<ID, ? extends T> chunk) {
		if (!chunk.getQualityLevel().isInterpolated()) {
			throw new IllegalArgumentException(
					"This generator can only be used for interpolated datasets.");
		}

		ChunkPosition pos = chunk.getPosition();
		GridTransform transform = GridTransform.createTransformFor(
				chunk.getQualityLevel(),
				pos.getX(),
				pos.getY());
		PointIndexStore store = new ArrayPointIndexStore(
				transform.toGridX(pos.getX() + pos.getWidth()) + 1,
				transform.toGridZ(pos.getY() + pos.getHeight()) + 1
		);
		
		// Sort the data.
		for (Vector3d point : chunk.getData().getVector3D()) {
			point = filter.filter(point);
			if (point == null) continue;
			int x = transform.toGridX(point.x());
			int z = transform.toGridZ(point.z());
			if (store.hasPoint(x, z)) {
				continue;
			}
			store.set(x, z, new PointIndex(point));
		}

		PreProcessing.fillNullPoints(store, transform);
		
		// Create vertex buffer.
		VertexBufferManager vertexManager = VertexBufferManager.createManagerFor(
				Settings.VERTEX_TYPE, chunk.getData().size());
		
		for (int z = 0; z < store.getHeight(); z++) {
			for (int x = 0; x < store.getWidth(); x++) {
				if (!store.hasPoint(x, z)) continue;
				Vector3d normal = new Vector3d();
				for (int dz = -DIST; dz <= DIST; dz++) {
					for (int dx = -DIST; dx <= DIST; dx++) {
						if (dx == 0 && dz == 0) continue;
						int x2 = x + dx;
						int z2 = z + dz;
						if (!store.hasPoint(x2, z2)) continue;
						double dist = store.getPoint(x2, z2).distance(store.getPoint(x, z));
						normal.add(upProjection(store.getPoint(x, z), store.getPoint(x2, z2)).mul(dist));
					}
				}
				if (normal.x == 0 && normal.y == 0 && normal.z == 0) {
					normal.y = 1;
				} else {
					normal.normalize();
				}
				store.get(x, z).setIndex(vertexManager.addVertex(store.getPoint(x, z), normal));
			}
		}
		
		return new MeshChunkData(
				vertexManager.finalizeBuffer(),
				FullMeshGenerator.generateMesh(store, chunk));
	}
	
}
