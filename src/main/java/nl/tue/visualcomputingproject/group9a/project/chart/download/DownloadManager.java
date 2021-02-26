package nl.tue.visualcomputingproject.group9a.project.chart.download;

import com.google.common.eventbus.EventBus;
import nl.tue.visualcomputingproject.group9a.project.chart.MapSheetCacheManager;
import nl.tue.visualcomputingproject.group9a.project.chart.events.ExtractionRequestEvent;
import nl.tue.visualcomputingproject.group9a.project.chart.wfs.MapSheet;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.*;

public class DownloadManager {
	/** The logger of this class. */
	static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	private final Map<QualityLevel, Queue<DownloadJob>> queues = new HashMap<QualityLevel, Queue<DownloadJob>>() {{
		put(QualityLevel.FIVE_BY_FIVE, new ArrayDeque<>());
		put(QualityLevel.HALF_BY_HALF, new ArrayDeque<>());
		put(QualityLevel.LAS, new ArrayDeque<>());
	}};
	private final EventBus eventbus;
	private final MapSheetCacheManager cacheManager;
	private final DownloadThread downloadThread;
	
	public DownloadManager(EventBus eventbus, MapSheetCacheManager cacheManager) {
		this.eventbus = eventbus;
		this.cacheManager = cacheManager;
		downloadThread = new DownloadThread(this, cacheManager);
		downloadThread.start();
		logger.info("Download manager ready!");
	}
	
	public Optional<DownloadJob> getNextJob() {
		synchronized (queues) {
			QualityLevel q = QualityLevel.getWorst();
			while (true) {
				Queue<DownloadJob> queue = queues.get(q);
				
				DownloadJob job = queue.peek();
				if (job != null) {
					return Optional.of(job);
				}
				
				if (q.equals(QualityLevel.getBest())) {
					return Optional.empty();
				} else {
					q = q.next();
				}
			}
		}
	}
	
	public void requestDownload(MapSheet sheet, Collection<ChunkPosition> positions, QualityLevel level) {
		synchronized (queues) {
			//This check makes more logical sense in LookupManager, but is here to avoid a race condition.
			//Namely:
			// If a download finishes after the check but before we enter the sync block,
			//  then we can download the same mapsheet twice. A classic TOCTOU problem..
			//TODO: What if sheet is being written currently so sheet exists in cache but isn't done yet?
			if (cacheManager.isSheetAvailable(sheet, level)) {
				logger.info("Skipping download of sheet {} at level {} since it's already available...", sheet.getBladnr(), level.toString());
				eventbus.post(new ExtractionRequestEvent(sheet, level, positions));
			} else {
				Queue<DownloadJob> existingJobs = queues.get(level);
				for (DownloadJob job : existingJobs) {
					if (job.getSheet().equals(sheet)) {
						job.getChunksRequested().addAll(positions);
						return;
					}
				}
				
				DownloadJob newJob = new DownloadJob(sheet, level, new HashSet<>(positions));
				existingJobs.add(newJob);
				logger.info("New download job! {}", newJob);
			}
		}
	}
	
	public void cancelDownload(Collection<ChunkPosition> positions) {
		synchronized (queues) {
			for (Queue<DownloadJob> jobs : queues.values()) {
				for (DownloadJob job : jobs) {
					job.getChunksRequested().removeAll(positions);
				}
			}
		}
	}
	
	public void downloadCompleted(DownloadJob job) {
		synchronized (queues) {
			for (Queue<DownloadJob> jobs : queues.values()) {
				jobs.remove(job);
			}
		}
		
		logger.info("Download job {} has completed!", job);
		
		//Post extraction event.
		eventbus.post(new ExtractionRequestEvent(job.getSheet(), job.getLevel(), job.getChunksRequested()));
	}
}
