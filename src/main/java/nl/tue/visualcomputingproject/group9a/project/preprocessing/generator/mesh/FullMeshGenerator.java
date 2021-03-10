package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.mesh;

import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.Chunk;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkId;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.PointData;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.buffer_manager.MeshBufferManager;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.PointIndexStore;

import java.nio.ByteBuffer;

public class FullMeshGenerator {
	
	public static <ID extends ChunkId, T extends PointData> ByteBuffer generateMesh(
			PointIndexStore points,
			Chunk<ID, ? extends T> chunk) {
		MeshBufferManager meshManager = MeshBufferManager.createManagerFor(
				chunk.getQualityLevel(),
				Settings.MESH_TYPE,
				points.getWidth(), points.getHeight(),
				chunk.getData().size()
		);
		
		for (int z = 0; z < points.getHeight() - 1; z++) {
			for (int x = 0; x < points.getWidth() - 1; x++) {
				switch (Settings.MESH_TYPE) {
					case TRIANGLES_CLOCKWISE_3_INT:
					case TRIANGLES_COUNTER_CLOCKWISE_3_INT:
						meshManager.add(
								points.getIndex(x    , z    ),
								points.getIndex(x    , z + 1),
								points.getIndex(x + 1, z + 1)
						);
						meshManager.add(
								points.getIndex(x    , z    ),
								points.getIndex(x + 1, z + 1),
								points.getIndex(x + 1, z    )
						);
						break;
					case QUADS_CLOCKWISE_4_INT:
					case QUADS_COUNTER_CLOCKWISE_4_INT:
						meshManager.add(
								points.getIndex(x    , z    ),
								points.getIndex(x    , z + 1),
								points.getIndex(x + 1, z + 1),
								points.getIndex(x + 1, z    )
						);
						break;
					default:
						throw new IllegalStateException();
				}
			}
		}
		
		return meshManager.finalizeBuffer();
	}
	
}
