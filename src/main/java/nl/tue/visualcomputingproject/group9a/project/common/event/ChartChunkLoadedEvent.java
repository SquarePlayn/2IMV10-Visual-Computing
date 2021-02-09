package nl.tue.visualcomputingproject.group9a.project.common.event;

import lombok.EqualsAndHashCode;
import lombok.Value;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.Chunk;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.PointCloudChunkData;

/**
 * Sent by the chart module when a new chunk is loaded.
 */
@EqualsAndHashCode(callSuper = true)
@Value
public class ChartChunkLoadedEvent extends AbstractEvent {
	Chunk<PointCloudChunkData> chunk;
}
