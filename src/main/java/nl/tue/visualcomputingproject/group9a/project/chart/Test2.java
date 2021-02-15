package nl.tue.visualcomputingproject.group9a.project.chart;

import nl.tue.visualcomputingproject.group9a.project.chart.wfs.MapSheet;
import nl.tue.visualcomputingproject.group9a.project.chart.wfs.WFSApi;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.FactoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;

public class Test2 {
	static final Logger logger = LoggerFactory.getLogger(Test2.class);

	public static void main(String[] args) {
		try {
			WFSApi api = new WFSApi();
			ReferencedEnvelope bbox = new ReferencedEnvelope(156421.841335, 168141.521335, 375867.552253, 387587.232253,
				api.getCrs());
			Collection<MapSheet> sheets = api.query(bbox);
			for (MapSheet sheet: sheets) {
				logger.info("- {}", sheet);
			}
		} catch (IOException | FactoryException e) {
			e.printStackTrace();
		}
	}
}
