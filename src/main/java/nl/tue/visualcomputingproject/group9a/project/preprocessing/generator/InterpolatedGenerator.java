package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import nl.tue.visualcomputingproject.group9a.project.common.Point;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.buffer_manager.MeshBufferManager;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.buffer_manager.VertexBufferManager;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * A trivial {@link Generator} implementation suitable for data interpolated on a grid.
 * 
 * @param <T> The type of point data container.
 */
public class InterpolatedGenerator<T extends PointData>
		extends Generator<T> {
	/** The local distance used to approximate the normals with. */
	private static final int DIST = 1;
	/** The type of vertex buffer to generate. */
	private static final VertexBufferType VERTEX_TYPE = VertexBufferType.INTERLEAVED_VERTEX_3_FLOAT_NORMAL_3_FLOAT;
	/** The type of mesh buffer to generate. */
	private static final MeshBufferType MESH_TYPE = MeshBufferType.TRIANGLES_CLOCKWISE_3_INT;

	/**
	 * Data class storing a point with it's index in the vertex buffer.
	 */
	@Getter
	@RequiredArgsConstructor
	private static class PointIndex {
		final private Vector3d point;
		@Setter
		private int index = -1;
		
		public String toString() {
			return "[" + index + ": " + point + "]";
		}
		
	}

	/**
	 * Computes the relative position of a value in the grid.
	 * 
	 * @param quality The quality level.
	 * @param val     The value.
	 * 
	 * @return The relative position of the value in the grid.
	 */
	private static int getPos(QualityLevel quality, double val) {
		switch (quality) {
			case FIVE_BY_FIVE:
				return ((int) val) / 5;
			case HALF_BY_HALF:
				return (int) (val * 2);
			default:
				throw new IllegalArgumentException("Invalid quality level: " + quality);
		}
	}

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
	public MeshChunkData generateChunkData(Chunk<T> chunk) {
		if (!chunk.getQualityLevel().isInterpolated()) {
			throw new IllegalArgumentException(
					"This generator can only be used for interpolated datasets.");
		}
		
		ChunkPosition pos = chunk.getPosition();
		int width = getPos(chunk.getQualityLevel(), pos.getWidth());
		int height = getPos(chunk.getQualityLevel(), pos.getHeight());
		PointIndex[][] points = new PointIndex[width][height];
		
		// Sort the data.
		for (Point point : chunk.getData().getPointIterator()) {
			int x = getPos(chunk.getQualityLevel(), point.getX() - pos.getY());
			int y = getPos(chunk.getQualityLevel(), point.getY() - pos.getX());
			if (points[x][y] != null) {
				throw new IllegalArgumentException(
						"The point " + point.asVec3d() + " clashes with " + points[x][y]);
			}
			points[x][y] = new PointIndex(point.asVec3d());
		}
		
		// Create vertex buffer.
		VertexBufferManager vertexManager = VertexBufferManager.createManagerFor(
				VERTEX_TYPE, chunk.getData().size());

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				Vector3d normal = new Vector3d();
				for (int dy = -DIST; dy <= DIST; dy++) {
					for (int dx = -DIST; dx <= DIST; dx++) {
						if (dx == 0 && dy == 0) continue;
						int x2 = x + dx;
						int y2 = y + dy;
						if (x2 < 0 || x2 >= width) continue;
						if (y2 < 0 || y2 >= height) continue;
						normal.add(upProjection(points[x][y].getPoint(), points[x2][y2].getPoint()));
					}
				}
				if (normal.x == 0 && normal.y == 0 && normal.z == 0) {
					normal.y = 1;
				} else {
					normal.normalize();
				}
				points[x][y].setIndex(vertexManager.addVertex(points[x][y].getPoint(), normal));
			}
		}
		
		// Create mesh buffer.
		MeshBufferManager meshManager = MeshBufferManager.createManagerFor(
				chunk.getQualityLevel(),
				MESH_TYPE,
				width, height,
				chunk.getData().size());

		for (int y = 0; y < height - 1; y++) {
			for (int x = 0; x < width - 1; x++) {
				switch (MESH_TYPE) {
					case TRIANGLES_CLOCKWISE_3_INT:
					case TRIANGLES_COUNTER_CLOCKWISE_3_INT:
						meshManager.add(
								points[x  ][y  ].getIndex(),
								points[x+1][y  ].getIndex(),
								points[x  ][y+1].getIndex());
						meshManager.add(
								points[x  ][y+1].getIndex(),
								points[x+1][y  ].getIndex(),
								points[x+1][y+1].getIndex());
						break;
						
					case QUADS_CLOCKWISE_4_INT:
					case QUADS_COUNTER_CLOCKWISE_4_INT:
						meshManager.add(
								points[x  ][y  ].getIndex(),
								points[x+1][y  ].getIndex(),
								points[x+1][y+1].getIndex(),
								points[x+1][y  ].getIndex());
						break;
				}
			}
		}
		
		return new MeshChunkData(
				vertexManager.finalizeBuffer(),
				meshManager.finalizeBuffer());
	}

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
		Chunk<PointCloudChunkData> chunk = new Chunk<>(
				new ChunkPosition(-5, -5, 15, 15),
				QualityLevel.FIVE_BY_FIVE,
				data
		);
		Generator
				.<PointCloudChunkData>createGeneratorFor(chunk.getQualityLevel())
				.generateChunkData(chunk);
	}
	
}
