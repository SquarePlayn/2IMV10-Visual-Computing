package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.Value;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Defines a bounding box of points that are contained within a chunk.
 *
 * The point is the lower left corner of the rectangle.
 */
@Value
public class ChunkPosition {
	double x, y, width, height;
	
	public Geometry getJtsGeometry(GeometryFactory factory, CoordinateReferenceSystem crs) {
		
		Envelope env = new ReferencedEnvelope(x, x+width, y, y+height, crs);
		
		return factory.toGeometry(env);
		
		/*Coordinate[] coords = {
			new Coordinate(x, y),
			new Coordinate(x +width, y),
			new Coordinate(x +width, y +height),
			new Coordinate(x, y +height),
			new Coordinate(x, y)
		};
		
		return new LinearRing(CoordinateArraySequenceFactory.instance().create(coords), factory);*/
	}
}
