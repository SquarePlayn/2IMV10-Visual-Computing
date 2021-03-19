package nl.tue.visualcomputingproject.group9a.project.common.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class Pair<V1, V2> {
	private V1 first;
	private V2 second;
}
