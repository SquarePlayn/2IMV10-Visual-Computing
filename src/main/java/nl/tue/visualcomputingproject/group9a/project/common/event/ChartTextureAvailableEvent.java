package nl.tue.visualcomputingproject.group9a.project.common.event;

import lombok.EqualsAndHashCode;
import lombok.Value;
import nl.tue.visualcomputingproject.group9a.project.common.TextureType;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;

import java.nio.ByteBuffer;

@EqualsAndHashCode(callSuper = true)
@Value
public class ChartTextureAvailableEvent
		extends AbstractEvent {
	ChunkPosition position;
	TextureType type;
	ByteBuffer image;
	int width;
	int height;
}
