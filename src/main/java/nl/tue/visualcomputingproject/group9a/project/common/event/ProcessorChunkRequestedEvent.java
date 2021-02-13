package nl.tue.visualcomputingproject.group9a.project.common.event;

import lombok.EqualsAndHashCode;
import lombok.Value;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkId;

import java.util.Collection;

/**
 * Sent by the preprocessor to the chart module to request chunks be loaded from the map sheets.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class ProcessorChunkRequestedEvent extends AbstractEvent {
	Collection<ChunkId> newChunksRequested;
}
