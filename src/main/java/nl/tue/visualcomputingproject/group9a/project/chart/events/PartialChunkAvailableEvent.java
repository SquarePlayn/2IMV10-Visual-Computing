package nl.tue.visualcomputingproject.group9a.project.chart.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import nl.tue.visualcomputingproject.group9a.project.chart.wfs.MapSheet;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.Chunk;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.PointCloudChunkData;

@Data
@AllArgsConstructor
public class PartialChunkAvailableEvent {
	Chunk<PointCloudChunkData> chunk;
	MapSheet sheet;
}
