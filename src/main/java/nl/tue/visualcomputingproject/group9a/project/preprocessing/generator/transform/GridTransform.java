package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.transform;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.Chunk;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;

public interface GridTransform {
	
	double toCoordX(int indexX);
	
	double toCoordZ(int indexZ);
	
	int toGridX(double coordX);
	
	int toGridZ(double coordZ);
	
	static ScaleGridTransform createTransformFor(Chunk<?, ?> chunk, ChunkPosition crop) {
		ChunkPosition pos = chunk.getPosition();
		double rootX = pos.getX() - crop.getX();
		double rootZ = pos.getY() - crop.getY();
		
		switch (chunk.getQualityLevel()) {
			case FIVE_BY_FIVE:
				return new FiveByFiveGridTransform(rootX, rootZ);
			case HALF_BY_HALF:
				return new HalfByHalfGridTransform(rootX, rootZ);
			case LAS:
//				return new ScaleGridTransform(0.25, 0.25, rootX, rootZ);
			default:
				throw new UnsupportedOperationException("LAS not implemented!");
		}
	}
	
}
