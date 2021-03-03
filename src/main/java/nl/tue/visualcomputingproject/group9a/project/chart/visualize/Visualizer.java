package nl.tue.visualcomputingproject.group9a.project.chart.visualize;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import nl.tue.visualcomputingproject.group9a.project.common.Point;
import nl.tue.visualcomputingproject.group9a.project.common.event.ChartChunkLoadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Visualizer {
	/**
	 * The logger of this class.
	 */
	static final Logger logger = LoggerFactory.getLogger(Visualizer.class);
	
	private final EventBus eventBus;
	
	public Visualizer(EventBus eventBus) {
		this.eventBus = eventBus;
		eventBus.register(this);
	}
	
	private int mapToGridX(ChartChunkLoadedEvent event, double x) {
		switch (event.getChunk().getQualityLevel()) {
			case FIVE_BY_FIVE:
				return (int) ((x - event.getChunk().getPosition().getX()) / 5.0);
			case HALF_BY_HALF:
				return (int) ((x - event.getChunk().getPosition().getX()) * 2.0);
			case LAS:
				throw new UnsupportedOperationException("LAS not supported yet");
		}
		return 0;
	}
	
	private int mapToGridY(ChartChunkLoadedEvent event, double y) {
		switch (event.getChunk().getQualityLevel()) {
			case FIVE_BY_FIVE:
				return (int) ((y - event.getChunk().getPosition().getY()) / 5.0);
			case HALF_BY_HALF:
				return (int) ((y - event.getChunk().getPosition().getY()) * 2.0);
			case LAS:
				throw new UnsupportedOperationException("LAS not supported yet");
		}
		return 0;
	}
	
	@Subscribe
	public void loadedEvent(ChartChunkLoadedEvent event) throws IOException {
		logger.info("Got chunk!");
		
		double maxZ = -Double.MAX_VALUE, minZ = Double.MAX_VALUE;
		
		for (Point p : event.getChunk().getData().getPointIterator()) {
			if (p.getAlt() < 1000) {
				maxZ = Math.max(maxZ, p.getAlt());
				minZ = Math.min(minZ, p.getAlt());
			}
		}
		
		maxZ = 50; //Hardcoded
		
		double far_x = event.getChunk().getPosition().getX() + event.getChunk().getPosition().getWidth();
		double far_y = event.getChunk().getPosition().getY() + event.getChunk().getPosition().getHeight();
		int image_w = mapToGridX(event, far_x) + 1;
		int image_h = mapToGridY(event, far_y) + 1;
		
		logger.info("Decided to use: {} x {} image, minZ = {}, maxZ = {}", image_w, image_h, minZ, maxZ);
		
		BufferedImage image = new BufferedImage(image_w, image_h, BufferedImage.TYPE_USHORT_GRAY);
		WritableRaster raster = image.getRaster();
		
		for (Point p : event.getChunk().getData().getPointIterator()) {
			int x = mapToGridX(event, p.getX());
			int y = mapToGridY(event, p.getY());
			if (x < 0 || y < 0) {
				logger.error("AAAAA {} x {}", x, y);
			}
			if (x >= image.getWidth() || y >= image.getHeight()) {
				logger.error("AAAAA {} x {}", x, y);
			}
			double r = Math.max(Math.min((p.getAlt() - minZ) / (maxZ - minZ), 1.0), 0.0);
			raster.setPixel((int) x, image.getHeight() - (int)y - 1, new int[]{
				(int)(r * (Short.MAX_VALUE * 2))
			});
		}
		
		ImageIO.write(image, "png", new File("test.png"));
		logger.info("Written chunk!");
	}
}
