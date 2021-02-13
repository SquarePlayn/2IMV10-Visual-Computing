package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.Getter;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

@Getter
public enum VertexBufferType {
	VERTEX_3_FLOAT_NORMAL_3_FLOAT,
	INTERLEAVED_VERTEX_3_FLOAT_NORMAL_3_FLOAT
}
