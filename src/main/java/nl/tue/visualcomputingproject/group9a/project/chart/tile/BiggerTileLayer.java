package nl.tue.visualcomputingproject.group9a.project.chart.tile;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.DirectLayer;
import org.geotools.map.MapContent;
import org.geotools.map.MapViewport;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.tile.Tile;
import org.geotools.tile.TileService;
import org.geotools.tile.util.TileLayer;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Copy-paste of org.geotools.tile.util.TileLayer but with the max tiles cranked up.
 */
public class BiggerTileLayer extends DirectLayer {
	
	private static final Logger LOGGER = Logging.getLogger(TileLayer.class);
	
	private static final GridCoverageFactory gridFactory = new GridCoverageFactory();
	
	private TileService service;
	
	/** Resolution in DPI */
	private double resolution = 90;
	
	private GridCoverage2D coverage;
	
	public BiggerTileLayer(TileService service) {
		super();
		this.service = service;
	}
	
	public GridCoverage2D getCoverage() {
		return this.coverage;
	}
	
	public ReferencedEnvelope getBounds() {
		return new ReferencedEnvelope(-180, 180, -85, 85, DefaultGeographicCRS.WGS84);
	}
	
	@Override
	public void draw(Graphics2D graphics, MapContent map, MapViewport theViewport) {
		
		LOGGER.fine("Started drawing");
		
		final MapViewport viewport = new MapViewport(theViewport);
		
		final ReferencedEnvelope viewportExtent = viewport.getBounds();
		int scale = calculateScale(viewportExtent, viewport.getScreenArea());
		
		Collection<Tile> tiles = service.findTilesInExtent(viewportExtent, scale, false, 12800);
		
		BufferedImage mosaickedImage = createImage(viewport.getScreenArea());
		Graphics2D g2d = mosaickedImage.createGraphics();
		renderTiles(tiles, g2d, viewportExtent, viewport.getWorldToScreen());
		
		this.coverage = gridFactory.create("GridCoverage", mosaickedImage, viewportExtent);
		
		graphics.drawImage(mosaickedImage, 0, 0, null);
		
		LOGGER.fine("Drawing done");
	}
	
	protected void renderTiles(
		Collection<Tile> tiles,
		Graphics2D g2d,
		ReferencedEnvelope viewportExtent,
		AffineTransform worldToImageTransform) {
		
		g2d.setRenderingHint(
			RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		
		double[] points = new double[4];
		
		for (Tile tile : tiles) {
			ReferencedEnvelope nativeTileEnvelope = tile.getExtent();
			
			ReferencedEnvelope tileEnvViewport;
			try {
				tileEnvViewport =
					nativeTileEnvelope.transform(
						viewportExtent.getCoordinateReferenceSystem(), true);
			} catch (TransformException | FactoryException e) {
				throw new RuntimeException(e);
			}
			
			points[0] = tileEnvViewport.getMinX();
			points[3] = tileEnvViewport.getMinY();
			points[2] = tileEnvViewport.getMaxX();
			points[1] = tileEnvViewport.getMaxY();
			
			worldToImageTransform.transform(points, 0, points, 0, 2);
			
			renderTile(tile, g2d, points);
		}
	}
	
	protected void renderTile(Tile tile, Graphics2D g2d, double[] points) {
		
		BufferedImage img = getTileImage(tile);
		
		g2d.drawImage(
			img,
			(int) points[0],
			(int) points[1],
			(int) Math.ceil(points[2] - points[0]),
			(int) Math.ceil(points[3] - points[1]),
			null);
	}
	
	protected BufferedImage getTileImage(Tile tile) {
		return tile.getBufferedImage();
	}
	
	private int calculateScale(ReferencedEnvelope extent, Rectangle screenArea) {
		
		int scale = 0;
		
		try {
			scale =
				(int)
					Math.round(
						RendererUtilities.calculateScale(
							extent,
							screenArea.width,
							screenArea.height,
							this.resolution));
		} catch (FactoryException | TransformException ex) {
			throw new RuntimeException("Failed to calculate scale", ex);
		}
		return scale;
	}
	
	private BufferedImage createImage(Rectangle rectangle) {
		
		return new BufferedImage(rectangle.width, rectangle.height, BufferedImage.TYPE_INT_ARGB);
	}
}
