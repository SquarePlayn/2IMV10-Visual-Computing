package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.mesh;

import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.Chunk;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkId;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.PointData;
import nl.tue.visualcomputingproject.group9a.project.common.util.Pair;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.buffer_manager.MeshBufferManager;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.PointIndexData;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.StoreElement;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.Store;
import org.joml.Vector3d;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Iterator;

public class FullMeshGenerator {
	
	private static <Data extends PointIndexData> void addSide(
			MeshBufferManager meshManager,
			StoreElement<Data> pi1,
			StoreElement<Data> pi2) {
		if (pi1.size() <= 1 && pi2.size() <= 1) return;
		
		switch (Settings.MESH_TYPE) {
			case TRIANGLES_CLOCKWISE_3_INT:
			case TRIANGLES_COUNTER_CLOCKWISE_3_INT:
				if (pi1.size() >= 2) {
					meshManager.add(
							pi1.back().getIndex(),
							pi2.get(0).getIndex(),
							pi1.get(0).getIndex()
					);
				}
				if (pi2.size() >= 2) {
					meshManager.add(
							pi1.back().getIndex(),
							pi2.back().getIndex(),
							pi2.get(0).getIndex()
					);
				}
				if (pi1.size() >= 2 && pi2.size() >= 2) {
					meshManager.add(
							pi1.back().getIndex(),
							pi2.get(0).getIndex(),
							pi1.get(0).getIndex()
					);
					meshManager.add(
							pi1.back().getIndex(),
							pi2.back().getIndex(),
							pi2.get(0).getIndex()
					);
				}
				break;

			case QUADS_CLOCKWISE_4_INT:
			case QUADS_COUNTER_CLOCKWISE_4_INT:
				if (pi1.size() == 2 && pi2.size() == 2) {
					meshManager.add(
							pi1.back().getIndex(),
							pi1.get(0).getIndex(),
							pi2.get(0).getIndex(),
							pi2.back().getIndex()
					);
				}
				break;
				
			default:
				throw new IllegalStateException();
		}
	}
	
	private static <Data extends PointIndexData> void removeMultiClusterAndSort(StoreElement<Data> pi) {
		if (pi == null || pi.size() <= 1) return;
		pi.sort(Comparator.comparingDouble(p -> p.getVec().y));
		
		if (pi.size() == 2) return;
		Iterator<Data> it = pi.iterator();
		it.next();
		while (it.hasNext() && pi.size() > 2) {
			it.next();
			it.remove();
		}
	}
	
	public static <Data extends PointIndexData> int preprocess(Store<Data> store) {
		int count = 0;
		for (int z = 0; z < store.getHeight(); z++) {
			for (int x = 0; x < store.getWidth(); x++) {
				if (!store.hasPoint(x, z)) continue;
				removeMultiClusterAndSort(store.get(x, z));
				count += store.get(x, z).size();
			}
		}
		return count;
	}
	
	public static <ID extends ChunkId, T extends PointData, Data extends PointIndexData> ByteBuffer generateMesh(
			Store<Data> store,
			Chunk<ID, ? extends T> chunk,
			boolean preprocess) {
		if (preprocess) {
			preprocess(store);
		}
		
		MeshBufferManager meshManager = MeshBufferManager.createManagerFor(
				chunk.getQualityLevel(),
				Settings.MESH_TYPE,
				store.getWidth(), store.getHeight(),
				chunk.getData().size()
		);
		
		for (int z = 0; z < store.getHeight() - 1; z++) {
			for (int x = 0; x < store.getWidth() - 1; x++) {
				StoreElement<Data> pi00 = store.get(x    , z    );
				StoreElement<Data> pi10 = store.get(x + 1, z    );
				StoreElement<Data> pi01 = store.get(x    , z + 1);
				StoreElement<Data> pi11 = store.get(x + 1, z + 1);
				
				addSide(meshManager, pi00, pi10);
				addSide(meshManager, pi10, pi11);
				addSide(meshManager, pi11, pi01);
				addSide(meshManager, pi01, pi00);
				
				switch (Settings.MESH_TYPE) {
					case TRIANGLES_CLOCKWISE_3_INT:
					case TRIANGLES_COUNTER_CLOCKWISE_3_INT:
						meshManager.add(
								pi00.back().getIndex(),
								pi01.back().getIndex(),
								pi11.back().getIndex()
						);
						meshManager.add(
								pi00.back().getIndex(),
								pi11.back().getIndex(),
								pi10.back().getIndex()
						);
						break;
						
					case QUADS_CLOCKWISE_4_INT:
					case QUADS_COUNTER_CLOCKWISE_4_INT:
						meshManager.add(
								pi00.back().getIndex(),
								pi01.back().getIndex(),
								pi11.back().getIndex(),
								pi10.back().getIndex()
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
