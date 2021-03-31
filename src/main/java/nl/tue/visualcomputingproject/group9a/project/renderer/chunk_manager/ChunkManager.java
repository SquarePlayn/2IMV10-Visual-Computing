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
			
		} else if (model.hasNewImage()) {
			if (raw == null) {
				ChunkPosition cp = model.getPosition();
				Vector2f offset = new Vector2f((float) cp.getX(), (float) cp.getY());
				posData = Loader.createPlaneVertices();
				raw = Loader.loadToVAO(
						posData,
						Loader.createPlaneMesh(),
						offset,
						texId
				);
				
			} else {
				raw.setTexId(texId);
			}
			
		} else {
			return raw;
		}
		
		// Update the position data as well.
		positionData.put(model.getPosition(), posData);
		
		return raw;
	}

	public Iterable<RawModel> getModels() {
		return models.values();
	}
	
	private static boolean isLeftOf(float p1, float p2, float t1, float t2) {
		float v = p1*t2 - p2*t1;
		return v <= 0;
	}
	
	public Iterable<RawModel> getVisibleModels(Camera camera) {
		if (camera.getPitch() < -55) return getModels();
		final Vector3f forward = camera.getForward();
		if (forward.x == 0 && forward.z == 0) return getModels();
		final Vector3f pos = camera.getPosition();

		return models.entrySet().stream().filter((entry) -> {
			ChunkPosition cp = entry.getKey();
			float x1 = (float) cp.getX() - pos.x;
			float x2 = x1 + (float) cp.getWidth();
			float y1 = -((float) cp.getY() - pos.z);
			float y2 = y1 + (float) cp.getHeight();

			return isLeftOf(forward.z, forward.x, x1, y1) ||
					isLeftOf(forward.z, forward.x, x1, y2) ||
					isLeftOf(forward.z, forward.x, x2, y1) ||
					isLeftOf(forward.z, forward.x, x2, y2);
		}).map(Map.Entry::getValue)
				.collect(Collectors.toList());
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
