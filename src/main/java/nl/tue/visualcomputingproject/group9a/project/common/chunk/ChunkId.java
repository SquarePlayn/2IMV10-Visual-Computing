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
	
	
}
