package nl.tue.visualcomputingproject.group9a.project.chart.download;

import nl.tue.visualcomputingproject.group9a.project.chart.MapSheetCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Optional;

public class DownloadThread
		extends Thread {
	/** The logger of this class. */
	static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	private final DownloadManager manager;
	private final MapSheetCacheManager cacheManager;
	
	public DownloadThread(DownloadManager manager, MapSheetCacheManager cacheManager) {
		super("download-thread");
		this.manager = manager;
		this.cacheManager = cacheManager;
		setDaemon(true);
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				Optional<DownloadJob> optionalJob = manager.getNextJob();
				if (optionalJob.isPresent()) {
					
					doJob(optionalJob.get());
					
				} else {
					Thread.sleep(1000);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void doJob(DownloadJob job) throws IOException {
		URL url = job.getSheet().getDownloadUrl(job.getLevel());
		logger.info("Downloading {} for {}...", url, job);
		
		try (BufferedInputStream in = new BufferedInputStream(url.openStream())) {
			try (OutputStream out = job.getClaim().getOutputStream()) {
				byte dataBuffer[] = new byte[1024];
				int bytesRead;
				while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
					out.write(dataBuffer, 0, bytesRead);
				}
			}
			
		}
		
		manager.downloadCompleted(job);
		logger.info("Done!");
	}
}
