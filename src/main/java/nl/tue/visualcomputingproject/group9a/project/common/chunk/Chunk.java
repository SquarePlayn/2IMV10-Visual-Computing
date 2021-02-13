package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.AllArgsConstructor;
import lombok.Data;
import nl.tue.visualcomputingproject.group9a.project.common.Point;

/**
 * A single chunk from the map.
 *
 * @param <T> The type of data.
 */
@Data
@AllArgsConstructor
public class Chunk<T> {
	ChunkPosition position;
	QualityLevel qualityLevel;
	T data;
}
