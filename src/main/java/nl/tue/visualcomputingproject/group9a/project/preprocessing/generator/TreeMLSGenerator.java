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
public class TreeMLSGenerator<ID extends ChunkId, T extends PointData>
		extends Generator<ID, T> {
	
	@Override
	public MeshChunkData generateChunkData(Chunk<ID, ? extends T> chunk, ChunkPosition crop) {
		ChunkPosition pos = chunk.getPosition();
		ScaleGridTransform transform = GridTransform.createTransformFor(chunk, crop);
		Vector3d offset = new Vector3d(crop.getX(), 0, crop.getY());
		crop = refineCrop(crop, transform);
		Store<PointIndexData> store = new ArrayStore<>(pos, transform);
		store.addPoints(
				offset,
				chunk.getData().getVector3D(),
				PointIndexData::new
		);

		FullMeshGenerator.preprocess(store);
		PreProcessing.fillNullPoints(store, PointIndexData::new);
		
		if (chunk.getQualityLevel().getOrder() >= QualityLevel.HALF_BY_HALF.getOrder()) {
			Store<PointIndexData> newStore = new ArrayStore<>(pos, transform);
			PreProcessing.treeSmoothing2(store, newStore, PointIndexData::new);
			store = newStore;
		}
		
		// Create vertex buffer.
		VertexBufferManager vertexManager = VertexBufferManager.createManagerFor(
				Settings.VERTEX_TYPE, store.countCropped(crop)
		);
		store.addToVertexManagerGenWLSNormals(vertexManager, crop);

		return new MeshChunkData(
				vertexManager.finalizeBuffer(),
				FullMeshGenerator.generateMesh(store, chunk, crop, false),
				new Vector2f((float) offset.x(), (float) offset.z()));
	}
	
}
