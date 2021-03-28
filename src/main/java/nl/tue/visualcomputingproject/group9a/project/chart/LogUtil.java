package nl.tue.visualcomputingproject.group9a.project.chart;

import org.geotools.util.logging.Logging;

public class LogUtil {
	public static void setupGeotools() throws ClassNotFoundException {
		Logging.ALL.setLoggerFactory("org.geotools.util.logging.CommonsLoggerFactory");
	}
}
