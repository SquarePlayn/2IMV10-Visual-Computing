package nl.tue.visualcomputingproject.group9a.project.renderer.engine.model;

import lombok.Data;

import java.nio.ByteBuffer;

@Data
public class Texture {
	final int width;
	final int height;
	final ByteBuffer buffer;
}
