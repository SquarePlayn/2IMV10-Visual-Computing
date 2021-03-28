package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator;

import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.buffer_manager.VertexBufferManager;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.mesh.FullMeshGenerator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.*;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing.PreProcessing;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform.GridTransform;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform.ScaleGridTransform;
import org.joml.Vector2f;
import org.joml.Vector3d;

import java.util.Iterator;
import java.util.function.Function;


public class RIMLSGenerator<ID extends ChunkId, T extends PointData>
		extends Generator<ID, T> {
	
	private static final int MAX_ITERATIONS = 10;
	
	// TODO: begin
	static final double sigmaR = 2;
	static final double sigmaN = 2;
	static final double h = 0.5;
	static final Function<Double, Double> phiD2 = (d2) -> {
		double val = 1 - d2 / (h*h);
		val = val * val;
		return val * val;
	};
	static final Function<Double, Double> dPhiD2 = (d2) -> {
		double h2 = h*h;
		double val = 1 - d2 / h2;
		val = val * val * val;
		return -8 * val / h2;
	};
	static final Function<Vector3d, Double> phi = (px) -> {
		double val = 1 - px.lengthSquared() / (h*h);
		val = val * val;
		return val * val;
	};
	static final Function<Vector3d, Vector3d> dPhi = (px) -> {
		double h2 = h*h;
		double val = 1 - px.lengthSquared() / h2;
		val = val * val * val;
		return px.mul(-8*val / h2, new Vector3d());
	};
	// TODO: end
	
	private static double calcDist(QualityLevel quality) {
		switch (quality) {
			case FIVE_BY_FIVE:
				return 10.0;
			case HALF_BY_HALF:
				return 2.0;
			case LAS:
				return 1.0;
			default:
				throw new IllegalStateException();
		}
	}
	
	private static double calcGridDist(QualityLevel quality) {
		switch (quality) {
			case FIVE_BY_FIVE:
				return 5.0;
			case HALF_BY_HALF:
				return 0.5;
			case LAS:
				return 0.25;
			default:
				throw new IllegalStateException();
		}
	}
	
	@Override
	public MeshChunkData generateChunkData(Chunk<ID, ? extends T> chunk) {
		ChunkPosition pos = chunk.getPosition();
		ScaleGridTransform transform = GridTransform.createTransformFor(
				chunk.getQualityLevel(),
				0, 0
		);
		Vector3d offset = new Vector3d(pos.getX(), 0, pos.getY());
		Store<PointNormalIndexData> store = new ArrayStore<>(pos, transform);
		store.addPoints(
				store,
				offset,
				chunk.getData().getVector3D(),
				(vec) -> new PointNormalIndexData(vec, null)
		);
		PreProcessing.fillNullPoints(
				store,
				transform,
				PointNormalIndexData::new
		);
		Store.genWLSNormals(store, transform.getScaleX() * 1.5);
		
		final double scaleDist = store.getTransform().getScaleX();
		final double dist = scaleDist * 1.5;

		for (int zCoord = 0; zCoord < store.getHeight(); zCoord++) {
			for (int xCoord = 0; xCoord < store.getWidth(); xCoord++) {
				// TODO
				double f = 0;
				Vector3d gradF = new Vector3d();

				StoreElement<PointNormalIndexData> elem = store.get(xCoord, zCoord);
				if (elem == null) continue;
				for (PointNormalIndexData pni : elem) {
					Vector3d x = pni.getVec();
					
					for (int iter = 0; iter < MAX_ITERATIONS; iter++) { // TODO
						Iterator<PointNormalIndexData> it = store.neighborIteratorOf(x, xCoord, zCoord, dist);
						if (!it.hasNext()) continue;
						
						double sumW, sumF;
						sumW = sumF = 0;
						Vector3d sumGw = new Vector3d();
						Vector3d sumGF = new Vector3d();
						Vector3d sumN = new Vector3d();
						for (PointNormalIndexData neighbor = it.next(); it.hasNext(); neighbor = it.next()) {
							Vector3d p = neighbor.getVec();
							Vector3d pNormal = neighbor.getNormal();// TODO: Currently WLS-normal
							Vector3d px = x.sub(p, new Vector3d());
							double fx = px.dot(pNormal);
							
							double alpha = 1;
							if (iter > 0) {
								double v1 = (fx - f) / sigmaR;
								double v2 = pNormal.distanceSquared(gradF) / (sigmaN*sigmaN);
								alpha = Math.exp(-v1*v1 - v2);
							}
							
//							double w = alpha * phi.apply(px);
//							Vector3d gradW = dPhi.apply(px).mul(alpha);
							double pxLengthSquared = px.lengthSquared();
							double w = alpha * phiD2.apply(pxLengthSquared);
							Vector3d gradW = px.mul(dPhiD2.apply(pxLengthSquared)).mul(2 * alpha);
							
							sumW += w;
							sumGw.add(gradW);
							sumF += w * fx;
							sumGF.add(gradW.mul(fx, new Vector3d()));
							sumN.add(pNormal.mul(w, new Vector3d()));
						}
						f = sumF / sumW;
						gradF = sumGw.mul(-f, new Vector3d()).add(sumGF).add(sumN).div(sumW);
					}
					x.sub(gradF.mul(f, new Vector3d()));
				}
			}
		}
		
		// Create vertex buffer.
		int count = FullMeshGenerator.preprocess(store);
		VertexBufferManager vertexManager = VertexBufferManager.createManagerFor(
				Settings.VERTEX_TYPE, count // numPoints
		);
		
		Store.addToVertexManager(store, vertexManager);
		
		// Generate mesh and return.
		return new MeshChunkData(
				vertexManager.finalizeBuffer(),
				FullMeshGenerator.generateMesh(store, chunk, false),
				new Vector2f((float) offset.x(), (float) offset.z()));
	}
	
}
