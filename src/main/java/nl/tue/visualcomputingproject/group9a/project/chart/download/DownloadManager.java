package nl.tue.visualcomputingproject.group9a.project.chart.download;

import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class DownloadManager {
	private Map<QualityLevel, Queue<DownloadJob>> queues = new HashMap<QualityLevel, Queue<DownloadJob>>() {{
		put(QualityLevel.FIVE_BY_FIVE, new ArrayDeque<>());
		put(QualityLevel.HALF_BY_HALF, new ArrayDeque<>());
		put(QualityLevel.LAS, new ArrayDeque<>());
	}};
	private Set<DownloadJob> jobs = new HashSet<>();
	private ReentrantLock mutex = new ReentrantLock(true);
	
	public void requestDownload() {
	
	}
}
