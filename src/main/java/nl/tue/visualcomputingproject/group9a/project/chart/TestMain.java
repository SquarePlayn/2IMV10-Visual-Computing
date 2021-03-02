package nl.tue.visualcomputingproject.group9a.project.chart;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import nl.tue.visualcomputingproject.group9a.project.chart.visualize.Visualizer;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.CachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.LRUCachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkId;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;
import nl.tue.visualcomputingproject.group9a.project.common.event.ProcessorChunkRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

/**
 * The main class and start point of the application.
 */
@SuppressWarnings("UnstableApiUsage")
public class TestMain {
	/** The logger of this class. */
	static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	/** The modules of this application. */
	static final Module[] modules = {
		new ChartingModule(),
	};
	
	/**
	 * The entrypoint of the application.
	 * 
	 * @param args The commandline arguments.
	 */
	public static void main(String[] args) {
		new TestMain().run(args);
	}

	/**
	 * The non-static entry point of the application.
	 * 
	 * @param args The commandline arguments.
	 */
	public void run(String[] args) {
		try {
			logger.info("Setting up cache manager...");
			CachePolicy diskPolicy = new LRUCachePolicy(5 * CachePolicy.SIZE_GiB);
			CachePolicy memoryPolicy = new LRUCachePolicy(2 * CachePolicy.SIZE_GiB);
			logger.info("Starting up modules...");
			EventBus bus = new AsyncEventBus(Settings.executorService);
			for (Module mod : modules) {
				mod.startup(bus, diskPolicy, memoryPolicy);
			}
			logger.info("Finished starting up modules!");
			
			new Visualizer(bus);
			
			ArrayList<ChunkId> l = new ArrayList<>();
			l.add(new ChunkId(new ChunkPosition(
				150001,375001, 20000, 20000
			), QualityLevel.FIVE_BY_FIVE));
			bus.post(new ProcessorChunkRequestedEvent(l));
			Thread.sleep(1000_000_000_000L);
		} catch (Exception e) {
			logger.error("An exception happened!", e);
		}
	}
}
