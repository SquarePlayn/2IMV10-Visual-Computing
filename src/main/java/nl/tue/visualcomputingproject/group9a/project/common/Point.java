package nl.tue.visualcomputingproject.group9a.project.common;

import lombok.Value;

/**
 * Defines a point in 3D space from the map sheet data.
 */
@Value
public class Point {
	double lat, lon, alt;
}
