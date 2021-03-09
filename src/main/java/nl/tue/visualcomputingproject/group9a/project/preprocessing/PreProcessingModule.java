package nl.tue.visualcomputingproject.group9a.project.preprocessing;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.cache.policy.CachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.cache.stream.ZipBufferedFileStreamFactory;
import nl.tue.visualcomputingproject.group9a.project.common.cache.write_back.WriteBackCacheManager;
import nl.tue.visualcomputingproject.group9a.project.common.cache.write_back.WriteBackReadCacheClaim;
import nl.tue.visualcomputingproject.group9a.project.common.cache.write_back.WriteBackReadWriteCacheClaim;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;
import nl.tue.visualcomputingproject.group9a.project.common.event.ChartChunkLoadedEvent;
import nl.tue.visualcomputingproject.group9a.project.common.event.ProcessorChunkLoadedEvent;
import nl.tue.visualcomputingproject.group9a.project.common.event.ProcessorChunkRequestedEvent;
import nl.tue.visualcomputingproject.group9a.project.common.event.RendererChunkStatusEvent;
import nl.tue.visualcomputingproject.group9a.project.common.util.Pair;
import nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.Generator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class for the pre-processing module.
 */
@SuppressWarnings("UnstableApiUsage")
public class PreProcessingModule
		implements Module {
	/** The logger of this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	/** The thread used for io-operations. */
	private static final ExecutorService ioThread = Executors.newSingleThreadExecutor();
	
	/** Map storing which chunks are requested from the chart module. */
	private final Map<ChunkPosition, MeshChunkId> requesting = new HashMap<>();
	/** Set storing which chunks are currently being processed. */
	private final Set<MeshChunkId> processing = new HashSet<>();
	/** Set storing which chunks should be delivered to the renderer module. */
	private final Set<MeshChunkId> deliver = new HashSet<>();
	/** The lock used for concurrently accessing the requesting map. */
	private final Lock lock = new ReentrantLock();
	
	/** The event bus used in the application. */
	private EventBus eventBus;
	/** The cache manager used to store the mesh chunk data. */
	private WriteBackCacheManager<MeshChunkData> cache;
	
	@Override
	public void startup(EventBus eventBus, CachePolicy diskPolicy, CachePolicy memoryPolicy) {
		LOGGER.info("Preprocessing starting up!");
		
		this.eventBus = eventBus;
		eventBus.register(this);
		
		cache = new WriteBackCacheManager<>(
				memoryPolicy,
				diskPolicy,
				Settings.CACHE_DIR, "mesh_chunk",
				new ZipBufferedFileStreamFactory(),MeshChunkData.createSerializer());
		cache.indexCache(MeshChunkId.createMeshChunkIdFactory());
	}

	/**
	 * Method handling the renderer status events.
	 * 
	 * @param e The renderer status event.
	 */
	@Subscribe
	public void rendererStatus(final RendererChunkStatusEvent e) {
		LOGGER.info("Got renderer status event!");
		if (!e.getNewChunks().isEmpty()) {
			// First obtain read-access for all requested chunks, if they exist.
			// Also send memory cached chunks back to the renderer.
			List<MeshChunkId> request = new ArrayList<>();
			List<Pair<MeshChunkId, WriteBackReadCacheClaim<MeshChunkData>>> claims = new ArrayList<>();
			for (ChunkPosition pos : e.getNewChunks()) {
				MeshChunkId id = new MeshChunkId(
						pos,
						QualityLevel.getWorst(),
						Settings.VERTEX_TYPE,
						Settings.MESH_TYPE);
				// Scan the cache for each quality level, starting from the best quality.
				WriteBackReadCacheClaim<MeshChunkData> claim = null;
				QualityLevel level = null;
				boolean found = false;
				while (level != QualityLevel.getWorst()) {
					if (level == null) level = QualityLevel.getBest();
					else level = level.prev();
					
					lock.lock();
					try {
						if (processing.contains(id)) {
							deliver.add(id);
							found = true;
							break;
						}
					} finally {
						lock.unlock();
					}

					claim = cache.requestReadClaim(id);
					if (claim == null) continue; // Cache miss.
					if (claim.isInMemory()) {
						// Cache in memory.
						// Simply post the data and release the claim.
						LOGGER.info("Memory cache hit: " + id.getPosition());
						LOGGER.info("Posting preprocessor loaded event!");
						eventBus.post(new ProcessorChunkLoadedEvent(
								new Chunk<>(id, claim.get())));
						cache.releaseCacheClaim(claim);

					} else {
						// Cache on disk.
						// Keep the claim open and read the data later.
						LOGGER.info("Disk cache hit: " + id.getPosition());
						claims.add(new Pair<>(id, claim));
					}
					found = true;
					break;
				}
				if (!found) {
					LOGGER.info("Cache miss: " + id.getPosition());
					// Request the lowest quality data from chart module later.
					request.add(id);
				}
			}

			// Request the cache misses from the chart module.
			if (!request.isEmpty()) {
				List<ChunkId> req = new ArrayList<>(request.size());
				lock.lock();
				try {
					for (MeshChunkId id : request) {
						requesting.put(id.getPosition(), id);
						req.add(id.asChunkId());
					}
				} finally {
					lock.unlock();
				}
				LOGGER.info("Posting chunk request event!");
				eventBus.post(new ProcessorChunkRequestedEvent(req));
			}
			
			// Load the chunks from disk.
			if (!claims.isEmpty()) {
				ioThread.submit(() -> {
					for (Pair<MeshChunkId, WriteBackReadCacheClaim<MeshChunkData>> pair : claims) {
						LOGGER.info("Posting preprocessor loaded event!");
						eventBus.post(new ProcessorChunkLoadedEvent(
								new Chunk<>(pair.getFirst(), pair.getSecond().get())));
						cache.releaseCacheClaim(pair.getSecond());
					}
				});
			}
		}
		
		if (!e.getUnloadedChunks().isEmpty()) {
			lock.lock();
			try {
				for (ChunkPosition pos : e.getUnloadedChunks()) {
					requesting.remove(pos);
					for (QualityLevel level : QualityLevel.values()) {
						MeshChunkId id = new MeshChunkId(
								pos,
								level,
								Settings.VERTEX_TYPE,
								Settings.MESH_TYPE);
						deliver.remove(id);
					}
				}
				
			} finally {
				lock.unlock();
			}
		}
	}

	/**
	 * Method handling the chunk loaded event.
	 * 
	 * @param e The chunk loaded event.
	 */
	@Subscribe
	public void chunkLoaded(ChartChunkLoadedEvent e) {
		LOGGER.info("Got chunk loaded event: " + e.getChunk().getChunkId().getPosition());
		// First determine if the event is valid.
		// If so, then update the request map accordingly.
		final MeshChunkId id;
		lock.lock();
		try {
			final ChunkId eventId = e.getChunk().getChunkId();
			MeshChunkId reqId = requesting.get(eventId.getPosition());
			if (reqId == null) {
				// Ignore event since the chunk is not needed anymore.
				return;
			}
			if (Objects.equals(eventId.getPosition(), reqId.getPosition()) && // Wrong position.
					eventId.getQuality().getOrder() < reqId.getQuality().getOrder()) { // Wrong quality.
				LOGGER.warn("Expected '" + reqId.asChunkId() + "', but got: '" + eventId + "'!");
				return;
			}
			
			// Update quality of current ID.
			id = reqId.withQuality(eventId.getQuality());

			// Remove request if the highest quality level has been received.
			if (id.getQuality() == QualityLevel.getBest()) {
				requesting.remove(id.getPosition());

			} else {
				// Update request.
				requesting.put(id.getPosition(), id.withQuality(eventId.getQuality().next()));
			}
			
			if (!processing.add(id)) {
				// The request is already being processed.
				return;
			}
			deliver.add(id);
			
		} finally {
			lock.unlock();
		}
		
		// Pre-process the chunk.
		Settings.executorService.submit(() -> {
			// Check if the data still needs to be processed.
			lock.lock();
			try {
				if (!deliver.contains(id)) {
					LOGGER.info("Ignoring " + id + " since it is no longer requested!");
					return;
				}
				
			} finally {
				lock.unlock();
			}
			
			// Process the data.
			MeshChunkData data;
			try {
				data = Generator
						.createGeneratorFor(id.getQuality())
						.generateChunkData(e.getChunk());
				
			} catch (Exception ex) {
				ex.printStackTrace();
				throw ex;
			}

			lock.lock();
			try {
				// Remove the id from the deliver set and notify the renderer
				// about the data if it is still needed.
				if (deliver.remove(id)) {
					eventBus.post(new ProcessorChunkLoadedEvent(new Chunk<>(
							id,
							data)));
				}
				
			} finally {
				lock.unlock();
			}
			
			// Put the data in the cache.
			WriteBackReadWriteCacheClaim<MeshChunkData> claim = cache.requestReadWriteClaim(id);
			if (claim != null) {
				claim.set(data);
				// Cache new data.
				cache.releaseCacheClaim(claim);
				
				// Delete old data.
				MeshChunkId mcId = id;
				while (mcId.getQuality() != QualityLevel.getWorst()) {
					claim = cache.requestReadWriteClaim(id);
					if (claim != null) {
						claim.delete();
						cache.releaseCacheClaim(claim);
					}
					mcId = mcId.withQuality(mcId.getQuality().prev());
				}
			}
			
			// Finish processing.
			lock.lock();
			try {
				processing.remove(id);
				// Check if the data still needs to be delivered again, and do so if needed.
				if (deliver.remove(id)) {
					eventBus.post(new ProcessorChunkLoadedEvent(new Chunk<>(
							id,
							data)));
				}
			} finally {
				lock.lock();
			}
		});
	}
	
}
