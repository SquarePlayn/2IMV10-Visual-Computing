package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The quality of the point cloud data a chunk was sourced from.
 */
@Getter
@AllArgsConstructor
public enum QualityLevel {
	FIVE_BY_FIVE(true),
	HALF_BY_HALF(true),
	LAS(false);
	
	final private boolean interpolated;
	
}
