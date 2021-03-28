package nl.tue.visualcomputingproject.group9a.project.chart.events;

import lombok.Value;
import nl.tue.visualcomputingproject.group9a.project.common.TextureType;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;

@Value
public class TextureRequestEvent {
	TextureType type;
	ChunkPosition position;
}
