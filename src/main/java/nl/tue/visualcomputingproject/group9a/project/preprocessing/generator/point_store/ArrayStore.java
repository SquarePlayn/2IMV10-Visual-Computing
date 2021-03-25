package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;
import nl.tue.visualcomputingproject.group9a.project.common.util.GeneratorIterator;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform.ScaleGridTransform;

import java.util.Iterator;

@Getter
@AllArgsConstructor
public class ArrayStore<Data extends PointIndexData>
		implements Store<Data> {
	
	private final StoreElement<Data>[][] points;
	private final int width;
	private final int height;
	private final ScaleGridTransform transform;
	
//	public ArrayStore(int width, int height) {
//		this.width = width;
//		this.height = height;
//		//noinspection unchecked
//		points = (StoreElement<Data>[][]) new StoreElement[width][height];
//	}
	
	public ArrayStore(ChunkPosition pos, ScaleGridTransform transform) {
		this.transform = transform;
		width = (int) (pos.getWidth() / transform.getScaleX()) + 1;
		height = (int) (pos.getHeight() / transform.getScaleY()) + 1;
		//noinspection unchecked
		points = (StoreElement<Data>[][]) new StoreElement[width][height];
	}

	@Override
	public StoreElement<Data> get(int x, int y) {
		if (x < 0 || x >= width ||
				y < 0 || y >= height) {
			return null;
		} else {
			return points[x][y];
		}
	}

	@Override
	public void set(int x, int y, StoreElement<Data> point) {
		if (x < 0 || x >= width ||
				y < 0 || y >= height) {
			throw new IllegalArgumentException("Tried to set (" + x + ", " + y + ") for store (" +
					width + ", " + height + ")");
		} else {
			points[x][y] = point;
		}
	}

	@Override
	public boolean hasPoint(int x, int z) {
		if (x < 0 || x >= width ||
				z < 0 || z >= height) {
			return false;
		} else {
			return points[x][z] != null;
		}
	}
	
	public Iterator<StoreElement<Data>> iterator() {
		return new GeneratorIterator<StoreElement<Data>>() {
			int x = 0;
			int z = 0;
			
			@Override
			protected StoreElement<Data> generateNext() {
				for (; x < width; x++) {
					for (; z < height; z++) {
						if (points[x][z] != null) {
							return points[x][z++];
						}
					}
					z = 0;
				}
				done();
				return null;
			}
		};
	}
	
}
