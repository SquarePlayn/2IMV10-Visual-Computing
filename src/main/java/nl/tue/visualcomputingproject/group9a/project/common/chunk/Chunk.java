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
public class Chunk<ID extends ChunkId, T> {
	/** The ID uniquely identifying of a chunk. */
	ID chunkId;
	/** The data of the chunk. */
	T data;

	/**
	 * @return The position of the chunk.
	 */
	public ChunkPosition getPosition() {
		return chunkId.getPosition();
	}

	/**
	 * @return The quality level of the chunk.
	 */
	public QualityLevel getQualityLevel() {
		return chunkId.getQuality();
	}
	
	
}
