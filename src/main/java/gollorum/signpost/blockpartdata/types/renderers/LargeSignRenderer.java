package gollorum.signpost.blockpartdata.types.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import gollorum.signpost.blockpartdata.Overlay;
import gollorum.signpost.blockpartdata.types.LargeSignBlockPart;
import gollorum.signpost.minecraft.rendering.ModelRegistry;
import gollorum.signpost.utils.modelGeneration.SignModel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;

import static gollorum.signpost.minecraft.utils.CoordinatesUtil.FontToVoxelSize;
import static gollorum.signpost.minecraft.utils.CoordinatesUtil.VoxelSize;

public class LargeSignRenderer extends SignRenderer<LargeSignBlockPart> {

	private static final float TEXT_OFFSET_RIGHT = 7f * VoxelSize;
	private static final float TEXT_OFFSET_LEFT_SHORT = 9f * VoxelSize;
	private static final float TEXT_OFFSET_LEFT_LONG = 10f * VoxelSize;
	private static final float MAXIMUM_TEXT_WIDTH_SHORT = TEXT_OFFSET_RIGHT + TEXT_OFFSET_LEFT_SHORT;
	private static final float MAXIMUM_TEXT_WIDTH_LONG = TEXT_OFFSET_RIGHT + TEXT_OFFSET_LEFT_LONG;

	private static final float TEXT_RATIO = 1.3f;
	private static final float FONT_SIZE_VOXELS = 2 / TEXT_RATIO;

	@Override
	protected BakedModel makeBakedModel(LargeSignBlockPart sign) {
		return ModelRegistry.LargeBakedSign.makeModel(sign);
	}

	@Override
	protected BakedModel makeBakedOverlayModel(LargeSignBlockPart sign, Overlay overlay) {
		return ModelRegistry.LargeBakedSign.makeOverlayModel(sign, overlay);
	}

	@Override
	protected SignModel makeModel(LargeSignBlockPart sign) {
		return ModelRegistry.LargeSign.makeModel(sign);
	}

	@Override
	protected SignModel makeOverlayModel(LargeSignBlockPart sign, Overlay overlay) {
		return ModelRegistry.LargeSign.makeOverlayModel(sign, overlay);
	}

	@Override
	public void renderText(LargeSignBlockPart sign, PoseStack matrix, Font fontRenderer, MultiBufferSource buffer, int combinedLights) {
		matrix.mulPose(Vector3f.ZP.rotationDegrees(180));
		matrix.mulPose(Vector3f.YP.rotation((float) (
			sign.isFlipped()
				? -sign.getAngle().radians() // No idea why this works. It does though, so I'm not touching it.
				: Math.PI - sign.getAngle().radians()
		)));
		matrix.translate(0, 3.5f * VoxelSize, -3.005 * VoxelSize);

		matrix.pushPose();
		render(sign, fontRenderer, sign.getText()[3], matrix, buffer, combinedLights, false);
		matrix.popPose();
		matrix.translate(0, -7 / 3f * VoxelSize, 0);

		matrix.pushPose();
		render(sign, fontRenderer, sign.getText()[2], matrix, buffer, combinedLights, true);
		matrix.popPose();
		matrix.translate(0, -7 / 3f * VoxelSize, 0);

		matrix.pushPose();
		render(sign, fontRenderer, sign.getText()[1], matrix, buffer, combinedLights, true);
		matrix.popPose();
		matrix.translate(0, -7 / 3f * VoxelSize, 0);

		matrix.pushPose();
		render(sign, fontRenderer, sign.getText()[0], matrix, buffer, combinedLights, false);
		matrix.popPose();
	}

	private void render(LargeSignBlockPart sign, Font fontRenderer, String text, PoseStack matrix, MultiBufferSource buffer, int combinedLights, boolean isLong) {
		float scale = FONT_SIZE_VOXELS * FontToVoxelSize;
		float MAX_WIDTH_FRAC = fontRenderer.width(text) * scale / (isLong ? MAXIMUM_TEXT_WIDTH_LONG : MAXIMUM_TEXT_WIDTH_SHORT);
		scale /= Math.max(1, MAX_WIDTH_FRAC);
		float offset = TEXT_OFFSET_RIGHT * Math.min(1, MAX_WIDTH_FRAC);
		matrix.translate(
			sign.isFlipped() ? offset - fontRenderer.width(text) * scale : -offset,
			-scale * 4 * TEXT_RATIO,
			0
		);
		matrix.scale(scale, scale * TEXT_RATIO, scale);
		fontRenderer.drawInBatch(text, 0, 0,
			sign.getColor(), false, matrix.last().pose(), buffer, false, 0, combinedLights);
	}

}
