package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ArrayPointIndexStore
		implements PointIndexStore {
	
	private final PointIndex[][] points;
	private final int width;
	private final int height;
	
	public ArrayPointIndexStore(int width, int height) {
		this.width = width;
		this.height = height;
		points = new PointIndex[width][height];
	}
	
	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public PointIndex get(int x, int y) {
		if (x < 0 || x >= width ||
				y < 0 || y >= height) {
			return null;
		} else {
			return points[x][y];
		}
	}

	@Override
	public void set(int x, int y, PointIndex point) {
		if (x < 0 || x >= width ||
				y < 0 || y >= height) {
			throw new IllegalArgumentException("Tried to set (" + x + ", " + y + ") for store (" +
					width + ", " + height + ")");
		} else {
			points[x][y] = point;
		}
	}

	@Override
	public boolean hasPoint(int x, int y) {
		if (x < 0 || x >= width ||
				y < 0 || y >= height) {
			return false;
		} else {
			return points[x][y] != null;
		}
	}
	
}
