package nl.tue.visualcomputingproject.group9a.project.common.cache.cache_policy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheFileManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@AllArgsConstructor
public class LRUCachePolicy
		implements CachePolicy {
	private final PriorityBlockingQueue<QueueElem> queue;
	private final Map<File, QueueElem> fileMap;
	private final long maxSize;
	private final AtomicLong curSize;
	private final AtomicLong id;
	private final Lock lock;
	
	@Getter
	@Setter
	@AllArgsConstructor
	private static class QueueElem
			implements Comparable<QueueElem> {
		private final File file;
		private long size;
		private long id;

		@Override
		public int compareTo(QueueElem elem) {
			return Long.compare(id, elem.id);
		}
	}
	
	public LRUCachePolicy(long maxSize) {
		queue = new PriorityBlockingQueue<>();
		fileMap = new ConcurrentHashMap<>();
		this.maxSize = maxSize;
		curSize = new AtomicLong(0);
		id = new AtomicLong(Long.MIN_VALUE);
		lock = new ReentrantLock();
	}
	
	@Override
	public boolean update(CacheFileManager fileManager, File file) {
		long newId = id.incrementAndGet();
		long size = 0;
		if (file.exists()) {
			try {
				size = Files.size(file.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		long addSize = 0;
		lock.lock();
		try {
			QueueElem elem = fileMap.get(file);
			if (elem == null) {
				addSize = size;
				elem = fileMap.put(file, new QueueElem(file, size, newId));
				queue.put(elem);
				return true;
				
			} else {
				addSize = size - elem.size;
				queue.remove(elem);
				elem.size = size;
				elem.id = newId;
				queue.put(elem);
				return false;
			}
			
		} finally {
			if (addSize != 0) {
				curSize.addAndGet(addSize);
			}
			lock.unlock();
			checkSize(fileManager);
		}
	}

	@Override
	public boolean remove(File file) {
		lock.lock();
		try {
			QueueElem elem = fileMap.get(file);
			if (elem == null) {
				return false;
			}
			fileMap.remove(file);
			queue.remove(elem);
			curSize.addAndGet(-elem.size);
			
		} finally {
			lock.unlock();
		}
		return true;
	}

	@Override
	public boolean isRegistered(File file) {
		return fileMap.containsKey(file);
	}

	private void checkSize(CacheFileManager fileManager) {
		while (curSize.get() <= maxSize) {
			try {
				QueueElem elem = queue.take();
				if (curSize.get() > maxSize) return;
				curSize.addAndGet(-elem.size);
				fileManager.cacheDeleteFile(elem.file);
				
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
}
