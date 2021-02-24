package nl.tue.visualcomputingproject.group9a.project.preprocessing;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import nl.tue.visualcomputingproject.group9a.project.common.Module;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.cachev2.CacheFileStreamRequester;
import nl.tue.visualcomputingproject.group9a.project.common.cachev2.DiskCacheFileManager;
import nl.tue.visualcomputingproject.group9a.project.common.cachev2.ObjectCacheManager;
import nl.tue.visualcomputingproject.group9a.project.common.cachev2.cache_policy.CachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.cachev2.cache_policy.LRUCachePolicy;
import nl.tue.visualcomputingproject.group9a.project.common.cachev2.stream.BufferedFileStreamFactory;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;
import nl.tue.visualcomputingproject.group9a.project.common.event.ChartChunkLoadedEvent;
import nl.tue.visualcomputingproject.group9a.project.common.event.RendererChunkStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class for the pre-processing module.
 */
@SuppressWarnings("UnstableApiUsage")
public class PreProcessingModule
		implements Module {
	/** The logger of this class. */
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	/** The event bus used in the application. */
	private EventBus eventBus; // TODO maybe store this in {@link Settings}?
	/** The cache manager used to store the mesh chunk data. */
	private ObjectCacheManager<ChunkId, MeshChunkData> diskCache;
	
	private static final ExecutorService ioThread = Executors.newSingleThreadExecutor();
	
	private Map<ChunkPosition, QualityLevel> requesting;
	
	@Override
	public void startup(EventBus eventBus) {
		logger.info("Preprocessing starting up!");
		requesting = new ConcurrentHashMap<>();
		
		this.eventBus = eventBus;
		eventBus.register(this);
		
		// TODO: move up hierarchy.
		CachePolicy diskPolicy = new LRUCachePolicy(CachePolicy.SIZE_GB);
		
		DiskCacheFileManager cacheManager = new DiskCacheFileManager(diskPolicy, Settings.CACHE_DIR);
		
		diskCache = new ObjectCacheManager<>(
				new CacheFileStreamRequester<>(cacheManager, new BufferedFileStreamFactory()),
				MeshChunkData.createCacheFactory());
		
//		cache = new KeepBestCacheManager<>(
//				Settings.CACHE_DIR,
//				ChunkId.createCacheNameFactory("mesh_data" + File.separator),
//				MeshChunkData.createCacheFactory(),
//				new ZipBufferedFileStreamFactory());
//		cache.indexDiskCache();
	}

	/**
	 * Method handling the renderer status events.
	 * 
	 * @param e The renderer status event.
	 */
	@Subscribe
	public void rendererStatus(final RendererChunkStatusEvent e) {
//		if (!e.getNewChunks().isEmpty()) {
//			final List<ChunkPosition> diskCached = new ArrayList<>();
//			final List<ChunkId> request = new ArrayList<>();
//			
//			// First return the chunks which are already in memory.
//			for (ChunkPosition id : e.getNewChunks()) {
//				Pair<MeshChunkData, QualityLevel> data = diskCache.getFromMemory(id);
//				if (data != null) {
//					eventBus.post(new ProcessorChunkLoadedEvent(
//							new Chunk<>(id, data.getSecond(), data.getFirst())));
//					
//				} else if (diskCache.isDiskCached(id)) {
//					diskCached.add(id);
//				} else {
//					request.add(new ChunkId(id, QualityLevel.getWorst()));
//				}
//			}
//			
//			// Then request the missing chunks from the chart module.
//			if (!request.isEmpty()) {
//				for (ChunkId req : request) {
//					requesting.put(req.getPosition(), req.getQuality());
//				}
//				eventBus.post(new ProcessorChunkRequestedEvent(request));
//				request.clear();
//			}
//
//			// Then load the chunks from disk.
//			ioThread.submit(() -> {
//				for (ChunkPosition id : diskCached) {
//					Pair<MeshChunkData, QualityLevel> data = diskCache.getBest(id);
//					if (data == null) {
//						request.add(new ChunkId(id, QualityLevel.getWorst()));
//					} else {
//						eventBus.post(new ProcessorChunkLoadedEvent(
//								new Chunk<>(id, data.getSecond(), data.getFirst())));
//						if (QualityLevel.getBest() != data.getSecond()) {
//							request.add(new ChunkId(id, data.getSecond().next()));
//						}
//					}
//				}
//
//				// Finally request any higher quality data and any unexpected cache-misses from the chart module.
//				if (!request.isEmpty()) {
//					for (ChunkId req : request) {
//						requesting.put(req.getPosition(), req.getQuality());
//					}
//					eventBus.post(new ProcessorChunkRequestedEvent(request));
//				}
//			});
//		}
//		
//		if (!e.getUnloadedChunks().isEmpty()) {
//			ioThread.submit(() -> {
//				for (ChunkPosition id : e.getUnloadedChunks()) {
//					diskCache.removeMemoryCache(id);
//				}
//			});
//		}
	}

	/**
	 * Method handling the chunk loaded event.
	 * 
	 * @param e The chunk loaded event.
	 */
	@Subscribe
	public void chunkLoaded(ChartChunkLoadedEvent e) {
//		final ChunkId id = e.getChunk().getChunkId();
//		final QualityLevel requestedQuality = requesting.get(id.getPosition());
//		
//		// Ignore event since the chunk is not needed anymore.
//		if (requestedQuality == null) {
//			return;
//		}
//		
//		// Remove request if the highest quality level has been received.
//		if (id.getQuality() == QualityLevel.getBest()) {
//			requesting.remove(id.getPosition());
//			
//		} else if (id.getQuality().getOrder() < requestedQuality.getOrder()) {
//			// Ignore event if a better quality is already available.
//			return;
//			
//		} else {
//			// Update request.
//			requesting.put(id.getPosition(), id.getQuality().next());
//		}
//		
//		// Check if the chunk is already cached.
//		if (diskCache.isCached(id)) {
//			MeshChunkData data = diskCache.get(id);
//			if (data != null) {
//				eventBus.post(new ProcessorChunkLoadedEvent(new Chunk<>(
//						e.getChunk().getPosition(),
//						e.getChunk().getQualityLevel(),
//						diskCache.get(id))));
//				return;
//			}
//		}
//
//		// Pre-process the chunk.
//		Settings.executorService.submit(() -> {
//			if (!requesting.containsKey(e.getChunk().getPosition())) return;
//
//			MeshChunkData data = Generator
//					.<PointCloudChunkData>createGeneratorFor(e.getChunk().getQualityLevel())
//					.generateChunkData(e.getChunk());
//			
//			
//			if (requesting.containsKey(e.getChunk().getPosition())) {
//				eventBus.post(new ProcessorChunkLoadedEvent(new Chunk<>(
//						e.getChunk().getChunkId(),
//						data)));
//			}
//			
//			diskCache.putMemoryCache(id, data);
//			ioThread.submit(() -> diskCache.putDiskCache(id, data));
//		});
	}
	
}
