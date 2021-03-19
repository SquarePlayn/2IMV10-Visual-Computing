package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.mesh;

import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.Chunk;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkId;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.PointData;
import nl.tue.visualcomputingproject.group9a.project.common.util.Pair;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.buffer_manager.MeshBufferManager;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.PointIndex;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.PointIndexStore;
import org.joml.Vector3d;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Iterator;

public class FullMeshGenerator {
	
	private static void addSide(MeshBufferManager meshManager, PointIndex pi1, PointIndex pi2) {
		if (pi1.size() == 2 && pi2.size() == 2) {
			switch (Settings.MESH_TYPE) {
				case TRIANGLES_CLOCKWISE_3_INT:
				case TRIANGLES_COUNTER_CLOCKWISE_3_INT:
					meshManager.add(
							pi1.get(1).getSecond(),
							pi2.get(0).getSecond(),
							pi1.get(0).getSecond()
					);
					meshManager.add(
							pi1.get(1).getSecond(),
							pi2.get(1).getSecond(),
							pi2.get(0).getSecond()
					);
					break;

				case QUADS_CLOCKWISE_4_INT:
				case QUADS_COUNTER_CLOCKWISE_4_INT:
					meshManager.add(
							pi1.get(1).getSecond(),
							pi1.get(0).getSecond(),
							pi2.get(0).getSecond(),
							pi2.get(1).getSecond()
					);
					break;
				default:
					throw new IllegalStateException();
			}
		}
	}
	
	public static <ID extends ChunkId, T extends PointData> ByteBuffer generateMesh(
			PointIndexStore store,
			Chunk<ID, ? extends T> chunk) {
		MeshBufferManager meshManager = MeshBufferManager.createManagerFor(
				chunk.getQualityLevel(),
				Settings.MESH_TYPE,
				store.getWidth(), store.getHeight(),
				chunk.getData().size()
		);
		
		for (int z = 0; z < store.getHeight() - 1; z++) {
			for (int x = 0; x < store.getWidth() - 1; x++) {
				PointIndex pi00 = store.get(x, z);
				if (pi00.size() >= 2) {
					pi00.sort(Comparator.comparingDouble(p -> p.getFirst().y));
					if (pi00.size() > 2) {
						Iterator<Pair<Vector3d, Integer>> it = pi00.iterator();
						it.next();
						while (it.hasNext() && pi00.size() > 2) {
							it.next();
							it.remove();
						}
					}
				}

				PointIndex pi01 = store.get(x    , z + 1);
				PointIndex pi10 = store.get(x + 1, z    );
				PointIndex pi11 = store.get(x + 1, z + 1);
				addSide(meshManager, pi00, pi10);
				addSide(meshManager, pi10, pi11);
				addSide(meshManager, pi11, pi01);
				addSide(meshManager, pi01, pi00);
				
				switch (Settings.MESH_TYPE) {
					case TRIANGLES_CLOCKWISE_3_INT:
					case TRIANGLES_COUNTER_CLOCKWISE_3_INT:
						meshManager.add(
								pi00.back().getSecond(),
								pi01.back().getSecond(),
								pi11.back().getSecond()
						);
						meshManager.add(
								pi00.back().getSecond(),
								pi11.back().getSecond(),
								pi10.back().getSecond()
						);
						break;
						
					case QUADS_CLOCKWISE_4_INT:
					case QUADS_COUNTER_CLOCKWISE_4_INT:
						meshManager.add(
								pi00.back().getSecond(),
								pi01.back().getSecond(),
								pi11.back().getSecond(),
								pi10.back().getSecond()
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
