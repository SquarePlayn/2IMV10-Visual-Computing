package nl.tue.visualcomputingproject.group9a.project.chart.wfs;

import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.spatial.BBOX;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.*;

public class WFSApi {
	static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	private final DataStore dataStore;
	private final FeatureSource<SimpleFeatureType, SimpleFeature> source;
	private final String typeName, geomName;
	@Getter
	private final CoordinateReferenceSystem crs;
	
	public WFSApi() throws IOException {
		String getCapabilities = "https://geodata.nationaalgeoregister.nl/ahn3/wfs?REQUEST=GetCapabilities&VERSION=2.0.0&SRS=EPSG:28992";
		
		Map<String, String> connectionParameters = new HashMap<>();
		connectionParameters.put("WFSDataStoreFactory:GET_CAPABILITIES_URL", getCapabilities);
		
		dataStore = DataStoreFinder.getDataStore(connectionParameters);
		
		String[] typeNames = dataStore.getTypeNames();
		
		logger.info("Type names:");
		for (String type : typeNames) {
			logger.info("- {}", type);
		}
		
		typeName = typeNames[0];
		SimpleFeatureType schema = dataStore.getSchema(typeName);
		geomName = schema.getGeometryDescriptor().getLocalName();
		crs = schema.getCoordinateReferenceSystem();
		logger.info("Geom name: {}", geomName);
		
		logger.info("Schema:");
		for (int i = 0; i < schema.getAttributeCount(); i++) {
			logger.info("- {}", schema.getDescriptor(i).toString());
		}
		
		source = dataStore.getFeatureSource(typeName);
		logger.info("Metadata Bounds: {}", source.getBounds());
	}
	
	public GeometryFactory getGeometryFactory() {
		return JTSFactoryFinder.getGeometryFactory(new Hints(
			Hints.CRS,
			crs
		));
	}
	
	public Collection<MapSheet> query(Collection<ChunkPosition> positions) throws FactoryException, IOException {
		logger.info("Querying for {} chunk positions...", positions.size());
		
		double x1 = Double.MAX_VALUE, x2 = -Double.MAX_VALUE, y1 = Double.MAX_VALUE, y2 = -Double.MAX_VALUE;
		for (ChunkPosition p : positions) {
			x1 = Math.min(p.getX(), x1);
			x2 = Math.max(p.getX() + p.getWidth(), x2);
			y1 = Math.min(p.getY(), y1);
			y2 = Math.max(p.getY() + p.getHeight(), y2);
		}
		
		ReferencedEnvelope bbox = new ReferencedEnvelope(x1, x2, y1, y2, crs);
		Collection<MapSheet> sheets = query(bbox);
		
		//Filter results to only include ones that overlap with the chunks.
		List<MapSheet> results = new ArrayList<>();
		GeometryFactory factory = getGeometryFactory();
		for (MapSheet sheet : sheets) {
			boolean found = false;
			for (ChunkPosition position : positions) {
				Geometry chunkGeom = position.getJtsGeometry(factory, getCrs());
				if (chunkGeom.intersects(sheet.getGeom())) {
					results.add(sheet);
					found = true;
					break;
				}
			}
			if (!found) {
				logger.warn("Rejecting sheet! {} {}", sheet.getBladnr(), sheet.getGeom());
			}
		}
		
		return results;
	}
	
	public Collection<MapSheet> query(BoundingBox bbox) throws FactoryException, IOException {
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
		logger.info("Querying for {}...", bbox);
		BBOX filter = ff.bbox(ff.property(geomName), bbox);
		
		String[] props = new String[]{
			geomName,
			"bladnr",
			"has_data_05m_dsm",
			"has_data_5m_dsm",
			"has_data_laz",
		};
		Query query = new Query(typeName, filter, props);
		
		FeatureCollection<SimpleFeatureType, SimpleFeature> fc = source.getFeatures(query);
		
		long counter = 0;
		List<MapSheet> sheets = new ArrayList<>();
		try (FeatureIterator<SimpleFeature> iterator = fc.features()) {
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				MapSheet sheet = new MapSheet(
					(MultiPolygon) feature.getAttribute(0),
					(String) feature.getAttribute(1),
					(Boolean) feature.getAttribute(2),
					(Boolean) feature.getAttribute(3),
					(Boolean) feature.getAttribute(4)
				);
				sheets.add(sheet);
				counter++;
			}
		}
		logger.info("Done! - Fetched {} features!", counter);
		return sheets;
	}
}
