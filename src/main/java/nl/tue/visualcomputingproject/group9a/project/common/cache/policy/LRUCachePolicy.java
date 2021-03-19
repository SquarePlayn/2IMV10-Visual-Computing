package nl.tue.visualcomputingproject.group9a.project.common.cache.policy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import nl.tue.visualcomputingproject.group9a.project.common.cache.*;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LRUCachePolicy
		implements CachePolicy {
	/** The queue used to determine which file to remove first. */
	private final PriorityQueue<QueueElem<?, ?>> queue;
	/** Map mapping the file ID's to their respective queue element. */
	private final Map<FileId, QueueElem<?, ?>> elemMap;
	/** The lock of this policy. */
	private final Lock lock;
	/** The ordering ID used in the {@link QueueElem}'s. */
	private long ordering = Long.MIN_VALUE;
	/** The maximum size of the cache policy. */
	@Getter
	private long maxSize;
	/** The current size of the cache policy. */
	@Getter
	private long curSize;

	@Override
	public void setMaxSize(long size) {
		lock.lock();
		try {
			maxSize = size;
			checkSize();
			
		} finally {
			lock.unlock();
		}
	}

	/**
	 * The queue element used in the queue.
	 *
	 * @param <Read>      The type of the read claims of the manager.
	 * @param <ReadWrite> The type of the read-write claims of the manager.
	 */
	@Getter
	@AllArgsConstructor
	private static class QueueElem<Read extends ReadCacheClaim, ReadWrite extends ReadWriteCacheClaim>
			implements Comparable<QueueElem<?, ?>> {
		/** The ID of the tracked file. */
		private final FileId id;
		/** The manager to notify after deletion. */
		private final CacheManager<Read, ReadWrite> manager;
		/** The claim used to delete the file. */
		private final ReadWrite claim;
		
		/** The ordering of this element. */
		@Setter
		private long ordering;
		/** The size used in the size calculation for the file. */
		@Setter
		private long size;
		
		@Override
		public int compareTo(QueueElem<?, ?> elem) {
			return Long.compare(ordering, elem.ordering);
		}
		
		/**
		 * Releases the claim to this file.
		 */
		public void release() {
			manager.releaseCacheClaim(claim);
		}
		
	}

	/**
	 * Creates a new Least Recently Used (LRU) cache policy.
	 * 
	 * @param maxSize The maximum size in bytes the sum of the files tracked
	 *                by this policy should have.
	 */
	public LRUCachePolicy(long maxSize) {
		queue = new PriorityQueue<>();
		elemMap = new HashMap<>();
		lock = new ReentrantLock();
		this.maxSize = maxSize;
	}
	
	@Override
	public <Read extends ReadCacheClaim, ReadWrite extends ReadWriteCacheClaim> void track(
			FileId id,
			CacheManager<Read, ReadWrite> manager,
			ReadWrite claim) {
		lock.lock();
		try {
			@SuppressWarnings("unchecked")
			QueueElem<Read, ReadWrite> elem = (QueueElem<Read, ReadWrite>) elemMap.get(id);
			if (elem != null) {
				queue.remove(elem);
				curSize += claim.size() - elem.size;
				elem.size = claim.size();
				elem.ordering = ordering++;
				queue.add(elem);
				
			} else {
				elem = new QueueElem<>(id, manager, claim, ordering++, claim.size());
				queue.add(elem);
				curSize += claim.size();
				elemMap.put(id, elem);
			}
			
			checkSize();
			
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean update(FileId id) {
		lock.lock();
		try {
			QueueElem<?, ?> elem = elemMap.get(id);
			if (elem == null) return false;
			queue.remove(elem);
			curSize += elem.claim.size() - elem.size;
			elem.size = elem.claim.size();
			elem.ordering = ordering++;
			queue.add(elem);
			
			checkSize();
			return true;
			
		} finally {
			lock.unlock();
		}
	}

	@Override
	public boolean untrack(FileId id) {
		lock.lock();
		try {
			QueueElem<?, ?> elem = elemMap.remove(id);
			if (elem == null) return false;
			queue.remove(elem);
			curSize -= elem.size;
			return true;
			
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Checks whether the maximum size is exceeded.
	 * If so, it removes the files in order.
	 */
	private void checkSize() {
		lock.lock();
		try {
			while (curSize > maxSize) {
				QueueElem<?, ?> elem = queue.poll();
				if (elem == null) {
					curSize = 0;
					return;
				}
				elem.claim.delete();
				elem.release();
			}
			
		} finally {
			lock.unlock();
		}
	}
	
}
