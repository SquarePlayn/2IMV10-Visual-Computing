package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The quality of the point cloud data a chunk was sourced from.
 */
@Getter
@AllArgsConstructor
public enum QualityLevel {
	FIVEBYFIVE(true),
	HALFBYHALF(true),
	LAS(false);
	
	final private boolean interpolated;
	
}
