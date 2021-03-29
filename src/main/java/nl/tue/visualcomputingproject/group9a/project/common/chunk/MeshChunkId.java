package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import nl.tue.visualcomputingproject.group9a.project.common.Settings;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileIdFactory;

import java.util.Objects;

@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
public class MeshChunkId
		extends ChunkId {
	private static final String PRE = "mesh-chunk-id";
	private final VertexBufferType vertexType;
	private final MeshBufferType meshType;
	
	public MeshChunkId(
			ChunkPosition position,
			QualityLevel quality,
			VertexBufferType vertexType,
			MeshBufferType meshType) {
		super(position, quality);
		this.vertexType = vertexType;
		this.meshType = meshType;
	}
	
	@Override
	public String getPath() {
		return FileId.genPath(
				PRE,
				getPosition().getX(),
				getPosition().getY(),
				getPosition().getWidth(),
				getPosition().getHeight(),
				getQuality().getOrder(),
				vertexType.getId(),
				meshType.getId());
	}
	
	public ChunkId asChunkId() {
		return new ChunkId(getPosition().transformed(), getQuality());
	}
	
	public ChunkId asExtraBorderChunkId(double border) {
		return new ChunkId(getPosition().transformedAddBorder(border), getQuality());
	}
	
	public MeshChunkId asExtraBorder(double border) {
		return new MeshChunkId(
				getPosition().addBorder(border),
				getQuality(),
				vertexType,
				meshType
		);
	}
	
	public MeshChunkId withQuality(QualityLevel quality) {
		if (quality == this.getQuality()) {
			return this;
		} else {
			return new MeshChunkId(getPosition(), quality, vertexType, meshType);
		}
	}
	
	public static FileIdFactory<MeshChunkId> createMeshChunkIdFactory() {
		return (String path) -> {
			String[] split = path.split(FileId.DELIM);
			if (split.length != 8 || !Objects.equals(split[0], PRE)) {
				return null;
			}
			try {
				ChunkPosition position = new ChunkPosition(
						Double.parseDouble(split[1]),
						Double.parseDouble(split[2]),
						Double.parseDouble(split[3]),
						Double.parseDouble(split[4]));
				QualityLevel quality = QualityLevel.fromOrder(Integer.parseInt(split[5]));
				VertexBufferType vertexType = VertexBufferType.fromId(Integer.parseInt(split[6]));
				MeshBufferType meshType = MeshBufferType.fromId(Integer.parseInt(split[7]));
				return new MeshChunkId(position, quality, vertexType, meshType);
				
			} catch (NumberFormatException e) {
				e.printStackTrace();
				return null;
			}
		};
	}
	
}
