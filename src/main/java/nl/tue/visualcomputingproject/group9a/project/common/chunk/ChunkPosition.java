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
	double lon, lat, width, height;
	
	public Geometry getJtsGeometry(GeometryFactory factory) {
		Coordinate[] coords = {
			new Coordinate(lon, lat),
			new Coordinate(lon+width, lat),
			new Coordinate(lon+width, lat+height),
			new Coordinate(lon, lat+height),
			new Coordinate(lon, lat)
		};
		
		return new LinearRing(CoordinateArraySequenceFactory.instance().create(coords), factory);
	}
}
