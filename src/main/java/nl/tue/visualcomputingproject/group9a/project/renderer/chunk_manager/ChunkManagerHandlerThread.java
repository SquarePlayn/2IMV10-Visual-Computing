package nl.tue.visualcomputingproject.group9a.project.renderer.chunk_manager;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import lombok.Setter;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.*;
import nl.tue.visualcomputingproject.group9a.project.common.event.ChartTextureAvailableEvent;
import nl.tue.visualcomputingproject.group9a.project.common.event.ProcessorChunkLoadedEvent;
import nl.tue.visualcomputingproject.group9a.project.common.event.RendererChunkStatusEvent;
import nl.tue.visualcomputingproject.group9a.project.common.util.GeneratorIterator;
import nl.tue.visualcomputingproject.group9a.project.renderer.engine.entities.Camera;
import org.joml.Vector2i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static nl.tue.visualcomputingproject.group9a.project.common.Settings.*;
import static nl.tue.visualcomputingproject.group9a.project.common.Settings.CHUNK_UNLOAD_DISTANCE;

@SuppressWarnings("UnstableApiUsage")
public class ChunkManagerHandlerThread
		extends Thread {
	

	/** The logger object of this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final Queue<ProcessorChunkLoadedEvent> chunkEventQueue = new ConcurrentLinkedQueue<>();
	private final Queue<ChartTextureAvailableEvent> textureEventQueue = new ConcurrentLinkedQueue<>();

	private final Map<ChunkPosition, Model> loaded = new HashMap<>();

	private final EventBus eventBus;

	private final Map<ChunkPosition, Model> toLoad = new ConcurrentHashMap<>();
	private final Queue<ChunkPosition> toLoadQueue = new ConcurrentLinkedQueue<>();
	private final Map<ChunkPosition, Model> toUpdate = new ConcurrentHashMap<>();
	private final Queue<ChunkPosition> toUpdateQueue = new ConcurrentLinkedQueue<>();
	private final Map<ChunkPosition, Model> toUnload = new ConcurrentHashMap<>();
	private final Queue<ChunkPosition> toUnloadQueue = new ConcurrentLinkedQueue<>();
	
	private final Lock lock = new ReentrantLock();
	private final Condition waitForEvent = lock.newCondition();
	private boolean newEvent;

	private Collection<Model> newLoad = new ArrayList<>();
	private Collection<ChunkPosition> newUnload = new ArrayList<>();
	
	private Vector2i curPos = null;
	private boolean posUpdate = false;
	
	
	public ChunkManagerHandlerThread(
			EventBus eventBus) {
		super("chunk-manager-handler-thread");
		this.eventBus = eventBus;
		eventBus.register(this);
		this.setDaemon(true);
	}
	
	public Iterator<Model> toLoad() {
		return safeRemoveIterator(toLoad, toLoadQueue);
	}

	public Iterator<Model> toUpdate() {
		return safeRemoveIterator(toUpdate, toUpdateQueue);
	}

	public Iterator<Model> toUnload() {
		return safeRemoveIterator(toUnload, toUnloadQueue);
	}
	
	private Iterator<Model> safeRemoveIterator(Map<ChunkPosition, Model> data, Queue<ChunkPosition> queue) {
		return new GeneratorIterator<Model>() {
			private Model model = null;
			@Override
			protected Model generateNext() {
				ChunkPosition cp = queue.poll();
				if (cp == null) {
					done();
					return null;
				}
				data.computeIfPresent(cp, (cp2, mod) -> {
					model = mod.extract();
					return null;
				});
				if (model == null) {
					done();
					return null;
				}
				return model;
			}
		};
	}
	
	@Override
	public void run() {
		//noinspection InfiniteLoopStatement
		while (true) {
			try {
				Vector2i curPos;
				boolean posUpdate;
				lock.lock();
				try {
					if (!newEvent) {
						waitForEvent.await();
						newEvent = false;
					}
					curPos = this.curPos;
					posUpdate = this.posUpdate;
				} finally {
					lock.unlock();
				}
				
				if (curPos != null) {
					if (posUpdate) {
						updateState(curPos);
						sendUpdate();
					}
					receiveEvents();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void updateState(Vector2i curPos) {
		// Check for chunks to load.
		getChunksInRadius(curPos, Settings.CHUNK_LOAD_DISTANCE).forEachRemaining((cp) -> {
			if (loaded.containsKey(cp)) return; // Already loaded, ignore.
			Model model = toUnload.remove(cp);
			if (model != null) {
				toUnloadQueue.remove(cp);
				// Scheduled for removal, but not yet processed by the render thread.
				loaded.put(cp, model);
				if (model.getQuality().getOrder() < QualityLevel.HALF_BY_HALF.getOrder() ||
						model.hasImage()) {
					// Request might have been canceled along the way, so request them if needed.
					newLoad.add(model);
				}
				
			} else {
				// Not loaded, so request.
				Model newModel = new Model(cp);
				toLoad.put(newModel.getPosition(), newModel);
				toLoadQueue.add(newModel.getPosition());
				newLoad.add(newModel);
			}
		});

		final int chunkUnloadRangeX = (int) Math.ceil(CHUNK_UNLOAD_DISTANCE / CHUNK_WIDTH);
		final int chunkUnloadRangeY = (int) Math.ceil(CHUNK_UNLOAD_DISTANCE / CHUNK_HEIGHT);
		newUnload = loaded.keySet().stream().filter((cp) -> {
			int chunkIX = (int) Math.floor(cp.getX() / CHUNK_WIDTH);
			int chunkIY = (int) Math.floor(cp.getY() / CHUNK_HEIGHT);
			return Math.abs(chunkIX - curPos.x) >= chunkUnloadRangeX ||
					Math.abs(chunkIY - curPos.y) >= chunkUnloadRangeY;
		}).collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Find all chunk positions in a certain radius around the camera.
	 */
	private Iterator<ChunkPosition> getChunksInRadius(final Vector2i curPos,  final double radius) {
		return new GeneratorIterator<ChunkPosition>() {
			private final int chunkRangeX = (int) Math.ceil(radius / CHUNK_WIDTH);
			private final int chunkRangeY = (int) Math.ceil(radius / CHUNK_HEIGHT);
			
			private int cdx = 0;
			private int cdy = 0;
			private int cr = 0;

			@Override
			protected ChunkPosition generateNext() {
				int maxDx = Math.min(chunkRangeX, cr);
				int maxDy = Math.min(chunkRangeY, cr);

				if (cdx > -maxDx && cdx < maxDx && cdy > -maxDy && cdy < maxDy) {
					cdy = maxDy;
				}
				if (cdy > maxDy) {
					cdx++;
					cdy = -maxDy;
				}
				if (cdx > maxDx) {
					cr++;
					if (cr > Math.max(chunkRangeX, chunkRangeY)) {
						done();
						return null;
					}
					maxDx = Math.min(chunkRangeX, cr);
					maxDy = Math.min(chunkRangeY, cr);
					cdx = -maxDx;
					cdy = -maxDy;
				}

				double x = (curPos.x + cdx) * CHUNK_WIDTH;
				double y = (curPos.y + cdy) * CHUNK_WIDTH;

				cdy++;

				return new ChunkPosition(x, y, CHUNK_WIDTH, CHUNK_HEIGHT);
			}
		};
	}

	private void sendUpdate() {
		for (Model cp : newLoad) {
			loaded.put(cp.getPosition(), cp);
		}
		for (ChunkPosition cp : newUnload) {
			Model model = loaded.remove(cp);
			if (toLoad.remove(cp) != null) {
				toLoadQueue.remove(cp);
				// If the chunk was added but not yet processed by the render thread,
				// then we don't have to notify the render thread that it was removed.
				continue;
			}
			if (toUpdate.remove(cp) != null) toUpdateQueue.remove(cp);
			toUnload.put(cp, model);
			toUnloadQueue.add(cp);
		}
		if (!newLoad.isEmpty() || !newUnload.isEmpty()) {
			eventBus.post(new RendererChunkStatusEvent(
					newLoad.stream().map(Model::getPosition).collect(Collectors.toList()),
					newUnload
			));
			newLoad = new ArrayList<>();
			newUnload = new ArrayList<>();
		}
	}

	private void receiveEvents() {
		List<ChunkPosition> cancelRequests = new ArrayList<>();
		while (!chunkEventQueue.isEmpty()) {
			final ProcessorChunkLoadedEvent e = chunkEventQueue.poll();
			final Model model = loaded.get(e.getChunk().getPosition());
			if (model == null) {
				// Ignore event if it is not requested anymore.
				continue;
			}
			if (model.getQuality() == QualityLevel.getBest() &&
					model.getQuality() != e.getChunk().getQualityLevel()) {
				// If a the best quality is already available, send an update
				// to not update it further. Ignore it afterwards.
				cancelRequests.add(e.getChunk().getPosition());
				continue;
			}
			if (e.getChunk().getQualityLevel().getOrder() < model.getQuality().getOrder()) {
				// If a better quality is already available, simply ignore it.
				continue;
			}
			
			// Request the renderer to load the chunk and update the internal state.
			updateModel(model, () -> model.setData(e.getChunk().getData(), e.getChunk().getQualityLevel()));
		}
		
		while (!textureEventQueue.isEmpty()) {
			final ChartTextureAvailableEvent e = textureEventQueue.poll();
			final Model model = loaded.get(e.getPosition());
			if (model == null || model.hasImage()) {
				// If the chunk is not loaded or already has an image, ignore it.
				continue;
			}
			updateModel(model, () -> model.setImage(e.getImage(), e.getWidth(), e.getHeight()));
		}
		
		if (!cancelRequests.isEmpty()) {
			eventBus.post(new RendererChunkStatusEvent(
					new ArrayList<>(),
					cancelRequests
			));
		}
	}
	
	private void updateModel(final Model model, final Runnable r) {
		loaded.put(model.getPosition(), model);
		if (!model.hasData() && !model.hasImage()) {
			r.run();
			toLoad.put(model.getPosition(), model);
			toLoadQueue.add(model.getPosition());
		} else {
			toLoad.compute(model.getPosition(), (pos, mod) -> {
				if (mod == null) {
					toUpdate.compute(model.getPosition(), (pos2, mod2) -> {
						r.run();
						if (mod2 == null) {
							toUpdateQueue.add(model.getPosition());
						}
						return model;
					});
					return null;
				} else {
					r.run();
					return model;
				}
			});
		}
	}

	private void signalEvent() {
		lock.lock();
		try {
			newEvent = true;
			waitForEvent.signal();
		} finally {
			lock.unlock();
		}
	}

	public void updateCamera(Camera camera) {
		Vector2i newCurPos = ChunkManager.getChunkIndices(
				camera.getPosition().x,
				camera.getPosition().z
		);
		if (!Objects.equals(curPos, newCurPos)) {
			curPos = newCurPos;
			posUpdate = true;
			signalEvent();
		}
	}

	/**
	 * Receive events and put them in the event queue
	 *
	 * @param event Received event
	 */
	@Subscribe
	public void receiveEvent(ProcessorChunkLoadedEvent event) {
		LOGGER.info("Chunk load event received, added to queue");
		chunkEventQueue.add(event);
		signalEvent();
	}

	@Subscribe
	public void receiveTextureEvent(ChartTextureAvailableEvent event) {
		LOGGER.info("Texture available event received, added to queue");
		textureEventQueue.add(event);
		signalEvent();
	}
	
}
