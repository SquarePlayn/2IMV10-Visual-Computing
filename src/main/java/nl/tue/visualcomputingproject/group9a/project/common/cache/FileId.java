package nl.tue.visualcomputingproject.group9a.project.common.cache;

import java.util.Objects;

public abstract class FileId {
	
	public abstract String getPath();
	
	protected static String genPath(Object... objs) {
		if (objs == null) {
			return null;
		}
		if (objs.length == 0) {
			return "_";
		}
		
		StringBuilder sb = new StringBuilder(Objects.toString(objs[0]));
		for (int i = 1; i < objs.length; i++) {
			sb.append(Objects.toString(objs[i]));
		}
		return sb.toString();
	}
	
}
