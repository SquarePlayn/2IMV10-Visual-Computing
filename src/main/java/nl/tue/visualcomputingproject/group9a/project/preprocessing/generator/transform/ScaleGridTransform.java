package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ScaleGridTransform
		implements GridTransform {
	private final double scaleX;
	private final double scaleY;
	private final double rootX;
	private final double rootY;

	@Override
	public double toCoordX(int indexX) {
		return rootX + scaleX*indexX;
	}

	@Override
	public double toCoordZ(int indexZ) {
		return rootY + scaleY*indexZ;
	}

	@Override
	public int toGridX(double coordX) {
		return (int) ((coordX - rootX) / scaleX);
	}

	@Override
	public int toGridZ(double coordZ) {
		return (int) ((coordZ - rootY) / scaleY);
	}
	
}
