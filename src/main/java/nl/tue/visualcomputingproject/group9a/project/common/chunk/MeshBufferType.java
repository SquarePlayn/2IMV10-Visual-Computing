package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MeshBufferType {
	TRIANGLES_CLOCKWISE_3_INT(true),
	TRIANGLES_COUNTER_CLOCKWISE_3_INT(false),
	QUADS_CLOCKWISE_4_INT(true),
	QUADS_COUNTER_CLOCKWISE_4_INT(false);
	
	private final boolean clockwise;
	
}
