package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator;

import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.buffer_manager.VertexBufferManager;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.mesh.FullMeshGenerator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.ArrayStore;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.PointIndexData;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.Store;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing.PreProcessing;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform.GridTransform;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform.ScaleGridTransform;
import org.joml.Vector2f;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * A trivial {@link Generator} implementation suitable for data interpolated on a grid.
 * 
 * @param <T> The type of point data container.
 */
public class RawGenerator<ID extends ChunkId, T extends PointData>
		extends Generator<ID, T> {
	/** The local distance used to approximate the normals with. */
	private static final int DIST = 1;
	
	@Override
	public MeshChunkData generateChunkData(Chunk<ID, ? extends T> chunk) {
		if (!chunk.getQualityLevel().isInterpolated()) {
			throw new IllegalArgumentException(
					"This generator can only be used for interpolated datasets.");
		}
		
		ChunkPosition pos = chunk.getPosition();
		ScaleGridTransform transform = GridTransform.createTransformFor(
				chunk.getQualityLevel(),
				0, 0
		);
		Vector3d offset = new Vector3d(pos.getX(), 0, pos.getY());
		Store<PointIndexData> store = new ArrayStore<>(pos, transform);
		store.addPoints(
				offset,
				chunk.getData().getVector3D(),
				PointIndexData::new
		);

		FullMeshGenerator.preprocess(store);
		int count = PreProcessing.fillNullPoints(store, PointIndexData::new);
		
		if (chunk.getQualityLevel().getOrder() >= QualityLevel.HALF_BY_HALF.getOrder()) {
			Store<PointIndexData> newStore = new ArrayStore<>(pos, transform);
			count = PreProcessing.treeSmoothing(store, newStore, PointIndexData::new);
			store = newStore;
		}
		
		// Create vertex buffer.
		VertexBufferManager vertexManager = VertexBufferManager.createManagerFor(
				Settings.VERTEX_TYPE, count
		);
		
		for (int z = 0; z < store.getHeight(); z++) {
			for (int x = 0; x < store.getWidth(); x++) {
				if (!store.hasPoint(x, z)) continue;
				for (PointIndexData point : store.get(x, z)) {
					List<Vector3d> neighbors = new ArrayList<>();
					for (int dz = -DIST; dz <= DIST; dz++) {
						for (int dx = -DIST; dx <= DIST; dx++) {
							if (dx == 0 && dz == 0) continue;
							int x2 = x + dx;
							int z2 = z + dz;
							if (!store.hasPoint(x2, z2)) continue;
							for (PointIndexData point2 : store.get(x2, z2)) {
								neighbors.add(point2.getVec());
							}
						}
					}
					Vector3d normal = generateWLSNormalFor(point.getVec(), neighbors.iterator());
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
