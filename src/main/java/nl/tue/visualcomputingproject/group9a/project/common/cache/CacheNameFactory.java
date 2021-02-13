package nl.tue.visualcomputingproject.group9a.project.common.cache;

public interface CacheNameFactory<V> {
	
	String getString(V val);
	
	V fromString(String str);
	
}
