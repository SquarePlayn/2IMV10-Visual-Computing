package nl.tue.visualcomputingproject.group9a.project.chart;

import com.google.common.eventbus.EventBus;
import nl.tue.visualcomputingproject.group9a.project.chart.assembly.ChunkAssemblyManager;
import nl.tue.visualcomputingproject.group9a.project.chart.download.DownloadManager;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Class for the charting module.
 */
public class ChartingModule
		implements Module {
	/** The logger of this class. */
	static final Logger logger = LoggerFactory.getLogger(ChartingModule.class);
	private ChunkAssemblyManager assemblyManager;
	private MapSheetCacheManager cacheManager;
	private DownloadManager downloadManager;
	private LookupManager lookupManager;
	
	@Override
	public void startup(EventBus eventBus) throws IOException {
		logger.info("Charting starting up!");
		cacheManager = new MapSheetCacheManager();
		assemblyManager = new ChunkAssemblyManager(eventBus);
		downloadManager = new DownloadManager(eventBus);
		lookupManager = new LookupManager(eventBus, cacheManager, downloadManager, assemblyManager);
		logger.info("Charting is ready!");
	}
}
