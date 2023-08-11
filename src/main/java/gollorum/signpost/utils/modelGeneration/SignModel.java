package gollorum.signpost.utils.modelGeneration;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import gollorum.signpost.minecraft.gui.utils.Colors;
import gollorum.signpost.minecraft.gui.utils.TextureResource;
import gollorum.signpost.minecraft.rendering.RenderingUtil;
import gollorum.signpost.utils.math.geometry.AABB;
import gollorum.signpost.utils.math.geometry.Vector3;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.*;

import static net.minecraft.client.renderer.LevelRenderer.DIRECTIONS;

public class SignModel {

	private final Map<Material, List<Quad>> quads = new HashMap<>();

	public void addCube(Cube<ResourceLocation> cube) {
		for(Cube.Quad<ResourceLocation> q: cube.getQuads()) {
			Quad quad = new Quad(
				q.normal().asVec3f(),
				Arrays.stream(q.vertices()).map(v -> new Quad.Vertex(
					v.pos().div((float) TextureResource.defaultTextureSize).asVec3f(),
					v.u() / (float) TextureResource.defaultTextureSize,
					v.v() / (float) TextureResource.defaultTextureSize,
					q.faceData().rotation()
				)).toArray(Quad.Vertex[]::new),
				q.faceData().tintIndex()
			);
			quads.computeIfAbsent(new Material(InventoryMenu.BLOCK_ATLAS, q.faceData().texture()), k -> new ArrayList<>())
				.add(quad);
		}
	}

	public void render(
		PoseStack.Pose matrixEntry,
		Matrix4f rotationMatrix,
		MultiBufferSource buffer,
		RenderType renderType,
		int packedLight,
		int packedOverlay,
		boolean useAmbientOcclusion,
		@Nullable BlockAndTintGetter level,
		@Nullable BlockState state,
		@Nullable BlockPos pos,
		int[] tints
	) {
		Matrix4f matrix4f = matrixEntry.pose();
		Matrix3f matrixNormal = matrixEntry.normal();

		var renderer = RenderingUtil.Renderer.get();
		BitSet bitset = new BitSet(3);
		float[] aoValues = useAmbientOcclusion ? new float[DIRECTIONS.length * 2] : null;
		ModelBlockRenderer.AmbientOcclusionFace aoFace = useAmbientOcclusion ? renderer.new AmbientOcclusionFace() : null;

		var colors = new float[3 * tints.length];
		for(var i = 0; i < tints.length; i++) {
			int tint = tints[i];
			colors[i * 3] = Colors.getRed(tint) / 255f;
			colors[i * 3 + 1] = Colors.getGreen(tint) / 255f;
			colors[i * 3 + 2] = Colors.getBlue(tint) / 255f;
		}

		var transformedVertices = new Vector4f[4];
		for(Map.Entry<Material, List<Quad>> entry : quads.entrySet()) {
			for(Quad quad : entry.getValue()) {

				Vector3f normal = quad.normal.copy();
				normal.transform(matrixNormal);
				float normalX = normal.x();
				float normalY = normal.y();
				float normalZ = normal.z();

				float rFinal = 1;
				float gFinal = 1;
				float bFinal = 1;
				if(quad.tintIndex >= 0) {
					rFinal = colors[quad.tintIndex * 3];
					gFinal = colors[quad.tintIndex * 3 + 1];
					bFinal = colors[quad.tintIndex * 3 + 2];
				}
// TODO DS: THIS THIS THIS HTIS THIS HTIS HTIS HTRIS HRISTHIS THIS THIS THIS THIS
				for(var i = 0; i < quad.vertices.length; i++) {
					var vertex = quad.vertices[i];// tgese are im camera space, in newed them in global spacve
					var vert = new Vector4f(vertex.pos.x(), vertex.pos.y(), vertex.pos.z(), 1.0F);
					vert.transform(matrix4f);
					transformedVertices[i] = vert;
				}

				VertexConsumer vertexBuilder = entry.getKey().buffer(buffer, x -> renderType);

				if(useAmbientOcclusion) {
					var localNormal = new Vector4f(quad.normal.x(), quad.normal.y(), quad.normal.z(), 0);
					localNormal.transform(rotationMatrix);
					var dir = Direction.getNearest(localNormal.x(), localNormal.y(), localNormal.z());
					calculateShape(level, state, pos, transformedVertices, dir, aoValues, bitset);
					aoFace.calculate(level, state, pos, dir, aoValues, bitset, true);
				}

ß				for(var i = 0; i < quad.vertices.length; i++) {
					Vector4f vert = transformedVertices[i];
					var vertex = quad.vertices[i];
					if(useAmbientOcclusion) {
						vertexBuilder.vertex(
							vert.x(), vert.y(), vert.z(),
							rFinal * aoFace.brightness[0], gFinal * aoFace.brightness[1], bFinal * aoFace.brightness[2], 1,
							vertex.u,
							vertex.v,
							packedOverlay,
							packedLight,
							normalX, normalY, normalZ
						);
					} else
						vertexBuilder.vertex(
							vert.x(), vert.y(), vert.z(),
							rFinal, gFinal, bFinal, 1,
							vertex.u,
							vertex.v,
							packedOverlay,
							packedLight,
							normalX, normalY, normalZ
						);
				}
			}
		}
	}

	private static void calculateShape(BlockAndTintGetter level, BlockState state, BlockPos pos, Vector4f[] vertices, Direction dir, @Nullable float[] aoValues, BitSet bitSet) {
		var bounds = new AABB(Arrays.stream(vertices).map(Vector3::fromVector4f));

		if(aoValues != null) {
			aoValues[Direction.WEST.get3DDataValue()] = bounds.min.x;
			aoValues[Direction.EAST.get3DDataValue()] = bounds.max.x;
			aoValues[Direction.DOWN.get3DDataValue()] = bounds.min.y;
			aoValues[Direction.UP.get3DDataValue()] = bounds.max.y;
			aoValues[Direction.NORTH.get3DDataValue()] = bounds.min.z;
			aoValues[Direction.SOUTH.get3DDataValue()] = bounds.max.z;
			int j = DIRECTIONS.length;
			aoValues[Direction.WEST.get3DDataValue() + j] = 1.0F - bounds.min.x;
			aoValues[Direction.EAST.get3DDataValue() + j] = 1.0F - bounds.max.x;
			aoValues[Direction.DOWN.get3DDataValue() + j] = 1.0F - bounds.min.y;
			aoValues[Direction.UP.get3DDataValue() + j] = 1.0F - bounds.max.y;
			aoValues[Direction.NORTH.get3DDataValue() + j] = 1.0F - bounds.min.z;
			aoValues[Direction.SOUTH.get3DDataValue() + j] = 1.0F - bounds.max.z;
		}

		float minThresh = 1.0E-4F;
		float maxThresh = 0.9999F;
		switch (dir) {
			case DOWN, UP -> {
				bitSet.set(1, bounds.min.x >= minThresh || bounds.min.z >= minThresh || bounds.max.x <= maxThresh || bounds.max.z <= maxThresh);
				bitSet.set(0, bounds.min.y == bounds.max.y && ((dir == Direction.DOWN ? bounds.min.y < minThresh : bounds.max.y > maxThresh) || state.isCollisionShapeFullBlock(level, pos)));
			}
            case NORTH, SOUTH -> {
				bitSet.set(1, bounds.min.x >= minThresh || bounds.min.y >= minThresh || bounds.max.x <= maxThresh || bounds.max.y <= maxThresh);
				bitSet.set(0, bounds.min.z == bounds.max.z && ((dir == Direction.NORTH ? bounds.min.z < minThresh : bounds.max.z > maxThresh) || state.isCollisionShapeFullBlock(level, pos)));
			}
			case WEST, EAST -> {
				bitSet.set(1, bounds.min.y >= minThresh || bounds.min.z >= minThresh || bounds.max.y <= maxThresh || bounds.max.z <= maxThresh);
				bitSet.set(0, bounds.min.x == bounds.max.x && ((dir == Direction.EAST ? bounds.min.x < minThresh : bounds.max.x > maxThresh) || state.isCollisionShapeFullBlock(level, pos)));
			}
		}
	}

	private static record Quad(Vector3f normal, Vertex[] vertices, int tintIndex) {

		public static class Vertex {
			public final Vector3f pos;
			public final float u;
			public final float v;

			public Vertex(Vector3f pos, float u, float v, FaceRotation rotation) {
				this.pos = pos;
				switch (rotation) {
					case Clockwise90 -> {
						this.u = 1 - v;
						this.v = u;
					}
					case CounterClockwise90 -> {
						this.u = v;
						this.v = 1 - u;
					}
					case UpsideDown -> {
						this.u = 1 - u;
						this.v = 1 - v;
					}
					default -> {
						this.u = u;
						this.v = v;
					}
				}
			}
		}

	}

}
