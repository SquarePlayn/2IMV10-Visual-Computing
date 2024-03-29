package nl.tue.visualcomputingproject.group9a.project.common.event;

import lombok.EqualsAndHashCode;
import lombok.Value;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.Chunk;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.MeshChunkData;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.MeshChunkId;

/**
 * Sent by the pre-processor to the renderer to load a new chunk.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class ProcessorChunkLoadedEvent
		extends AbstractEvent {
	Chunk<MeshChunkId, MeshChunkData> chunk;
}
