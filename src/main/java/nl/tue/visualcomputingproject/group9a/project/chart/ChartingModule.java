package nl.tue.visualcomputingproject.group9a.project.chart;

import com.google.common.eventbus.EventBus;
import nl.tue.visualcomputingproject.group9a.project.chart.assembly.ChunkAssemblyManager;
import nl.tue.visualcomputingproject.group9a.project.chart.download.DownloadManager;
import nl.tue.visualcomputingproject.group9a.project.chart.extractor.Extractor;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.CachePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Class for the charting module.
 */
@SuppressWarnings("UnstableApiUsage")
public class ChartingModule
	implements Module {
	/**
	 * The logger of this class.
	 */
	static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private ChunkAssemblyManager assemblyManager;
	private MapSheetCacheManager cacheManager;
	private DownloadManager downloadManager;
	private LookupManager lookupManager;
	private Extractor extractor;
	
	@Override
	public void startup(EventBus eventBus, CachePolicy diskPolicy, CachePolicy memoryPolicy)
		throws IOException, ClassNotFoundException {
		logger.info("Charting starting up!");
		LogUtil.setupGeotools();
		this.cacheManager = new MapSheetCacheManager(diskPolicy);
		assemblyManager = new ChunkAssemblyManager(eventBus);
		downloadManager = new DownloadManager(eventBus, this.cacheManager);
		lookupManager = new LookupManager(eventBus, downloadManager, assemblyManager);
		extractor = new Extractor(eventBus, this.cacheManager);
		logger.info("Charting is ready!");
	}
}
