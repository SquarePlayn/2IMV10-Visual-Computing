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
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

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

	private double prevUpdateTime = 0;

	private static final double updateInterval = 10; // seconds TODO Move

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

		if (time - prevUpdateTime >= updateInterval) {
			sendUpdate();
			prevUpdateTime = time;
		}
	}

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

	private void updateStatus(Camera camera, double time) {
		Vector3f position = camera.getPosition();

		// Chunk index of the current chunk we're in
		int chunkX = (int) Math.floor(position.x / Settings.CHUNK_WIDTH);
		int chunkY = (int) Math.floor(position.z / Settings.CHUNK_HEIGHT);

		// TODO Unload chunks that are too far away

		double loadDistance = 1000; // Distance to load chunk in. TODO Move
		int chunkRangeX = (int) Math.ceil(loadDistance / Settings.CHUNK_WIDTH);
		int chunkRangeY = (int) Math.ceil(loadDistance / Settings.CHUNK_HEIGHT);

		for (int cdx = -chunkRangeX; cdx <= chunkRangeX; cdx++) {
			int cx = chunkX + cdx;
			double x = cx * Settings.CHUNK_WIDTH;
			for (int cdy = -chunkRangeY; cdy <= chunkRangeY; cdy++) {
				int cy = chunkY + cdy;
				double y = cy * Settings.CHUNK_HEIGHT;
				ChunkPosition chunkPosition = new ChunkPosition(x, y, Settings.CHUNK_WIDTH, Settings.CHUNK_HEIGHT);
				if (!(request.contains(chunkPosition) ||
						pending.contains(chunkPosition) ||
						positionModel.containsKey(chunkPosition))) {
					// Chunk not requested / loaded yet, so request it
					LOGGER.info("Entering chunk " + chunkPosition);
					request.add(chunkPosition);
				}
			}
		}

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

			// Notify that the chunk was received
			loaded.add(chunkId);

			// Only add the new chunk if it's not loaded yet or better than the currently loaded version
			if (!positionQuality.containsKey(chunkPosition) ||
					positionQuality.get(chunkPosition).getOrder() < qualityLevel.getOrder()) {
				// Create the model
				RawModel newModel = Loader.loadToVAO(
						chunk.getData().getVertexBuffer(),
						chunk.getData().getMeshBuffer()
				);

				// Remove current chunk model if present
				if (positionModel.containsKey(chunkPosition)) {
					models.remove(positionModel.get(chunkPosition));
				}

				// Add the new model
				models.add(newModel);
				positionModel.put(chunkPosition, newModel);
				positionQuality.put(chunkPosition, qualityLevel);
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
