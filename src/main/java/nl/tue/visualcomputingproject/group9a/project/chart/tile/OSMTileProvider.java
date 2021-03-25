package nl.tue.visualcomputingproject.group9a.project.chart.tile;

import org.geotools.map.MapContent;
import org.geotools.tile.TileService;
import org.geotools.tile.impl.osm.OSMService;

public class OSMTileProvider implements TileProvider {
	@Override
	public MapContent getMapContent() {
		MapContent mapcontent = new MapContent();

		mapcontent.setTitle("OpenStreetMap");
		
		String baseURL = "http://tile.openstreetmap.org/";
		TileService service = new OSMService("OSM", baseURL);

		mapcontent.addLayer(new BiggerTileLayer(service));

		return mapcontent;
	}
}
