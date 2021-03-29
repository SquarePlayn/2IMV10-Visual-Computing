package nl.tue.visualcomputingproject.group9a.project.common.event;

import lombok.Value;
import nl.tue.visualcomputingproject.group9a.project.common.TextureType;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.ChunkPosition;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

@Value
public class ChartTextureAvailableEvent {
	ChunkPosition position;
	TextureType type;
	ByteBuffer image;
	int width;
	int height;
}
