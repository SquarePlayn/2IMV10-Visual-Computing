package nl.tue.visualcomputingproject.group9a.project.chart;

import nl.tue.visualcomputingproject.group9a.project.chart.tile.OSMTileProvider;
import nl.tue.visualcomputingproject.group9a.project.chart.tile.TileRenderer;
import nl.tue.visualcomputingproject.group9a.project.chart.tile.WMTSTileProvider;
import nl.tue.visualcomputingproject.group9a.project.chart.wfs.WFSApi;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class Test5 {
	static final Logger logger = LoggerFactory.getLogger(Test5.class);
	final WFSApi api = new WFSApi();
	
	public Test5() throws IOException {
	}
	
	private BufferedImage renderImage(TileRenderer tileRenderer) throws TransformException, FactoryException {
		double x = 150001, y = 375001, w = 20000, h = 20000;
		int image_width = 4001;
		int image_height = 4001;
		
		ReferencedEnvelope mapBounds = new ReferencedEnvelope(x, x + w, y, y + h, api.getCrs());
		return tileRenderer.render(mapBounds, image_width, image_height);
	}
	
	private void renderImage(TileRenderer tileRenderer, String filename) throws TransformException, FactoryException, IOException {
		logger.info("Rendering with {} to {}...", tileRenderer, filename);
		BufferedImage image = this.renderImage(tileRenderer);
		ImageIO.write(image, "png", new File(filename));
	}
	
	public void run(String[] args) throws Exception {
		//URL url = new URL("http://gis.sinica.edu.tw/worldmap/wmts?VERSION=1.0.0&Request=GetCapabilities");
		URL url = new URL("https://services.arcgisonline.com/arcgis/rest/services/World_Imagery/MapServer/WMTS/1.0.0/WMTSCapabilities.xml");
		
		TileRenderer wmtsTileRenderer = new TileRenderer(new WMTSTileProvider(url));
		TileRenderer osmTileRenderer = new TileRenderer(new OSMTileProvider());
		renderImage(wmtsTileRenderer, "test2.png");
		renderImage(osmTileRenderer, "test3.png");
	}
	
	public static void main(String[] args) {
		try {
			LogUtil.setupGeotools();
			new Test5().run(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
