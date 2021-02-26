package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileIdFactory;

import java.io.File;
import java.util.Objects;

/**
 * This class uniquely identifies a {@link Chunk}.
 */
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class ChunkId
		implements FileId {
	private static final String PRE = "chunk_id";
	
	/** The position of the chunk. */
	private final ChunkPosition position;
	/** The quality of the chunk. */
	private final QualityLevel quality;

	@Override
	public String getPath() {
		return FileId.genPath(
				PRE,
				position.getX(),
				position.getY(),
				position.getWidth(),
				position.getHeight(),
				quality.getOrder());
	}
	
	public static FileIdFactory<ChunkId> createChunkIdFactory() {
		return (String path) -> {
			String[] split = path.split(FileId.DELIM);
			if (split.length != 6 && !Objects.equals(split[0], PRE)) {
				return null;
			}
			try {
				ChunkPosition position = new ChunkPosition(
						Integer.parseInt(split[1]),
						Integer.parseInt(split[2]),
						Integer.parseInt(split[3]),
						Integer.parseInt(split[4]));
				QualityLevel quality = QualityLevel.fromOrder(Integer.parseInt(split[5]));
				return new ChunkId(position, quality);
				
			} catch (NumberFormatException e) {
				return null;
			}
		};
	}

//	/**
//	 * Factory class for serializing and deserializing this ID
//	 * to a string used for storing cache files.
//	 */
//	@Value
//	private static class ChunkIdCacheNameFactory
//			implements CacheNameFactory<ChunkId> {
//		/** The text to prepend each file with. */
//		String prepend;
//
//		@Override
//		public String getString(ChunkId val) {
//			return prepend + val.position.getY() + "_" +
//					val.position.getX() + "_" +
//					val.position.getWidth() + "_" +
//					val.position.getHeight() + "_" +
//					val.quality.ordinal();
//		}
//
//		@Override
//		public ChunkId fromString(String str) {
//			String[] args = str.substring(prepend.length()).split("_");
//			if (args.length != 5) {
//				throw new IllegalArgumentException("Invalid string: " + str);
//			}
//			return new ChunkId(
//					new ChunkPosition(
//							Double.parseDouble(args[0]),
//							Double.parseDouble(args[1]),
//							Double.parseDouble(args[2]),
//							Double.parseDouble(args[3])
//					),
//					QualityLevel.values()[Integer.parseInt(args[4])]
//			);
//		}
//	}
//
//	/**
//	 * Creates a {@link CacheNameFactory} for the {@link ChunkId} class where
//	 * each cache file contains the additional {@code prepend}ed string.
//	 * <br>
//	 * The prepended string can contain file separators, except for the first character.
//	 * 
//	 * @param prepend The string to prepend to the serialized data.
//	 * 
//	 * @return A {@link CacheNameFactory} used to generate the names of the cache files.
//	 */
//	public static CacheNameFactory<ChunkId> createCacheNameFactory(String prepend) {
//		return new ChunkIdCacheNameFactory(prepend);
//	}
//
//	/**
//	 * Creates a {@link CacheNameFactory} for the {@link ChunkId} class.
//	 *
//	 * @return A {@link CacheNameFactory} used to generate the names of the cache files.
//	 * 
//	 * @see #createCacheNameFactory(String) 
//	 */
//	public static CacheNameFactory<ChunkId> createCacheNameFactory() {
//		return createCacheNameFactory("");
//	}
	
}
