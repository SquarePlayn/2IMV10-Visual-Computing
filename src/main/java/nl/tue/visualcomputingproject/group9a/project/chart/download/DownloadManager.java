package nl.tue.visualcomputingproject.group9a.project.chart.download;

import com.google.common.eventbus.EventBus;
import nl.tue.visualcomputingproject.group9a.project.chart.events.ExtractionRequestEvent;
import nl.tue.visualcomputingproject.group9a.project.chart.wfs.MapSheet;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DownloadManager {
	/** The logger of this class. */
	static final Logger logger = LoggerFactory.getLogger(DownloadManager.class);
	
	private final Map<QualityLevel, Queue<DownloadJob>> queues = new HashMap<QualityLevel, Queue<DownloadJob>>() {{
		put(QualityLevel.FIVE_BY_FIVE, new ArrayDeque<>());
		put(QualityLevel.HALF_BY_HALF, new ArrayDeque<>());
		put(QualityLevel.LAS, new ArrayDeque<>());
	}};
	private final EventBus eventbus;
	
	public DownloadManager(EventBus eventbus) {
		this.eventbus = eventbus;
	}
	
	public void requestDownload(MapSheet sheet, Collection<ChunkPosition> positions, QualityLevel level) {
		//TODO: Potential race condition:
		// If a download finishes after this function gets called but before we enter the sync block,
		//  then we can download the same mapsheet twice. Maybe implement a check here?
		synchronized (queues) {
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
