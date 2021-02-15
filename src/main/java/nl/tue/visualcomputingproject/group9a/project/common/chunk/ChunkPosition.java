package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.Value;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory;

/**
 * Defines a bounding box of points that are contained within a chunk.
 *
 * The point is the lower left corner of the rectangle.
 */
@Value
public class ChunkPosition {
	double x, y, width, height;
	
	public Geometry getJtsGeometry(GeometryFactory factory) {
		Coordinate[] coords = {
			new Coordinate(x, y),
			new Coordinate(x +width, y),
			new Coordinate(x +width, y +height),
			new Coordinate(x, y +height),
			new Coordinate(x, y)
		};
		
		return new LinearRing(CoordinateArraySequenceFactory.instance().create(coords), factory);
	}
}
