package nl.tue.visualcomputingproject.group9a.project.chart.download;

import lombok.Value;
import nl.tue.visualcomputingproject.group9a.project.chart.wfs.MapSheet;
import nl.tue.visualcomputingproject.group9a.project.common.cache.disk.FileReadWriteCacheClaim;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;

import java.util.Set;

@Value
public class DownloadJob {
	FileReadWriteCacheClaim claim;
	MapSheet sheet;
	QualityLevel level;
	Set<ChunkPosition> chunksRequested;
}
