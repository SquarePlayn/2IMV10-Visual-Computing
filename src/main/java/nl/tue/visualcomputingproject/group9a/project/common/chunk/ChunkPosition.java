package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.Value;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.*;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Defines a bounding box of points that are contained within a chunk.
 *
 * The point is the lower left corner of the rectangle.
 */
@Value
public class ChunkPosition {
	double x, y, width, height;
	
	public ReferencedEnvelope getReferencedEnvelope(CoordinateReferenceSystem crs) {
		return new ReferencedEnvelope(x, x+width, y, y+height, crs);
	}
	
	public Geometry getJtsGeometry(GeometryFactory factory, CoordinateReferenceSystem crs) {
		return factory.toGeometry(getReferencedEnvelope(crs));
	}
}
