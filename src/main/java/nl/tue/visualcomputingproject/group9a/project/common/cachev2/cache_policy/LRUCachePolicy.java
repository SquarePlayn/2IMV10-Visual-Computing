package nl.tue.visualcomputingproject.group9a.project.common.cachev2.cache_policy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.cachev2.CacheManager;
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
public class LRUCachePolicy
		implements CachePolicy {
	/** The logger object of this class. */
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final PriorityQueue<QueueElem<?>> queue;
	private final Map<Object, QueueElem<?>> fileMap;
	@Getter
	private long maxSize;
	private final AtomicLong curSize;
	private final AtomicLong nextOrdering;
	private final Lock lock;
	
	@AllArgsConstructor
	private static class QueueElem<V>
			implements Comparable<QueueElem<?>> {
		private final CacheManager<V> manager;
		private final V id;
		private long size;
		private long ordering;

		@Override
		public int compareTo(QueueElem<?> elem) {
			return Long.compare(ordering, elem.ordering);
		}
		
		public void delete() {
			manager.notifyCacheDelete(id);
		}
		
	}
	
	public LRUCachePolicy(long maxSize) {
		queue = new PriorityQueue<>();
		fileMap = new ConcurrentHashMap<>();
		this.maxSize = maxSize;
		curSize = new AtomicLong(0);
		nextOrdering = new AtomicLong(Long.MIN_VALUE);
		lock = new ReentrantLock();
	}
	
	@Override
	public <T> boolean update(CacheManager<T> cacheManager, T id) {
		long newOrdering = this.nextOrdering.incrementAndGet();
		long size = 0;
		if (cacheManager.exists(id)) {
			size = cacheManager.sizeOf(id);
		}
		
		long addSize = 0;
		lock.lock();
		try {
			QueueElem<?> elem = fileMap.get(id);
			if (elem == null) {
				addSize = size;
				elem = new QueueElem<>(cacheManager, id, size, newOrdering);
				fileMap.put(id, elem);
				queue.add(elem);
				return true;
				
			} else {
				if (cacheManager != elem.manager) {
					throw new IllegalStateException("The cache manager " + cacheManager +
							" tried to update the file " + id +
							" which is owned by " + elem.manager);
				}
				addSize = size - elem.size;
				queue.remove(elem);
				elem.size = size;
				elem.ordering = newOrdering;
				queue.add(elem);
				return false;
			}
			
		} finally {
			if (addSize != 0) {
				curSize.addAndGet(addSize);
			}
			lock.unlock();
			checkSize();
		}
	}

	@Override
	public <T> boolean remove(T file) {
		lock.lock();
		try {
			QueueElem<?> elem = fileMap.get(file);
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
	public <T> boolean isRegistered(T file) {
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

	private void checkSize() {
		while (curSize.get() > maxSize) {
			QueueElem<?> elem;
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
			elem.delete();
			LOGGER.info("Removed cache file " + elem.id);
		}
	}
	
}
