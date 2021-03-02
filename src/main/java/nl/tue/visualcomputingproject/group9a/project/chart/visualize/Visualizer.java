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
	
	@Subscribe
	public void loadedEvent(ChartChunkLoadedEvent event) throws IOException {
		logger.info("Got chunk!");
		
		Set<Double> xCoords = new HashSet<>(), yCoords = new HashSet<>();
		double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
		double maxZ = -Double.MAX_VALUE, minZ = Double.MAX_VALUE;
		
		for (Point p : event.getChunk().getData().getPointIterator()) {
			xCoords.add(p.getX());
			yCoords.add(p.getY());
			minX = Math.min(minX, p.getX());
			maxX = Math.max(maxX, p.getX());
			minY = Math.min(minY, p.getY());
			maxY = Math.max(maxY, p.getY());
			if (p.getAlt() < 1000) {
				maxZ = Math.max(maxZ, p.getAlt());
				minZ = Math.min(minZ, p.getAlt());
			}
		}
		
		maxZ = 50; //Hardcoded
		
		double w = maxX - minX;
		double h = maxY - minY;
		
		double wRatio = (double) (xCoords.size()-1) / w;
		double hRatio = (double) (yCoords.size()-1) / h;
		
		logger.info("Decided to use: {} x {} image, w = {}, h = {}, wRatio = {}, hRatio = {}, minZ = {}, maxZ = {}", xCoords.size(), yCoords.size(), w, h, wRatio, hRatio, minZ, maxZ);
		
		BufferedImage image = new BufferedImage(xCoords.size(), yCoords.size(), BufferedImage.TYPE_USHORT_GRAY);
		WritableRaster raster = image.getRaster();
		
		for (Point p : event.getChunk().getData().getPointIterator()) {
			double x = (p.getX() - minX) * wRatio;
			double y = (p.getY() - minY) * hRatio;
			if (x < 0 || y < 0) {
				logger.error("AAAAA {} x {}", x, y);
			}
			if (x >= image.getWidth() || y >= image.getHeight()) {
				logger.error("AAAAA {} x {}", x, y);
			}
			double r = Math.max(Math.min((p.getAlt() - minZ) / (maxZ - minZ), 1.0), 0.0);
			raster.setPixel((int) x, image.getHeight() - (int)y - 1, new int[]{
				(int)(r * Short.MAX_VALUE)
			});
		}
		
		ImageIO.write(image, "png", new File("test.png"));
		logger.info("Written chunk!");
	}
}
