package nl.tue.visualcomputingproject.group9a.project.common.event;

import lombok.EqualsAndHashCode;
import lombok.Value;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;

import java.util.Collection;

/**
 * Sent by the render to inform the other modules which chunks need to be loaded and unloaded.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class RendererChunkStatusEvent extends AbstractEvent {
	Collection<ChunkPosition> loadedChunks, pendingChunks, newChunks, unloadedChunks;
}
