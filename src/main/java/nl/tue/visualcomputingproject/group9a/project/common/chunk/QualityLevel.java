package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The quality of the point cloud data a chunk was sourced from.
 */
@Getter
@AllArgsConstructor
public enum QualityLevel {
	FLAT(-1, true),
	FIVE_BY_FIVE(0, true),
	HALF_BY_HALF(1, true),
	LAS(5, false);
	
	final private int order;
	final private boolean interpolated;
	
	public static QualityLevel fromOrder(int order) {
		for (QualityLevel type : values()) {
			if (type.order == order) return type;
		}
		if (order < 0) {
			return getWorst();
		} else {
			return getBest();
		}
	}
	
	public static QualityLevel getBest() {
		return HALF_BY_HALF;
	}
	
	public static QualityLevel getWorst() {
		return FIVE_BY_FIVE;
	}
	
	public QualityLevel next() {
		return fromOrder(order + 1);
	}
	
	public QualityLevel prev() {
		return fromOrder(order - 1);
	}
	
}
