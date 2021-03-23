package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator;

import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.buffer_manager.VertexBufferManager;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.mesh.FullMeshGenerator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.PointIndexData;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.Store;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing.PreProcessing;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform.GridTransform;
import org.joml.Vector2f;
import org.joml.Vector3d;

/**
 * A trivial {@link Generator} implementation suitable for data interpolated on a grid.
 * 
 * @param <T> The type of point data container.
 */
public class RawGenerator<ID extends ChunkId, T extends PointData>
		extends Generator<ID, T> {
	/** The local distance used to approximate the normals with. */
	private static final int DIST = 1;


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
				0, 0
		);
		Vector3d offset = new Vector3d(pos.getX(), 0, pos.getY());
		Store<PointIndexData> store = Store.generateFrom(
				pos,
				transform,
				offset,
				chunk.getData().getVector3D(),
				PointIndexData::new
		);

		FullMeshGenerator.preprocess(store);
		PreProcessing.fillNullPoints(store, transform, PointIndexData::new);
		
		// Create vertex buffer.
		VertexBufferManager vertexManager = VertexBufferManager.createManagerFor(
				Settings.VERTEX_TYPE, chunk.getData().size()
		);
		
		for (int z = 0; z < store.getHeight(); z++) {
			for (int x = 0; x < store.getWidth(); x++) {
				if (!store.hasPoint(x, z)) continue;
				for (PointIndexData point : store.get(x, z)) {
					Vector3d normal = new Vector3d();
					for (int dz = -DIST; dz <= DIST; dz++) {
						for (int dx = -DIST; dx <= DIST; dx++) {
							if (dx == 0 && dz == 0) continue;
							int x2 = x + dx;
							int z2 = z + dz;
							if (!store.hasPoint(x2, z2)) continue;
							for (PointIndexData point2 : store.get(x2, z2)) {
								double dist = point2.getVec().distance(point.getVec());
								normal.add(upProjection(point.getVec(), point2.getVec()).mul(dist));
							}
						}
					}
					if (normal.x == 0 && normal.y == 0 && normal.z == 0) {
						normal.y = 1;
					} else {
						normal.normalize();
					}
					point.setIndex(vertexManager.addVertex(point.getVec(), normal));
				}
			}
		}

		return new MeshChunkData(
				vertexManager.finalizeBuffer(),
				FullMeshGenerator.generateMesh(store, chunk, false),
				new Vector2f((float) offset.x(), (float) offset.z()));
	}
	
}
