package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

/**
 * Enum for the type of a vertex buffer.
 */
@Getter
@AllArgsConstructor
public enum VertexBufferType {
	VERTEX_3_FLOAT_NORMAL_3_FLOAT(0),
	INTERLEAVED_VERTEX_3_FLOAT_NORMAL_3_FLOAT(1);

	private final int id;
	
	static VertexBufferType fromId(int id) {
		for (VertexBufferType type : values()) {
			if (type.id == id) return type;
		}
		return null;
	}
	
}
