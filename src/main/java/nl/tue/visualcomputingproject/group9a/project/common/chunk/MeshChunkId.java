package nl.tue.visualcomputingproject.group9a.project.common.chunk;

import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.cache.FileId;

import java.io.File;

@Getter
public class MeshChunkId
		extends ChunkId {
	VertexBufferType vertexType;
	MeshBufferType meshType;
	
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
		return "mesh_chunk" + File.separator + FileId.genPath(
				getPosition().getX(),
				getPosition().getY(),
				getPosition().getWidth(),
				getPosition().getHeight(),
				vertexType.getId(),
				meshType.getId());
	}
	
}
