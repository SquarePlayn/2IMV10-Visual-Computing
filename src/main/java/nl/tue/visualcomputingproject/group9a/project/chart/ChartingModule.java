package nl.tue.visualcomputingproject.group9a.project.chart;

import com.google.common.eventbus.EventBus;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChartingModule implements Module {
	final Logger logger = LoggerFactory.getLogger(ChartingModule.class);
	
	@Override
	public void startup(EventBus eventBus) {
		logger.info("Charting starting up!");
	}
}
