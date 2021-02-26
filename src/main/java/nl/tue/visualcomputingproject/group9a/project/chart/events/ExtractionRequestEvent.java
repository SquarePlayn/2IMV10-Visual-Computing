package nl.tue.visualcomputingproject.group9a.project.chart.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import nl.tue.visualcomputingproject.group9a.project.chart.wfs.MapSheet;
import nl.tue.visualcomputingproject.group9a.project.common.cache.disk.FileReadCacheClaim;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;

import java.util.Collection;

@Data
@AllArgsConstructor
public class ExtractionRequestEvent {
	FileReadCacheClaim claim;
	MapSheet sheet;
	QualityLevel level;
	Collection<ChunkPosition> positions;
}
