package nl.tue.visualcomputingproject.group9a.project.renderer.chunk_manager;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.TextureType;
import nl.tue.visualcomputingproject.group9a.project.common.cache.MemoryStore;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;
import nl.tue.visualcomputingproject.group9a.project.common.event.AbstractEvent;
import nl.tue.visualcomputingproject.group9a.project.common.event.ChartTextureAvailableEvent;
import nl.tue.visualcomputingproject.group9a.project.common.event.ProcessorChunkLoadedEvent;
import nl.tue.visualcomputingproject.group9a.project.common.event.RendererChunkStatusEvent;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Camera;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.Loader;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.RawModel;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.utils.Maths;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static nl.tue.visualcomputingproject.group9a.project.common.Settings.*;

@SuppressWarnings("UnstableApiUsage")
public class ChunkManager {

	/** The logger object of this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final Map<ChunkPosition, RawModel> models = new HashMap<>();
	private final Map<ChunkPosition, FloatBuffer> positionData = new HashMap<>();
	private final ChunkManagerHandlerThread handlerThread;
	
	public ChunkManager(EventBus eventBus) {
		handlerThread = new ChunkManagerHandlerThread(eventBus);
		handlerThread.start();
	}
	
	/**
	 * Update the managed chunks, called once per frame
	 */
	public void update(Camera camera) {
		handlerThread.updateCamera(camera);
		
		// Unload and remove all unloaded chunks.
		for (Iterator<Model> it = handlerThread.toUnload(); it.hasNext(); ) {
			ChunkPosition cp = it.next().getPosition();
			Loader.unloadModel(models.remove(cp), true);
			positionData.remove(cp);
		}

		// Add all new chunks.
		for (Iterator<Model> it = handlerThread.toLoad(); it.hasNext(); ) {
			Model model = it.next();
			models.put(model.getPosition(), loadModel(model, null));
		}

		// Update all existing chunks.
		for (Iterator<Model> it = handlerThread.toUpdate(); it.hasNext(); ) {
			Model model = it.next();
			models.compute(model.getPosition(), (cp, raw) -> loadModel(model, raw));
		}
	}
	
	private RawModel loadModel(Model model, RawModel raw) {
		int texId = -1;
		if (model.hasNewImage()) {
			// Only unload the existing image if a new one is available.
			if (raw != null) Loader.unloadTexture(raw);
			texId = Loader.loadTexture(model.getImage(), model.getWidth(), model.getHeight());
		} else if (raw != null) {
			texId = raw.getTexId();
		}

		FloatBuffer posData = null;
		if (model.hasNewData()) {
			raw = Loader.loadToVAO(
					posData = model.getData().getVertexBuffer(),
					model.getData().getMeshBuffer(),
					model.getData().getOffset(),
					texId
			);
			
		} else if (raw == null) {
			ChunkPosition cp = model.getPosition();
			Vector2f offset = new Vector2f((float) cp.getX(), (float) cp.getY());
			posData = Loader.createPlaneVertices();
			raw = Loader.loadToVAO(
				posData,
				Loader.createPlaneMesh(),
				offset,
				texId
			);
			
		}
		if (model.hasNewImage()) {
				raw.setTexId(texId);
		}
		
		if (posData == null) {
			return raw;
		}
		
		// Update the position data as well.
		positionData.put(model.getPosition(), posData);
		
		return raw;
	}

	public Iterable<RawModel> getModels() {
		return models.values();
	}
	
	/**
	 * Get the height at some position on the map, or null if unknown
	 */
	public Optional<Float> getHeight(float x, float z) {
		ChunkPosition cp = getChunkPosition(x, z);
		FloatBuffer data = positionData.get(cp);
		if (data == null) {
			return Optional.empty();
		}

		if (!cp.contains(x, z)) {
			return Optional.empty();
		}
		float height = 0f;
		float distSquared = Float.POSITIVE_INFINITY;
		for (int i = 0; i + 2 < data.remaining(); i += 6) {
			float xPos = x - (float) cp.getX() - data.get(i);
			float h = data.get(i+1);
			float zPos = z - (float) cp.getY() - data.get(i+2);
			float d2 = xPos*xPos + zPos*zPos;
			if (d2 < distSquared) {
				height = h;
				distSquared = d2;
			}
		}
		
		if (distSquared == Float.POSITIVE_INFINITY) {
			return Optional.empty();
		} else {
			return Optional.of(height);
		}
	}

	static ChunkPosition getChunkPosition(float x, float z) {
		Vector2i chunkIndices = getChunkIndices(x, z);
		return new ChunkPosition(
				chunkIndices.x * CHUNK_WIDTH,
				chunkIndices.y * CHUNK_HEIGHT,
				CHUNK_WIDTH,
				CHUNK_HEIGHT
		);
	}

	static Vector2i getChunkIndices(float x, float z) {
		return new Vector2i(
				(int) Math.floor(x / CHUNK_WIDTH),
				(int) Math.floor(z / CHUNK_HEIGHT)
		);
	}

}

//class ChunkManagerOld {
//	/**
//	 * The logger object of this class.
//	 */
//	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
//	
//	/** Event queue received events are temporarily inserted in, to be extracted in this thread. */
//	private final Queue<ProcessorChunkLoadedEvent> eventQueue = new ConcurrentLinkedQueue<>();
//	private final Queue<ChartTextureAvailableEvent> textureEventQueue = new ConcurrentLinkedQueue<>();
//	private final Queue<RawModel> unloadQueue = new ConcurrentLinkedQueue<>();
//
//	private final EventBus eventBus;
//
//	private final Set<ChunkId> loaded = ConcurrentHashMap.newKeySet();
//	private final Set<ChunkPosition> pending = ConcurrentHashMap.newKeySet();
//	private final Set<ChunkPosition> request = ConcurrentHashMap.newKeySet();
//	private final Set<ChunkPosition> removed = ConcurrentHashMap.newKeySet();
//
//	private final Set<RawModel> models = ConcurrentHashMap.newKeySet();
//
//	private final Map<ChunkPosition, RawModel> positionModel = new ConcurrentHashMap<>();
//	private final Map<ChunkPosition, QualityLevel> positionQuality = new ConcurrentHashMap<>();
//	private final Map<ChunkPosition, ChartTextureAvailableEvent> pendingTextures = new ConcurrentHashMap<>();
//	private final Map<ChunkPosition, MeshChunkData> positionData = new ConcurrentHashMap<>();
//	
//	private final Lock lock = new ReentrantLock();
//	private final Condition waitForUpdate = lock.newCondition();
//	private final AtomicBoolean doUpdate = new AtomicBoolean(false);
//	private final AtomicBoolean sendUpdate = new AtomicBoolean(false);
//
//	private volatile Camera camera;
//
//	/**
//	 * The last time in seconds at which a chunk update was sent.
//	 */
//	private double prevUpdateTime = 0;
//
//	public ChunkManagerOld(EventBus eventBus) {
//		this.eventBus = eventBus;
//		eventBus.register(this);
//		Thread updateThread = new Thread(() -> {
//			lock.lock();
//			try {
//				while (true) {
//					lock.lock();
//					try {
//						updateCycle();
//
//					} catch (Exception e) {
//						LOGGER.error("Caught exception in update thread:");
//						LOGGER.error(Arrays.toString(e.getStackTrace()));
//						e.printStackTrace();
//					}
//				}
//
//			} finally {
//				lock.unlock();
//			}
//		}, "Render-update-thread");
//		updateThread.setDaemon(true);
//		updateThread.start();
//	}
//	
//	private void updateCycle()
//			throws InterruptedException {
//		final Camera camera = this.camera;
//		boolean updated = false;
//		if (doUpdate.getAndSet(false)) {
//			updateStatus(camera);
//			updated = true;
//		}
//		if (sendUpdate.getAndSet(false)) {
//			sendUpdate();
//			updated = true;
//		}
//		if (!updated) {
//			waitForUpdate.await();
//		}
//	}
//
//
//	/**
//	 * Update the managed chunks, called once per frame
//	 */
//	public void update(Camera camera) {
//		// Fetch time in seconds
//		double time = System.currentTimeMillis() / 1_000.0;
//
//		int actionCounter = 10;
//		while (actionCounter-- > 0 && !unloadQueue.isEmpty()) {
//			RawModel model = unloadQueue.poll();
//			Loader.unloadModel(model);
//			if (models.contains(model)) {
//				System.out.println();
//			}
//		}
//		
//		this.camera = new Camera(camera);
//		doUpdate.set(true);
//
//		handleEvents();
//
//		if (time - prevUpdateTime >= Settings.CHUNK_UPDATE_INTERVAL) {
//			sendUpdate.set(true);
//			prevUpdateTime = time;
//		}
//		if (lock.tryLock()) {
//			try {
//				waitForUpdate.signal();
//			} finally {
//				lock.unlock();
//			}
//		}
//	}
//
//	/**
//	 * Send a Renderer chunk status event, informing other modules about which chunks are loaded, pending,
//	 * newly requested and recently removed.
//	 */
//	private void sendUpdate() {
//		for (ChunkPosition pos : request) {
//			LOGGER.info("Requesting " + pos);
//		}
//
//		// Send an update
//		eventBus.post(new RendererChunkStatusEvent(
////				new ArrayList<>(loaded),
////				new ArrayList<>(pending),
//				new ArrayList<>(request),
//				new ArrayList<>(removed)
//		));
//
//		// Mark the to-request chunks as already requested / pending
//		pending.addAll(request);
//		request.clear();
//
//		// Clear the removed positions, as they are now sent
//		removed.clear();
//
//		// Clear the loaded chunk IDs, as they are now sent
//		loaded.clear();
//	}
//
//	/**
//	 * Update which chunks should be (un)loaded
//	 */
//	private void updateStatus(Camera camera) {
//		// Check for chunks to load
//		Collection<ChunkPosition> loadChunks = getChunksInRadius(camera, Settings.CHUNK_LOAD_DISTANCE);
//		Collection<ChunkPosition> toLoad = loadChunks.stream().filter(cp -> !(
//				request.contains(cp) || pending.contains(cp) || positionModel.containsKey(cp)
//		)).collect(Collectors.toCollection(ArrayList::new));
//		toLoad.forEach(cp -> LOGGER.info("Entering chunk " + cp));
//		request.addAll(toLoad);
//
//		// Check for chunks to unload
//		Vector2i currentChunkIndex = getChunkIndices(camera.getPosition().x, camera.getPosition().z);
//		int chunkUnloadRangeX = (int) Math.ceil(CHUNK_UNLOAD_DISTANCE / CHUNK_WIDTH);
//		int chunkUnloadRangeY = (int) Math.ceil(CHUNK_UNLOAD_DISTANCE / CHUNK_HEIGHT);
//		Collection<ChunkPosition> toUnload = positionModel.keySet().stream()
//				.filter(cp -> {
//					int chunkIX = (int) Math.floor(cp.getX() / CHUNK_WIDTH);
//					int chunkIY = (int) Math.floor(cp.getY() / CHUNK_HEIGHT);
//					return Math.abs(chunkIX - currentChunkIndex.x) >= chunkUnloadRangeX ||
//							Math.abs(chunkIY - currentChunkIndex.y) >= chunkUnloadRangeY;
//				}).collect(Collectors.toCollection(ArrayList::new));
//		toUnload.forEach((cp) -> {
//			RawModel model = positionModel.get(cp);
//			if (!unloadQueue.contains(model)) {
//				unloadQueue.add(model);
//				if (!models.remove(model)) {
//					System.out.println();
//				}
//				positionModel.remove(cp);
//				positionQuality.remove(cp);
//				positionData.remove(cp);
//				removed.add(cp);
//			}
//		});
//	}
//
//	/**
//	 * Find all chunk positions in a certain radius around the camera
//	 */
//	private Collection<ChunkPosition> getChunksInRadius(Camera camera, double radius) {
//		Collection<ChunkPosition> chunks = new ArrayList<>();
//
//		// Go over the grid, adding each chunk
//		Vector2i currentChunkIndex = getChunkIndices(camera.getPosition().x, camera.getPosition().z);
//		int chunkRangeX = (int) Math.ceil(radius / CHUNK_WIDTH);
//		int chunkRangeY = (int) Math.ceil(radius / CHUNK_HEIGHT);
//		for (int cdx = -chunkRangeX; cdx <= chunkRangeX; cdx++) {
//			int cx = currentChunkIndex.x + cdx;
//			double x = cx * CHUNK_WIDTH;
//			for (int cdy = -chunkRangeY; cdy <= chunkRangeY; cdy++) {
//				int cy = currentChunkIndex.y + cdy;
//				double y = cy * Settings.CHUNK_HEIGHT;
//				chunks.add(new ChunkPosition(x, y, CHUNK_WIDTH, CHUNK_HEIGHT));
//			}
//		}
//
//		return chunks;
//	}
//
//	private Vector2i getChunkIndices(float x, float z) {
//		return new Vector2i(
//				(int) Math.floor(x / CHUNK_WIDTH),
//				(int) Math.floor(z / Settings.CHUNK_HEIGHT)
//		);
//	}
//
//	private ChunkPosition getChunkPosition(float x, float z) {
//		Vector2i chunkIndices = getChunkIndices(x, z);
//		return new ChunkPosition(
//				chunkIndices.x * CHUNK_WIDTH,
//				chunkIndices.y * CHUNK_HEIGHT,
//				CHUNK_WIDTH,
//				CHUNK_HEIGHT
//		);
//	}
//
//	/**
//	 * Get the height at some position on the map, or null if unknown
//	 */
//	public Optional<Float> getHeight(float x, float z) {
//		ChunkPosition chunk = getChunkPosition(x, z);
//
//		if (!positionData.containsKey(chunk)) {
//			return Optional.empty();
//		}
//
//		FloatBuffer data = positionData.get(chunk).getVertexBuffer();
//
//		double space = positionQuality.get(chunk) == QualityLevel.FIVE_BY_FIVE ? 5 : 0.5;
//		int numX = (int) (Math.ceil(CHUNK_WIDTH / space) + 1);
//		int numZ = (int) (Math.ceil(CHUNK_HEIGHT / space) + 1);
//		float[][] heights = new float[numX][numZ];
//		for (int i = 0; i + 2 < data.remaining(); i += 6) {
//			int ix = (int) Math.floor(data.get(i) / space);
//			int iz = (int) Math.floor(data.get(i + 2) / space);
//			if (ix > 0 && ix < numX && iz >= 0 && iz < numZ) {
//				heights[ix][iz] = data.get(i + 1);
//			}
//		}
//
//		Vector2f offset = positionData.get(chunk).getOffset();
//		int ix = (int) Math.floor((x - offset.x) / space);
//		int iz = (int) Math.floor((z - offset.y) / space);
//
//		if (ix >= 0 && ix < numX && iz >= 0 && iz < numZ) {
//			return Optional.of(heights[ix][iz]);
//		} else {
//			return Optional.empty();
//		}
//	}
//
//	/**
//	 * Process chunk loaded events
//	 */
//	private void handleEvents() {
//		int actionCounter = 20;
//		while (actionCounter-- > 0 && !eventQueue.isEmpty()) {
//			// Extract event
//			ProcessorChunkLoadedEvent event = eventQueue.poll();
//			Chunk<MeshChunkId, MeshChunkData> chunk = event.getChunk();
//			ChunkId chunkId = chunk.getChunkId();
//			ChunkPosition chunkPosition = chunk.getPosition();
//			QualityLevel qualityLevel = chunk.getQualityLevel();
//			Vector2f offset = chunk.getData().getOffset();
//
//			// Notify that the chunk was received
//			loaded.add(chunkId);
//			pending.remove(chunkId.getPosition());
//
//			// Only add the new chunk if it's not loaded yet or better than the currently loaded version
//			if (!positionQuality.containsKey(chunkPosition) ||
//					positionQuality.get(chunkPosition).getOrder() < qualityLevel.getOrder()) {
//				int texId = -1;
//
//				// Remove current chunk model if present
//				if (positionModel.containsKey(chunkPosition)) {
//					RawModel oldModel = positionModel.get(chunkPosition);
//					texId = oldModel.getTexId();
//					if (!models.remove(oldModel)) {
//						System.out.println();
//					}
//					Loader.unloadModel(oldModel, false);
//				}
//
//				if (texId < 0 && pendingTextures.containsKey(chunkPosition)) {
//					ChartTextureAvailableEvent e = pendingTextures.remove(chunkPosition);
//					texId = Loader.loadTexture(e.getImage(), e.getWidth(), e.getHeight());
//				}
//
//				// Create the model
//				RawModel newModel = Loader.loadToVAO(
//						chunk.getData().getVertexBuffer(),
//						chunk.getData().getMeshBuffer(),
//						offset,
//						texId
//				);
//
//				// Add the new model
//				positionModel.put(chunkPosition, newModel);
//				positionQuality.put(chunkPosition, qualityLevel);
//				positionData.put(chunkPosition, chunk.getData());
//				if (!models.add(newModel)) {
//					System.out.println();
//				}
//			}
//		}
//
//		while (actionCounter-- > 0 && !textureEventQueue.isEmpty()) {
//			ChartTextureAvailableEvent event = textureEventQueue.poll();
//			if (event.getType() == TextureType.Aerial) {
//				RawModel model = positionModel.get(event.getPosition());
//				if (model == null) {
//					pendingTextures.put(event.getPosition(), event);
//				} else {
//					int texId = Loader.loadTexture(event.getImage(), event.getWidth(), event.getHeight());
//					model.setTexId(texId);
//				}
//			}
//		}
//	}
//
//	/**
//	 * Fetch the models that are currently to be rendered
//	 */
//	public Iterable<RawModel> getModels() {
//		return models;
//	}
//
//	/**
//	 * Receive events and put them in the event queue
//	 *
//	 * @param event Received event
//	 */
//	@Subscribe
//	public void receiveEvent(ProcessorChunkLoadedEvent event) {
//		LOGGER.info("Chunk load event received, added to queue");
//		eventQueue.add(event);
//	}
//
//	@Subscribe
//	public void receiveTextureEvent(ChartTextureAvailableEvent event) {
//		LOGGER.info("Texture available event received, added to queue");
//		textureEventQueue.add(event);
//	}
//
//}
