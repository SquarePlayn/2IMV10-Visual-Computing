package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.Value;
import nl.tue.visualcomputingproject.group9a.project.common.cache.CacheNameFactory;

@Value
public class ChunkId {
	ChunkPosition position;
	QualityLevel quality;
	
	@Value
	private static class ChunkIdCacheNameFactory
			implements CacheNameFactory<ChunkId> {
		String prepend;

		@Override
		public String getString(ChunkId val) {
			return prepend + val.position.getLat() + "_" +
					val.position.getLon() + "_" +
					val.position.getWidth() + "_" +
					val.position.getHeight() + "_" +
					val.quality.ordinal();
		}

		@Override
		public ChunkId fromString(String str) {
			String[] args = str.substring(prepend.length()).split("_");
			if (args.length != 5) {
				throw new IllegalArgumentException("Invalid string: " + str);
			}
			return new ChunkId(
					new ChunkPosition(
							Double.parseDouble(args[0]),
							Double.parseDouble(args[1]),
							Double.parseDouble(args[2]),
							Double.parseDouble(args[3])
					),
					QualityLevel.values()[Integer.parseInt(args[4])]
			);
		}
	}
	
	public static CacheNameFactory<ChunkId> createCacheNameFactory(String prepend) {
		return new ChunkIdCacheNameFactory(prepend);
	}

	public static CacheNameFactory<ChunkId> createCacheNameFactory() {
		return createCacheNameFactory("");
	}
	
}
