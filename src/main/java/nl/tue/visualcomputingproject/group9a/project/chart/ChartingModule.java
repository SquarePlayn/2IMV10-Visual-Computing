package nl.tue.visualcomputingproject.group9a.project.chart;

import com.google.common.eventbus.EventBus;
import nl.tue.visualcomputingproject.group9a.project.chart.assembly.ChunkAssemblyManager;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for the charting module.
 */
public class ChartingModule
		implements Module {
	/** The logger of this class. */
	static final Logger logger = LoggerFactory.getLogger(ChartingModule.class);
	private ChunkAssemblyManager assemblyManager;
	
	@Override
	public void startup(EventBus eventBus) {
		logger.info("Charting starting up!");
		assemblyManager = new ChunkAssemblyManager(eventBus);
	}
}
