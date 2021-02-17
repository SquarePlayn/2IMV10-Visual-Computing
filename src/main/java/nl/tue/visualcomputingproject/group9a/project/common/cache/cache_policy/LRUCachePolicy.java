package nl.tue.visualcomputingproject.group9a.project.common.cache.cache_policy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@AllArgsConstructor
public class LRUCachePolicy<T>
		implements CachePolicy<T> {
	/** The logger object of this class. */
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final PriorityQueue<QueueElem<T>> queue;
	private final Map<T, QueueElem<T>> fileMap;
	@Getter
	private long maxSize;
	private final AtomicLong curSize;
	private final AtomicLong id;
	private final Lock lock;
	
	@Getter
	@Setter
	@AllArgsConstructor
	private static class QueueElem<V>
			implements Comparable<QueueElem<V>> {
		private final V file;
		private long size;
		private long id;

		@Override
		public int compareTo(QueueElem elem) {
			return Long.compare(id, elem.id);
		}
	}
	
	public LRUCachePolicy(long maxSize) {
		queue = new PriorityQueue<>();
		fileMap = new ConcurrentHashMap<>();
		this.maxSize = maxSize;
		curSize = new AtomicLong(0);
		id = new AtomicLong(Long.MIN_VALUE);
		lock = new ReentrantLock();
	}
	
	@Override
	public boolean update(CacheFileManager<T> fileManager, T file) {
		long newId = id.incrementAndGet();
		long size = 0;
		if (fileManager.exists(file)) {
			size = fileManager.sizeOf(file);
		}
		
		long addSize = 0;
		lock.lock();
		try {
			QueueElem<T> elem = fileMap.get(file);
			if (elem == null) {
				addSize = size;
				elem = new QueueElem<>(file, size, newId);
				fileMap.put(file, elem);
				queue.add(elem);
				return true;
				
			} else {
				addSize = size - elem.size;
				queue.remove(elem);
				elem.size = size;
				elem.id = newId;
				queue.add(elem);
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
	public boolean remove(T file) {
		lock.lock();
		try {
			QueueElem<T> elem = fileMap.get(file);
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
	public boolean isRegistered(T file) {
		return fileMap.containsKey(file);
	}
	
	@Override
	public void setMaxSize(long size) {
		maxSize = size;
	}
	
	@Override
	public long getCurSize() {
		return curSize.get();
	}

	private void checkSize(CacheFileManager<T> fileManager) {
		while (curSize.get() > maxSize) {
			QueueElem<T> elem;
			lock.lock();
			try {
				elem = queue.poll();
				if (elem == null) return;
				if (curSize.get() <= maxSize) {
					queue.add(elem);
					return;
				}
				curSize.addAndGet(-elem.size);
			} finally {
				lock.unlock();
			}
			LOGGER.info("Removed cache file " + elem.file);
			fileManager.cacheDeleteFile(elem.file);
		}
	}
	
}
