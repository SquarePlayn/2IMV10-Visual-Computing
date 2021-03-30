package nl.tue.visualcomputingproject.group9a.project.chart.assembly;

import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.chart.wfs.MapSheet;
import nl.tue.visualcomputingproject.group9a.project.common.Point;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.Chunk;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkId;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.PointCloudChunkData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ChunkAssemblyJob {
	@Getter
	private final ChunkId chunkId;
	private final Set<MapSheet> sheetsLeft;
	private final List<Chunk<ChunkId, PointCloudChunkData>> partialChunks = new ArrayList<>();
	
	public ChunkAssemblyJob(ChunkId chunkId, Set<MapSheet> sheetsLeft) {
		this.chunkId = chunkId;
		this.sheetsLeft = sheetsLeft;
	}
	
	public void newPartialChunk(Chunk<ChunkId, PointCloudChunkData> chunk, MapSheet sheet) {
		if (!sheetsLeft.contains(sheet)) {
			//Either we were never meant to get data from this sheet or we already have it.
			throw new IllegalStateException("New partial chunk of sheet that we're not looking for data from.");
		}
		if (!chunkId.equals(chunk.getChunkId())) {
			throw new IllegalStateException("Attempted to register partial chunk which isn't mine.");
		}
		
		sheetsLeft.remove(sheet);
		partialChunks.add(chunk);
	}
	
	public boolean isReadyForAssembly() {
		return sheetsLeft.isEmpty();
	}
	
	public int getNumberOfPartialChunks() {
		return partialChunks.size();
	}
	
	public Chunk<ChunkId, PointCloudChunkData> assembleChunk() {
		PointCloudChunkData points = new PointCloudChunkData();
		int numData = 0;
		for (Chunk<ChunkId, PointCloudChunkData> chunk : partialChunks) {
			numData += chunk.getData().getInterleavedPoints().length;
		}
		
		double[] newPoints = new double[numData];
		int ctr = 0;
		
		for (Chunk<ChunkId, PointCloudChunkData> chunk : partialChunks) {
			for (int i = 0; i < chunk.getData().getInterleavedPoints().length; i++) {
				newPoints[ctr++] = chunk.getData().getInterleavedPoints()[i];
			}
		}
		
		points.setInterleavedPoints(newPoints);
		
		return new Chunk<>(chunkId, points);
	}
	
	@Override
	public String toString() {
		return "ChunkAssemblyJob{" +
			"chunkId=" + chunkId +
			", sheetsLeft=" + sheetsLeft +
			", partialChunks=" + partialChunks +
			'}';
	}
}
