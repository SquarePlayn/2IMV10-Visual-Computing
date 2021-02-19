package nl.tue.visualcomputingproject.group9a.project.chart;

import com.google.common.eventbus.EventBus;
import nl.tue.visualcomputingproject.group9a.project.chart.assembly.ChunkAssemblyManager;
import nl.tue.visualcomputingproject.group9a.project.chart.download.DownloadManager;
import nl.tue.visualcomputingproject.group9a.project.chart.extractor.Extractor;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheFileManager;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkId;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;
import nl.tue.visualcomputingproject.group9a.project.common.event.ProcessorChunkRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
	private Extractor extractor;
	
	@Override
	public void startup(EventBus eventBus, CacheFileManager<File> cacheManager) throws IOException {
		logger.info("Charting starting up!");
		this.cacheManager = new MapSheetCacheManager(cacheManager);
		assemblyManager = new ChunkAssemblyManager(eventBus);
		downloadManager = new DownloadManager(eventBus, this.cacheManager);
		lookupManager = new LookupManager(eventBus, downloadManager, assemblyManager);
		extractor = new Extractor(eventBus, this.cacheManager);
		logger.info("Charting is ready!");
	}
}
