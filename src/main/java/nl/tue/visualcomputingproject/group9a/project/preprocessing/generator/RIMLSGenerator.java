package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator;

import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;
import nl.tue.visualcomputingproject.group9a.project.common.util.FunctionIterator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.buffer_manager.VertexBufferManager;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.mesh.FullMeshGenerator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.*;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing.PreProcessing;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform.GridTransform;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform.ScaleGridTransform;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.function.Function;

public class RIMLSGenerator<ID extends ChunkId, T extends PointData>
		extends Generator<ID, T> {
	/** The logger object of this class. */
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	private static final int MAX_ITERATIONS = 10;
	private static final int MAX_DIFF_ITERATIONS = 10;
	
	static final double SIGMA_R = 2;
	static final double SIGMA_N = 2;
	static final double H = 0.25;
	static final Function<Double, Double> phiD2 = (d2) -> {
		double val = 1 - d2 / (H * H);
		val = val * val;
		return val * val;
	};
	static final Function<Double, Double> dPhiD2 = (d2) -> {
		double h2 = H * H;
		double val = 1 - d2 / h2;
		val = val * val * val;
		return -8 * val / h2;
	};
	static final Function<Vector3d, Double> phi = (px) -> {
		double val = 1 - px.lengthSquared() / (H * H);
		val = val * val;
		return val * val;
	};
	static final Function<Vector3d, Vector3d> dPhi = (px) -> {
		double h2 = H * H;
		double val = 1 - px.lengthSquared() / h2;
		val = val * val * val;
		return px.mul(-8*val / h2, new Vector3d());
	};
	
	@Override
	public MeshChunkData generateChunkData(Chunk<ID, ? extends T> chunk, ChunkPosition crop) {
		ChunkPosition pos = chunk.getPosition();
		ScaleGridTransform transform = GridTransform.createTransformFor(
				chunk.getQualityLevel(),
				0, 0
		);
		Vector3d offset = new Vector3d(pos.getX(), 0, pos.getY());
		crop = refineCrop(crop, offset, transform);
		Store<PointNormalIndexData> store = new ArrayStore<>(pos, transform);
		int numLoaded = store.addPoints(
				offset,
				chunk.getData().getVector3D(),
				PointNormalIndexData::new
		);
		LOGGER.info("Loaded " + numLoaded + " points.");
		PreProcessing.fillNullPoints(
				store,
				PointNormalIndexData::new
		);
		
		final double scaleDist = store.getTransform().getScaleX();
		final double dist = scaleDist * 1.5;
		
		if (chunk.getQualityLevel().getOrder() >= QualityLevel.HALF_BY_HALF.getOrder()) {
			Store<PointNormalIndexData> newStore = new ArrayStore<>(pos, transform);
			PreProcessing.treeSmoothing(
					store,
					newStore,
					PointNormalIndexData::new
			);
			store = newStore;
		}

		Store.genWLSNormals(store, dist);

		for (int zCoord = 0; zCoord < store.getHeight(); zCoord++) {
			for (int xCoord = 0; xCoord < store.getWidth(); xCoord++) {
				StoreElement<PointNormalIndexData> elem = store.get(xCoord, zCoord);
				if (elem == null) continue;
				
				for (PointNormalIndexData pni : elem) {
					Vector3d diff;
					int diffIterRem = MAX_DIFF_ITERATIONS;
					do {
						double f = 0;
						Vector3d gradF = new Vector3d();
						Vector3d x = pni.getVec();

						for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
							Iterator<PointNormalIndexData> it = store.neighborIteratorOf(x, xCoord, zCoord, dist);
							if (!it.hasNext()) break;

							double sumW, sumF;
							sumW = sumF = 0;
							Vector3d sumGw = new Vector3d();
							Vector3d sumGF = new Vector3d();
							Vector3d sumN = new Vector3d();
							while (it.hasNext()) {
								PointNormalIndexData neighbor = it.next();
								Vector3d p = neighbor.getVec();
								Vector3d pNormal = neighbor.getNormal();
								Vector3d px = x.sub(p, new Vector3d());
								double fx = px.dot(pNormal);

								double alpha = 1;
								if (iter > 0) {
									double v1 = (fx - f) / SIGMA_R;
									double v2 = pNormal.distanceSquared(gradF) / (SIGMA_N * SIGMA_N);
									alpha = Math.exp(-v1 * v1 - v2);
								}

//								double w = alpha * phi.apply(px);
//								Vector3d gradW = dPhi.apply(px).mul(alpha);
						   		double pxLengthSquared = px.lengthSquared();
								double w = alpha * phiD2.apply(pxLengthSquared);
								Vector3d gradW = px.mul(dPhiD2.apply(pxLengthSquared)).mul(2 * alpha);

								sumW += w;
								sumGw.add(gradW);
								sumF += w * fx;
								sumGF.add(gradW.mul(fx, new Vector3d()));
								sumN.add(pNormal.mul(w, new Vector3d()));
							}
							if (sumW == 0) {
								break;
							}
							f = sumF / sumW;
							gradF = sumGw.mul(-f, new Vector3d()).add(sumGF).add(sumN).div(sumW);
						}
						diff = gradF.mul(f, new Vector3d());
						diff.x = Math.max(-0.1, Math.min(diff.x, 0.1));
						diff.z = Math.max(-0.1, Math.min(diff.z, 0.1));
						x.sub(diff);
						Iterator<Vector3d> neighbors = new FunctionIterator<>(
								new NeighborIterator<>(x, store, xCoord, zCoord, dist, scaleDist, true),
								PointIndexData::getVec
						);
						pni.setNormal(Generator.generateWLSNormalFor(x, neighbors));
					} while (diff.lengthSquared() > 0.1 && --diffIterRem > 0);
				}
			}
		}

		// Recompute normals
		Store.genWLSNormals(store, transform.getScaleX() * 1.5);
		
		// Create vertex buffer.
		FullMeshGenerator.preprocess(store);
		VertexBufferManager vertexManager = VertexBufferManager.createManagerFor(
				Settings.VERTEX_TYPE, store.countCropped(crop)
		);
		
		Store.addToVertexManager(store, crop, vertexManager);
		
		// Generate mesh and return.
		return new MeshChunkData(
				vertexManager.finalizeBuffer(),
				FullMeshGenerator.generateMesh(store, chunk, crop, false),
				new Vector2f((float) offset.x(), (float) offset.z()));
	}
	
}
