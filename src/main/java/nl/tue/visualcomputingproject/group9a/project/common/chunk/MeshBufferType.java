package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enum for storing the type of a mesh buffer.
 */
@Getter
@AllArgsConstructor
public enum MeshBufferType {
	TRIANGLES_CLOCKWISE_3_INT(0, true),
	TRIANGLES_COUNTER_CLOCKWISE_3_INT(1, false),
	QUADS_CLOCKWISE_4_INT(2, true),
	QUADS_COUNTER_CLOCKWISE_4_INT(3, false);

	private final int id;
	private final boolean clockwise;
	
	static MeshBufferType fromId(int id) {
		for (MeshBufferType type : values()) {
			if (type.id == id) return type;
		}
		return null;
	}
	
}
