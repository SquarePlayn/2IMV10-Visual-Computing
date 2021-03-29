package nl.tue.visualcomputingproject.group9a.project.chart.tile;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.MapContent;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;

public class TileRenderer {
	static final Logger logger = LoggerFactory.getLogger(TileRenderer.class);
	
	private final TileProvider tileProvider;
	
	public TileRenderer(TileProvider tileProvider) {
		this.tileProvider = tileProvider;
	}
	
	public BufferedImage render(ReferencedEnvelope envelope, int image_width, int image_height) throws TransformException, FactoryException {
		MapContent mapContent = tileProvider.getMapContent();
		
		logger.info("Rendering map image for {} in bounds {} to {}x{}...", mapContent.getTitle(), envelope, image_width, image_height);
		ReferencedEnvelope mapBounds = envelope.transform(mapContent.getCoordinateReferenceSystem(), true);
		
		
		Rectangle imageBounds = new Rectangle(0, 0, image_width, image_height);
		BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = image.createGraphics();
		g2d.setBackground(Color.WHITE);
		g2d.setPaint(Color.WHITE);
//		g2d.fill(imageBounds);
		
		mapContent.getViewport().setScreenArea(imageBounds);
		mapContent.getViewport().setBounds(mapBounds);
		
		//Do the heavy lifting.
		GTRenderer renderer = new StreamingRenderer();
		renderer.setMapContent(mapContent);
		renderer.paint(g2d, imageBounds, mapBounds);
		
		mapContent.dispose();
		
		return image;
	}
}
