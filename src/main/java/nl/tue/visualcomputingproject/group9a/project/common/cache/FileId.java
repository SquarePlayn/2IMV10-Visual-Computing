package nl.tue.visualcomputingproject.group9a.project.common.cache;

import java.util.Objects;

/**
 * Interface for all ID's used for files.
 */
@FunctionalInterface
public interface FileId {

	/**
	 * @return The unique path of this ID.
	 */
	String getPath();

	/**
	 * Generates a path from the given objects separated by underscores ({@code _}).
	 * 
	 * @param objs The objects to generate the path for.
	 * 
	 * @return A unique path using the {@link #toString()} function of the objects.
	 */
	static String genPath(Object... objs) {
		if (objs == null || objs.length == 0) {
			return "_";
		}
		
		StringBuilder sb = new StringBuilder(Objects.toString(objs[0]));
		for (int i = 1; i < objs.length; i++) {
			sb.append("_");
			sb.append(objs[i]);
		}
		return sb.toString();
	}
	
}
