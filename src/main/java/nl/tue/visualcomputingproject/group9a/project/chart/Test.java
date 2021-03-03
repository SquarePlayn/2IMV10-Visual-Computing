package nl.tue.visualcomputingproject.group9a.project.chart;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.geometry.jts.JTS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class Test {
	/** The logger of this class. */
	static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	public static void main(String[] args) {
		try {
			new Test().run();
			Thread.sleep(1000_000_000_000L);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void run() throws IOException, TransformException {
		File file = new File("R5_51DZ1.TIF");
		
		AbstractGridFormat format = GridFormatFinder.findFormat( file );
		GridCoverage2DReader reader = format.getReader( file );
		GridCoverage2D coverage = (GridCoverage2D) reader.read(null);
		CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem2D();
		logger.info("CRS: {}", crs);
		Envelope env = coverage.getEnvelope();
		logger.info("Envelope: {}", env);
		
		int sampleDims = coverage.getNumSampleDimensions();
		logger.info("Num sample dims: {}", sampleDims);
		
		GridEnvelope gridRange2D = coverage.getGridGeometry().getGridRange();
		GeometryFactory gf = new GeometryFactory();
		int count = 0;
		for (int i = gridRange2D.getLow(0); i < gridRange2D.getHigh(0); i++) {
			for (int j = gridRange2D.getLow(1); j < gridRange2D.getHigh(1); j++) {
				if (count++ > 10) System.exit(0);
				GridCoordinates2D coord = new GridCoordinates2D(i, j);
				DirectPosition p = coverage.getGridGeometry().gridToWorld(coord);
				
				double [] vals = new double[1];
				coverage.evaluate(p, vals);
				Point point = gf.createPoint(new Coordinate(p.getOrdinate(0), p.getOrdinate(1)));
				
				
				logger.info("{},{} -> {} -> {} -> {}", i, j, p, point, vals[0]);
			}
		}
	}
}
