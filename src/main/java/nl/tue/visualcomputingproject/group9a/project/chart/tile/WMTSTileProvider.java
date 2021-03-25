package nl.tue.visualcomputingproject.group9a.project.chart.tile;

import org.geotools.map.MapContent;
import org.geotools.ows.ServiceException;
import org.geotools.ows.wms.StyleImpl;
import org.geotools.ows.wmts.WebMapTileServer;
import org.geotools.ows.wmts.map.WMTSMapLayer;
import org.geotools.ows.wmts.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class WMTSTileProvider implements TileProvider {
	static final Logger logger = LoggerFactory.getLogger(WMTSTileProvider.class);
	
	private final WebMapTileServer wmts;
	private final WMTSCapabilities capabilities;
	
	public WMTSTileProvider(URL url) throws IOException, ServiceException {
		logger.info("Enumerating WMTS server {}...", url.toString());

		wmts = new WebMapTileServer(url);
		capabilities = wmts.getCapabilities();
		
		List<WMTSLayer> layers = capabilities.getLayerList();
		for (WMTSLayer layer : layers) {
			logger.info("- Layer: " + layer.getName());
			logger.info("         " + layer.getTitle());
			for (StyleImpl style : layer.getStyles()) {
				// Print style info
				logger.info("  - Style:");
				logger.info("      Name:  " + style.getName());
				logger.info("      Title: " + style.getTitle().toString());
			}
			for (String tileMatrixId : layer.getTileMatrixLinks().keySet()) {
				List<TileMatrixLimits> limits = layer.getTileMatrixLinks().get(tileMatrixId).getLimits();
				TileMatrixSet matrixSet = wmts.getCapabilities().getMatrixSet(tileMatrixId);
				
				List<TileMatrix> matrices = matrixSet.getMatrices();
				
				for (TileMatrix matrix : matrices) {
					logger.info("  - LAYER " + layer.getName() + ", crs: " + matrix.getCrs().getName()
						+ ", matrixSet id : " + tileMatrixId
						+ ", matrix " + matrix.getIdentifier()
						+ ", matrixGrid" + matrix.getMatrixWidth() + "x" + matrix.getMatrixHeight()
						+ ", tileSize" + matrix.getTileWidth() + "x" + matrix.getTileHeight());
				}
			}
		}
	}
	
	@Override
	public MapContent getMapContent() {
		MapContent mapcontent = new MapContent();
		mapcontent.addLayer(new WMTSMapLayer(wmts, capabilities.getLayerList().get(0)));
		mapcontent.setTitle(capabilities.getLayerList().get(0).getTitle());
		return mapcontent;
	}
}
