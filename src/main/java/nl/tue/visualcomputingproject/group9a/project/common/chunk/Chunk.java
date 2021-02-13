package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * A single chunk from the map.
 *
 * @param <T> The type of data.
 */
@Value
@AllArgsConstructor
public class Chunk<T> {
	ChunkId chunkId;
	T data;
	
	public Chunk(
			ChunkPosition position,
			QualityLevel qualityLevel,
			T data) {
		chunkId = new ChunkId(position, qualityLevel);
		this.data = data;
	}
	
	public ChunkPosition getPosition() {
		return chunkId.getPosition();
	}
	
	public QualityLevel getQualityLevel() {
		return chunkId.getQuality();
	}
	
	
}
