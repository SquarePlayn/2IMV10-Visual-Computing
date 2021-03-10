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
//				LOGGER.warn("The point " + point.asVec3d() + " clashes with " + points[x][z] + ". Ignoring the former.");
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
						normal.add(upProjection(store.getPoint(x, z), store.getPoint(x2, z2)));
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
		
//		// Create mesh buffer.
//		MeshBufferManager meshManager = MeshBufferManager.createManagerFor(
//				chunk.getQualityLevel(),
//				Settings.MESH_TYPE,
//				store.getWidth(), store.getHeight(),
//				chunk.getData().size());
//
//		for (int z = 0; z < store.getHeight() - 1; z++) {
//			// v_(dx,dz)
//			int v00 = 0;
//			boolean doBreak = false;
//			while (!store.hasPoint(v00, z)) {
//				if (++v00 >= store.getWidth()) {
//					doBreak = true;
//					break;
//				}
//				v00++;
//			}
//			if (doBreak) continue;
//			int v10 = v00;
//
//			int v01 = 0;
//			while (!store.hasPoint(v01, z+1)) {
//				if (++v01 >= store.getWidth()) {
//					doBreak = true;
//					break;
//				}
//			}
//			if (doBreak) continue;
//			int v11 = v01;
//
//			while (true) {
//				if (v10 < v11) {
//					while (++v10 < store.getWidth() && !store.hasPoint(v10, z));
//					if (v10 >= store.getWidth()) {
//						if (v11 >= store.getWidth()) {
//							break;
//						} else {
//							continue;
//						}
//					}
//					meshManager.add(
//							store.getIndex(v00, z  ),
//							store.getIndex(v11, z+1),
//							store.getIndex(v10, z  )
//					);
//					v00 = v10;
//					
//				} else {
//					while (++v11 < store.getWidth() && !store.hasPoint(v11, z+1));
//					if (v11 >= store.getWidth()) {
//						if (v10 >= store.getWidth()) {
//							break;
//						} else {
//							continue;
//						}
//					}
//					meshManager.add(
//							store.getIndex(v00, z  ),
//							store.getIndex(v01, z+1),
//							store.getIndex(v11, z+1)
//					);
//					v01 = v11;
//				}
//			}
//		}
//		return new MeshChunkData(
//				vertexManager.finalizeBuffer(),
//				meshManager.finalizeBuffer());
		
		return new MeshChunkData(
				vertexManager.finalizeBuffer(),
				FullMeshGenerator.generateMesh(store, chunk));
	}
		
//	private static void finishMesh(
//			MeshBufferManager manager,
//			PointIndex[][] points,
//			int width,
//			int fixedX,
//			int fixedZ,
//			int curX,
//			int prevX,
//			int curZ,
//			boolean reverse) {
//		do {
//			switch (Settings.MESH_TYPE) {
//				case TRIANGLES_CLOCKWISE_3_INT:
//				case TRIANGLES_COUNTER_CLOCKWISE_3_INT:
//					if (reverse) {
//						manager.add(
//								points[fixedX][fixedZ].getIndex(),
//								points[prevX][curZ].getIndex(),
//								points[curX][curZ].getIndex()
//						);
//					} else {
//						manager.add(
//								points[prevX][curZ].getIndex(),
//								points[fixedX][fixedZ].getIndex(),
//								points[curX][curZ].getIndex()
//						);
//					}
//					break;
//				case QUADS_CLOCKWISE_4_INT:
//				case QUADS_COUNTER_CLOCKWISE_4_INT:
//					throw new UnsupportedOperationException();
//			}
//			
//			// Find next binding.
//			prevX = curX;
//			while (++curX < width && points[curX][curZ] == null);
//		} while (curX < width);
//	}

	/**
	 * TODO: to be removed.
	 * @param args
	 */
	public static void main(String[] args) {
		PointCloudChunkData data = new PointCloudChunkData();
		for (int i = -1; i < 2; i++) {
			for (int j = -1; j < 2; j++) {
				data.addPoint(5*i, 5*j, 5*Math.floorMod(i + j, 2));
			}
		}
		Chunk<ChunkId, PointCloudChunkData> chunk = new Chunk<>(
				new ChunkId(
						new ChunkPosition(-5, -5, 15, 15),
						QualityLevel.FIVE_BY_FIVE),
				data
		);
		Generator
				.createGeneratorFor(chunk.getQualityLevel())
				.generateChunkData(chunk);
	}
	
}
