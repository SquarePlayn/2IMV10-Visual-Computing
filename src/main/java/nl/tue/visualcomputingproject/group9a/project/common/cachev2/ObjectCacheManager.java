package nl.tue.visualcomputingproject.group9a.project.common.cachev2;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Getter
@AllArgsConstructor
public class ObjectCacheManager<T extends FileId, V> {
	private final CacheFileStreamRequester<T> streamRequester;
	public final ObjectSerializer<V> serializer;
	
	public boolean writeObject(T id, V obj, boolean overwrite) {
		streamRequester.claim(id);
		try {
			if (!overwrite && !streamRequester.isCached(id)) {
				return false;
			}
			try (OutputStream os = streamRequester.getOutputStream(id)) {
				serializer.serialize(os, obj);
				return true;
				
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		} finally {
			streamRequester.release(id);
		}
	}
	
	public V readObject(T id) {
		streamRequester.claim(id);
		try {
			if (!streamRequester.isCached(id)) {
				return null;
			}
			try (InputStream is = streamRequester.getInputStream(id)) {
				return serializer.deserialize(is);
				
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			
		} finally {
			streamRequester.release(id);
		}
	}
	
	public boolean isCached(T id) {
		return streamRequester.isCached(id);
	}
	
}
