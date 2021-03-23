package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator;

import lombok.RequiredArgsConstructor;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;
import nl.tue.visualcomputingproject.group9a.project.common.util.GeneratorIterator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.buffer_manager.VertexBufferManager;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.mesh.FullMeshGenerator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store.*;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform.GridTransform;
import org.joml.Vector2f;
import org.joml.Vector3d;

import java.util.Iterator;
import java.util.function.Function;


public class RIMLSGenerator<ID extends ChunkId, T extends PointData>
		extends Generator<ID, T> {
	
	@RequiredArgsConstructor
	private static class NeighborIterator<Data extends PointIndexData>
			extends GeneratorIterator<Data> {
		private final Vector3d center;
		private final Store<Data> store;
		private final double distSquare;
		
		private final int maxX;
		private final int minZ;
		private final int maxZ;
		
		private int curX;
		private int curZ;
		private Iterator<Data> dataIt = null;
		
		public NeighborIterator(
				Vector3d center,
				Store<Data> store,
				int x, int z,
				double dist, double gridDist) {
			this.center = center;
			this.store = store;
			this.distSquare = dist * dist;
			int delta = (int) Math.ceil(dist / gridDist);
			maxX = x + delta;
			minZ = z - delta;
			maxZ = z + delta;
			curX = x - delta;
			curZ = minZ;
		}
		
		@Override
		protected Data generateNext() {
			for (; curX <= maxX; curX++) {
				for (; curZ <= maxZ; curZ++) {
					while (dataIt != null && dataIt.hasNext()) {
						Data data = dataIt.next();
						if (data.getVec().distanceSquared(center) <= distSquare) {
							return data;
						}
					}
					dataIt = null;
					
					StoreElement<Data> curPoint = store.get(curX, curZ);
					if (curPoint == null) continue;
					dataIt = curPoint.iterator();
				}
				curZ = minZ;
			}
			
			done();
			return null;
		}
	}
	
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
		GridTransform transform = GridTransform.createTransformFor(
				chunk.getQualityLevel(),
				0, 0
		);
		Vector3d offset = new Vector3d(pos.getX(), 0, pos.getY());
		Store<PointNormalIndexData> store = Store.generateFrom(
				pos,
				transform,
				offset,
				chunk.getData().getVector3D(),
				PointNormalIndexData::new
		);

		Store<PointNormalIndexData> newStore = new ArrayStore<>(
				transform.toGridX(pos.getWidth()) + 1,
				transform.toGridZ(pos.getHeight()) + 1
		);
		
		final double dist = calcDist(chunk.getQualityLevel());
		final double gridDist = calcGridDist(chunk.getQualityLevel());
		
		// TODO: begin
		final double sigmaR = 2;
		final double sigmaN = 2;
		final double h = 0.5;
//		final BiFunction<Vector3d, Vector3d, Double> phi = (x, xi) -> {
//			double val = 1 - x.distanceSquared(xi) / (h*h);
//			val *= val;
//			return val * val;// val ^ 4
//		};
		final Function<Double, Double> phi = (val) -> {
			return 0.0;
		};
		final Function<Double, Vector3d> dPhi = (val) -> {
			return new Vector3d();
		};
		// TODO: end

		for (int zCoord = 0; zCoord < store.getHeight(); zCoord++) {
			for (int xCoord = 0; xCoord < store.getWidth(); xCoord++) {
				// TODO
				double f = 0;
				Vector3d gradF = new Vector3d();
				
				for (int iter = 0; iter < 2; iter++) { // TODO
					double sumW, sumF, sumN;
					sumW = sumF = sumN = 0;
					Vector3d sumGw = new Vector3d();
					Vector3d sumGF = new Vector3d();
					StoreElement<PointNormalIndexData> elem = store.get(xCoord, zCoord);
					if (elem == null) continue;
					for (PointNormalIndexData pni : elem) {
						Vector3d x = pni.getVec();
						Iterator<PointNormalIndexData> it = new NeighborIterator<>(
								x,
								store,
								xCoord, zCoord,
								dist, gridDist
						);
						if (!it.hasNext()) continue;
						for (PointNormalIndexData neighbor = it.next(); it.hasNext(); neighbor = it.next()) {
							Vector3d p = neighbor.getVec();
							Vector3d px = x.sub(p, new Vector3d());
							Vector3d pNormal = neighbor.getNormal();// Currently 0-normal
							double fx = px.dot(pNormal);
							
							double alpha = 1;
							if (iter > 0) {
								double exp1 = (fx - f) / sigmaR;
								double exp2 = pNormal.distanceSquared(gradF) / (sigmaN*sigmaN);
								alpha = Math.exp(-exp1*exp1 - exp2);
							}
							double pxLengthSquared = px.lengthSquared();
							double w = alpha * phi.apply(pxLengthSquared); // TODO
							Vector3d gradW = px.mul(dPhi.apply(pxLengthSquared)).mul(2 * alpha);
							
							sumW += w;
							sumGw.add(gradW);
							sumF += w * fx;
							sumGF.add(gradW.mul(fx));
//							sumN += w * pNormal;
						}
						f = sumF / sumW;
//						gradF = (sumGF - f * sumGw + sumN) / sumW;
					}
				}
			}
		}
		
		
		

		// Create vertex buffer.
		VertexBufferManager vertexManager = VertexBufferManager.createManagerFor(
				Settings.VERTEX_TYPE, chunk.getData().size()
		);
		
		
		
		
		
		
		
		
		
		
		
		
		return new MeshChunkData(
				vertexManager.finalizeBuffer(),
				FullMeshGenerator.generateMesh(store, chunk, false),
				new Vector2f((float) offset.x(), (float) offset.z()));
	}
	
}
