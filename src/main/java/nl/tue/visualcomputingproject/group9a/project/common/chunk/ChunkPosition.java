package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.Value;

/**
 * Defines a bounding box of points that are contained within a chunk.
 *
 * The point is the lower left corner of the rectangle.
 */
@Value
public class ChunkPosition {
	double lat, lon, width, height;
}
