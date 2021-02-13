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
	/** The ID uniquely identifying of a chunk. */
	ChunkId chunkId;
	/** The data of the chunk. */
	T data;

	/**
	 * Alternative constructor of a chunk.
	 * 
	 * @param position     The position of the chunk. Part of the ID.
	 * @param qualityLevel The quality level of the chunk. Part of the ID.
	 * @param data         The data of the chunk.
	 */
	public Chunk(
			ChunkPosition position,
			QualityLevel qualityLevel,
			T data) {
		chunkId = new ChunkId(position, qualityLevel);
		this.data = data;
	}

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
