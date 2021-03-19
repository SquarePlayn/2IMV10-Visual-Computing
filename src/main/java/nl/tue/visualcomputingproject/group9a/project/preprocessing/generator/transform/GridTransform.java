package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;

public interface GridTransform {
	
	double toCoordX(int indexX);
	
	double toCoordZ(int indexZ);
	
	int toGridX(double coordX);
	
	int toGridZ(double coordZ);
	
	static GridTransform createTransformFor(QualityLevel quality, double rootX, double rootZ) {
		switch (quality) {
			case FIVE_BY_FIVE:
				return new FiveByFiveGridTransform(rootX, rootZ);
			case HALF_BY_HALF:
				return new HalfByHalfGridTransform(rootX, rootZ);
			case LAS:
			default:
				throw new UnsupportedOperationException("LAS not implemented!");
		}
	}
	
}