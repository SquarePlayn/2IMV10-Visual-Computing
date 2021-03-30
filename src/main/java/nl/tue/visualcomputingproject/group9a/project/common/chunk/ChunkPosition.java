package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.Value;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.joml.Vector2d;
import org.joml.Vector3d;
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
	
	public ChunkPosition transformed() {
		return new ChunkPosition(
			x, -y - height,
			width, height
		);
	}
	
	public ChunkPosition transformedAddBorder(double border) {
		return new ChunkPosition(
				x - border      , -y - height - border,
				width + 2*border, height + 2*border
		);
	}
	
	public ChunkPosition addBorder(double border) {
		return new ChunkPosition(
				x - border      , y - border,
				width + 2*border, height + 2*border
		);
	}

	public boolean contains(Vector3d point) {
		return contains(point.x, point.z);
	}
	
	public boolean contains (Vector2d point) {
		return contains(point.x, point.y);
	}
	
	public boolean contains(double x, double z) {
		return (x <= x && x <= x + width && y <= z && z <= y + height);
	}
	
}
