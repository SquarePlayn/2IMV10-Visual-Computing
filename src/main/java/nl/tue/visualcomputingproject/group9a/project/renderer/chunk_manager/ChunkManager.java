package nl.tue.visualcomputingproject.group9a.project.renderer.chunk_manager;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;
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

import java.lang.invoke.MethodHandles;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static nl.tue.visualcomputingproject.group9a.project.common.Settings.*;

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

	private final EventBus eventBus;

	private final Set<ChunkId> loaded = new HashSet<>();
	private final Set<ChunkPosition> pending = new HashSet<>();
	private final Set<ChunkPosition> request = new HashSet<>();
	private final Set<ChunkPosition> removed = new HashSet<>();

	private final Set<RawModel> models = new HashSet<>();

	private final HashMap<ChunkPosition, RawModel> positionModel = new HashMap<>();
	private final HashMap<ChunkPosition, QualityLevel> positionQuality = new HashMap<>();
	private final HashMap<ChunkPosition, MeshChunkData> positionData = new HashMap<>();

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
		Vector2i currentChunkIndex = getChunkIndices(camera.getPosition().x, camera.getPosition().z);
		int chunkUnloadRangeX = (int) Math.ceil(CHUNK_UNLOAD_DISTANCE / CHUNK_WIDTH);
		int chunkUnloadRangeY = (int) Math.ceil(CHUNK_UNLOAD_DISTANCE / CHUNK_HEIGHT);
		Collection<ChunkPosition> toUnload = positionModel.keySet().stream()
				.filter(cp -> {
					int chunkIX = (int) Math.floor(cp.getX() / CHUNK_WIDTH);
					int chunkIY = (int) Math.floor(cp.getY() / CHUNK_WIDTH);
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
		positionData.remove(cp);
		Loader.unloadModel(model);
		removed.add(cp);
	}

	/**
	 * Find all chunk positions in a certain radius around the camera
	 */
	private Collection<ChunkPosition> getChunksInRadius(Camera camera, double radius) {
		Collection<ChunkPosition> chunks = new ArrayList<>();

		// Go over the grid, adding each chunk
		Vector2i currentChunkIndex = getChunkIndices(camera.getPosition().x, camera.getPosition().z);
		int chunkRangeX = (int) Math.ceil(radius / CHUNK_WIDTH);
		int chunkRangeY = (int) Math.ceil(radius / CHUNK_HEIGHT);
		for (int cdx = -chunkRangeX; cdx <= chunkRangeX; cdx++) {
			int cx = currentChunkIndex.x + cdx;
			double x = cx * CHUNK_WIDTH;
			for (int cdy = -chunkRangeY; cdy <= chunkRangeY; cdy++) {
				int cy = currentChunkIndex.y + cdy;
				double y = cy * Settings.CHUNK_HEIGHT;
				chunks.add(new ChunkPosition(x, y, CHUNK_WIDTH, CHUNK_HEIGHT));
			}
		}

		return chunks;
	}

	private Vector2i getChunkIndices(float x, float z) {
		return new Vector2i(
				(int) Math.floor(x / CHUNK_WIDTH),
				(int) Math.floor(z / Settings.CHUNK_HEIGHT)
		);
	}

	private ChunkPosition getChunkPosition(float x, float z) {
		Vector2i chunkIndices = getChunkIndices(x, z);
		return new ChunkPosition(
				chunkIndices.x * CHUNK_WIDTH,
				chunkIndices.y * CHUNK_HEIGHT,
				CHUNK_WIDTH,
				CHUNK_HEIGHT
		);
	}

	/**
	 * Get the height at some position on the map, or null if unknown
	 */
	public Optional<Float> getHeight(float x, float z) {
		ChunkPosition chunk = getChunkPosition(x, z);

		if (!positionData.containsKey(chunk)) {
			return Optional.empty();
		}

		FloatBuffer data = positionData.get(chunk).getVertexBuffer();

		double space = positionQuality.get(chunk) == QualityLevel.FIVE_BY_FIVE ? 5 : 0.5;
		int numX = (int) (Math.ceil(CHUNK_WIDTH / space) + 1);
		int numZ = (int) (Math.ceil(CHUNK_HEIGHT / space) + 1);
		float[][] heights = new float[numX][numZ];
		for (int i = 0; i < data.remaining(); i += 6) {
			int ix = (int) Math.floor(data.get(i) / space);
			int iz = (int) Math.floor(data.get(i + 2) / space);
			if (ix > 0 && ix < numX && iz >= 0 && iz < numZ) {
				heights[ix][iz] = data.get(i + 1);
			}
		}

		Vector2f offset = positionData.get(chunk).getOffset();
		int ix = (int) Math.floor((x - offset.x)  / space);
		int iz = (int) Math.floor((z - offset.y)  / space);

		if (ix >= 0 && ix < numX && iz >= 0 && iz < numZ) {
			System.out.println("Position " + ix + " " + iz + " = " + heights[ix][iz]);
			return Optional.of(heights[ix][iz]);
		} else {
			System.out.println("Position " + ix + " " + iz + " unknown");
			return Optional.empty();
		}

	}

	/**
	 * Process chunk loaded events
	 */
	private void handleEvents() {
		while (!eventQueue.isEmpty()) {
			// Extract event
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
				// Create the model
				RawModel newModel = Loader.loadToVAO(
						chunk.getData().getVertexBuffer(),
						chunk.getData().getMeshBuffer(),
						offset
				);

				// Remove current chunk model if present
				if (positionModel.containsKey(chunkPosition)) {
					RawModel oldModel = positionModel.get(chunkPosition);
					models.remove(positionModel.get(chunkPosition));
					Loader.unloadModel(oldModel);
				}

				// Add the new model
				models.add(newModel);
				positionModel.put(chunkPosition, newModel);
				positionQuality.put(chunkPosition, qualityLevel);
				positionData.put(chunkPosition, chunk.getData());
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

}
