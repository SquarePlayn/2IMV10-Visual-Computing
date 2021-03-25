package nl.tue.visualcomputingproject.group9a.project.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.chunk.QualityLevel;

@Getter
@AllArgsConstructor
public enum TextureType {
	Aerial(0),
	OpenStreetMap(1),
	;
	
	final private int order;
	
	public static TextureType fromOrder(int order) {
		for (TextureType type : values()) {
			if (type.order == order) return type;
		}
		return null;
	}
}
