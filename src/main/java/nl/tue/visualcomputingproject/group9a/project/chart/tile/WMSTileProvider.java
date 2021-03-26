package nl.tue.visualcomputingproject.group9a.project.chart.tile;

import org.geotools.map.MapContent;
import org.geotools.ows.ServiceException;
import org.geotools.ows.wms.*;
import org.geotools.ows.wms.map.WMSLayer;
import org.geotools.ows.wmts.WebMapTileServer;
import org.geotools.ows.wmts.map.WMTSMapLayer;
import org.geotools.ows.wmts.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class WMSTileProvider implements TileProvider {
	static final Logger logger = LoggerFactory.getLogger(WMSTileProvider.class);
	
	private final WebMapServer wms;
	private final WMSCapabilities capabilities;
	private final Layer layer;
	
	public WMSTileProvider(URL url, String layerName) throws IOException, ServiceException {
		logger.info("Enumerating WMS server {}...", url.toString());

		wms = new WebMapServer(url);
		capabilities = wms.getCapabilities();
		Layer[] layers = WMSUtils.getNamedLayers(capabilities);
		layer = Arrays.stream(layers).filter(s -> s.getName().equalsIgnoreCase(layerName)).findFirst().get();
		
		for(int i=0; i< layers.length; i++){
			// Print layer info
			System.out.println("Layer: ("+i+")"+layers[i].getName());
			System.out.println("       "+layers[i].getTitle());
			System.out.println("       "+layers[i].getChildren().length);
			System.out.println("       "+layers[i].getBoundingBoxes());
			CRSEnvelope env = layers[i].getLatLonBoundingBox();
			System.out.println("       "+env.getLowerCorner()+" x "+env.getUpperCorner());
			
			// Get layer styles
			List styles = layers[i].getStyles();
			for (Iterator it = styles.iterator(); it.hasNext();) {
				StyleImpl elem = (StyleImpl) it.next();
				
				// Print style info
				System.out.println("Style:");
				System.out.println("  Name:"+elem.getName());
				System.out.println("  Title:"+elem.getTitle());
			}
		}
	}
	
	@Override
	public MapContent getMapContent() {
		MapContent mapcontent = new MapContent();
		mapcontent.addLayer(new WMSLayer(wms, layer));
		mapcontent.setTitle(layer.getTitle());
		logger.info("Created map content with crs {}...", mapcontent.getCoordinateReferenceSystem());
		return mapcontent;
	}
}
