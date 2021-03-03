package nl.tue.visualcomputingproject.group9a.project.common.event;

import lombok.EqualsAndHashCode;
import lombok.Value;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.Chunk;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkId;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.MeshChunkData;

/**
 * Sent by the pre-processor to the renderer to load a new chunk.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class ProcessorChunkLoadedEvent extends AbstractEvent {
	Chunk<ChunkId, MeshChunkData> chunk;
}
