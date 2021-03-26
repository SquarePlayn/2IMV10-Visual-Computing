package nl.tue.visualcomputingproject.group9a.project.renderer.chunk_manager;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.TextureType;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;
import nl.tue.visualcomputingproject.group9a.project.common.event.ChartTextureAvailableEvent;
import nl.tue.visualcomputingproject.group9a.project.common.event.ProcessorChunkLoadedEvent;
import nl.tue.visualcomputingproject.group9a.project.common.event.RendererChunkStatusEvent;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Camera;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.Loader;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.model.RawModel;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class ChunkManager {

	/**
	 * The logger object of this class.
	 */
	static private final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Event queue received events are temporarily inserted in, to be extracted in this thread
	 */
	final ConcurrentLinkedQueue<ProcessorChunkLoadedEvent> eventQueue = new ConcurrentLinkedQueue<>();
	final ConcurrentLinkedQueue<ChartTextureAvailableEvent> textureEventQueue = new ConcurrentLinkedQueue<>();

	private final EventBus eventBus;

	private final Set<ChunkId> loaded = new HashSet<>();
	private final Set<ChunkPosition> pending = new HashSet<>();
	private final Set<ChunkPosition> request = new HashSet<>();
	private final Set<ChunkPosition> removed = new HashSet<>();

	private final Set<RawModel> models = new HashSet<>();

	private final HashMap<ChunkPosition, RawModel> positionModel = new HashMap<>();
	private final HashMap<ChunkPosition, QualityLevel> positionQuality = new HashMap<>();
	private final HashMap<ChunkPosition, BufferedImage> pendingTextures = new HashMap<>();

	/**
	 * The last time in seconds at which a chunk update was sent.
	 */
	private double prevUpdateTime = 0;

	public ChunkManager(EventBus eventBus) {
		this.eventBus = eventBus;
		eventBus.register(this);
	}


	/**
	 * Update the managed chunks, called once per frame
	 */
	public void update(Camera camera) {
		// Fetch time in seconds
		double time = System.currentTimeMillis() / 1_000.0;

		handleEvents();

		updateStatus(camera, time);

		if (time - prevUpdateTime >= Settings.CHUNK_UPDATE_INTERVAL) {
			sendUpdate();
			prevUpdateTime = time;
		}
	}

	/**
	 * Send a Renderer chunk status event, informing other modules about which chunks are loaded, pending,
	 * newly requested and recently removed.
	 */
	private void sendUpdate() {
		for (ChunkPosition pos : request) {
			LOGGER.info("Requesting " + pos);
		}

		// Send an update
		eventBus.post(new RendererChunkStatusEvent(
				new ArrayList<>(loaded),
				new ArrayList<>(pending),
				new ArrayList<>(request),
				new ArrayList<>(removed)
		));

		// Mark the to-request chunks as already requested / pending
		pending.addAll(request);
		request.clear();

		// Clear the removed positions, as they are now sent
		removed.clear();

		// Clear the loaded chunk IDs, as they are now sent
		loaded.clear();
	}

	/**
	 * Update which chunks should be (un)loaded
	 */
	private void updateStatus(Camera camera, double time) {
		// Check for chunks to load
		Collection<ChunkPosition> loadChunks = getChunksInRadius(camera, Settings.CHUNK_LOAD_DISTANCE);
		Collection<ChunkPosition> toLoad = loadChunks.stream().filter(cp -> !(
				request.contains(cp) || pending.contains(cp) || positionModel.containsKey(cp)
		)).collect(Collectors.toCollection(ArrayList::new));
		toLoad.forEach(cp -> LOGGER.info("Entering chunk " + cp));
		request.addAll(toLoad);

		// Check for chunks to unload
		Vector2i currentChunkIndex = getChunkPosition(camera);
		int chunkUnloadRangeX = (int) Math.ceil(Settings.CHUNK_UNLOAD_DISTANCE / Settings.CHUNK_WIDTH);
		int chunkUnloadRangeY = (int) Math.ceil(Settings.CHUNK_UNLOAD_DISTANCE / Settings.CHUNK_HEIGHT);
		Collection<ChunkPosition> toUnload = positionModel.keySet().stream()
				.filter(cp -> {
					int chunkIX = (int) Math.floor(cp.getX() / Settings.CHUNK_WIDTH);
					int chunkIY = (int) Math.floor(cp.getY() / Settings.CHUNK_WIDTH);
					return Math.abs(chunkIX - currentChunkIndex.x) >= chunkUnloadRangeX ||
							Math.abs(chunkIY - currentChunkIndex.y) >= chunkUnloadRangeY;
				}).collect(Collectors.toCollection(ArrayList::new));
		toUnload.forEach(cp -> LOGGER.info("Leaving chunk " + cp));
		toUnload.forEach(this::unload);
	}

	/**
	 * Unload a chunk
	 */
	private void unload(ChunkPosition cp) {
		RawModel model = positionModel.get(cp);
		models.remove(model);
		positionModel.remove(cp);
		positionQuality.remove(cp);
		Loader.unloadModel(model);
		removed.add(cp);
	}

	/**
	 * Check the chunk index of the chunk the camera is in
	 */
	private Vector2i getChunkPosition(Camera camera) {
		Vector3f position = camera.getPosition();
		int chunkIX = (int) Math.floor(position.x / Settings.CHUNK_WIDTH);
		int chunkIY = (int) Math.floor(position.z / Settings.CHUNK_HEIGHT);
		return new Vector2i(chunkIX, chunkIY);
	}

	/**
	 * Find all chunk positions in a certain radius around the camera
	 */
	private Collection<ChunkPosition> getChunksInRadius(Camera camera, double radius) {
		Collection<ChunkPosition> chunks = new ArrayList<>();

		// Go over the grid, adding each chunk
		Vector2i currentChunkIndex = getChunkPosition(camera);
		int chunkRangeX = (int) Math.ceil(radius / Settings.CHUNK_WIDTH);
		int chunkRangeY = (int) Math.ceil(radius / Settings.CHUNK_HEIGHT);
		for (int cdx = -chunkRangeX; cdx <= chunkRangeX; cdx++) {
			int cx = currentChunkIndex.x + cdx;
			double x = cx * Settings.CHUNK_WIDTH;
			for (int cdy = -chunkRangeY; cdy <= chunkRangeY; cdy++) {
				int cy = currentChunkIndex.y + cdy;
				double y = cy * Settings.CHUNK_HEIGHT;
				chunks.add(new ChunkPosition(x, y, Settings.CHUNK_WIDTH, Settings.CHUNK_HEIGHT));
			}
		}

		return chunks;
	}

	/**
	 * Process chunk loaded events
	 */
	private void handleEvents() {
		while (!eventQueue.isEmpty()) {
			ProcessorChunkLoadedEvent event = eventQueue.poll();
			Chunk<MeshChunkId, MeshChunkData> chunk = event.getChunk();
			ChunkId chunkId = chunk.getChunkId();
			ChunkPosition chunkPosition = chunk.getPosition();
			QualityLevel qualityLevel = chunk.getQualityLevel();
			Vector2f offset = chunk.getData().getOffset();

			// Notify that the chunk was received
			loaded.add(chunkId);
			pending.remove(chunkId.getPosition());

			// Only add the new chunk if it's not loaded yet or better than the currently loaded version
			if (!positionQuality.containsKey(chunkPosition) ||
					positionQuality.get(chunkPosition).getOrder() < qualityLevel.getOrder()) {
				int texId = -1;
				
				// Remove current chunk model if present
				if (positionModel.containsKey(chunkPosition)) {
					RawModel oldModel = positionModel.get(chunkPosition);
					texId = oldModel.getTexId();
					models.remove(positionModel.get(chunkPosition));
					Loader.unloadModel(oldModel, false);
				}
				
				if (texId < 0 && pendingTextures.containsKey(chunkPosition)) {
					texId = Loader.loadTexture(pendingTextures.remove(chunkPosition));
				}

				// Create the model
				RawModel newModel = Loader.loadToVAO(
						chunk.getData().getVertexBuffer(),
						chunk.getData().getMeshBuffer(),
						offset,
						texId
				);


				// Add the new model
				models.add(newModel);
				positionModel.put(chunkPosition, newModel);
				positionQuality.put(chunkPosition, qualityLevel);
			}
		}
		
		while (!textureEventQueue.isEmpty()) {
			ChartTextureAvailableEvent event = textureEventQueue.poll();
			if (event.getType() == TextureType.Aerial) {
				if (positionModel.containsKey(event.getPosition())) {
					int texId = Loader.loadTexture(event.getImage());
					positionModel.get(event.getPosition()).setTexId(texId);
				} else {
					pendingTextures.put(event.getPosition(), event.getImage());
				}
			}
		}
	}

	/**
	 * Fetch the models that are currently to be rendered
	 */
	public Iterable<RawModel> getModels() {
		return models;
	}

	/**
	 * Receive events and put them in the event queue
	 *
	 * @param event Received event
	 */
	@Subscribe
	public void receiveEvent(ProcessorChunkLoadedEvent event) {
		LOGGER.info("Chunk load event received, added to queue");
		eventQueue.add(event);
	}
	
	@Subscribe
	public void receiveTextureEvent(ChartTextureAvailableEvent event) {
		LOGGER.info("Texture available event received, added to queue");
		textureEventQueue.add(event);
	}

}
