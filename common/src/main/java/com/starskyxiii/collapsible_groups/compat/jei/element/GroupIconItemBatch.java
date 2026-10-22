package com.starskyxiii.collapsible_groups.compat.jei.element;

import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;

/**
 * A single render call's item batch. Models and their overrides are resolved on every draw,
 * with the same player context as GuiGraphics.renderItem; no visual state survives the frame.
 */
final class GroupIconItemBatch implements AutoCloseable {
	private final GuiGraphics graphics;
	private final Minecraft minecraft;
	private final GroupIconBatchLighting lighting;

	GroupIconItemBatch(GuiGraphics graphics) {
		this.graphics = graphics;
		this.minecraft = Minecraft.getInstance();
		this.lighting = new GroupIconBatchLighting(graphics::flush, flat -> {
			if (flat) Lighting.setupForFlatItems();
			else Lighting.setupFor3DItems();
		});
	}

	/** Returns false when a custom renderer needs the original vanilla rendering boundary. */
	boolean renderItem(ItemStack stack, int x, int y) {
		if (stack.isEmpty()) return true;
		// ItemRenderer substitutes a different GUI model for these after getModel().
		if (stack.is(Items.TRIDENT) || stack.is(Items.SPYGLASS)) return false;
		ItemRenderer renderer = minecraft.getItemRenderer();
		BakedModel model = renderer.getModel(stack, minecraft.level, minecraft.player, 0);
		if (model.isCustomRenderer() || !GroupIconModelTransformSupport.canBatch(model)) return false;

		lighting.prepare(!model.usesBlockLight());
		graphics.pose().pushPose();
		try {
			graphics.pose().translate(x + 8, y + 8, 150);
			graphics.pose().mulPoseMatrix(new Matrix4f().scaling(1.0F, -1.0F, 1.0F));
			graphics.pose().scale(16.0F, 16.0F, 16.0F);
			renderer.render(stack, ItemDisplayContext.GUI, false, graphics.pose(),
				graphics.bufferSource(), 0xF000F0, OverlayTexture.NO_OVERLAY, model);
		} catch (Throwable throwable) {
			CrashReport report = CrashReport.forThrowable(throwable, "Rendering item");
			CrashReportCategory category = report.addCategory("Item being rendered");
			category.setDetail("Item Type", () -> String.valueOf(stack.getItem()));
			category.setDetail("Item Foil", () -> String.valueOf(stack.hasFoil()));
			throw new ReportedException(report);
		} finally {
			graphics.pose().popPose();
		}
		return true;
	}

	void beforeFallback() {
		lighting.beforeFallback();
	}

	void renderIndicator(String text, int x, int y) {
		Font font = minecraft.font;
		font.drawInBatch(text, x, y, 0xFFFFFFFF, true, graphics.pose().last().pose(),
			graphics.bufferSource(), Font.DisplayMode.NORMAL, 0, 0xF000F0, font.isBidirectional());
	}

	@Override
	public void close() {
		lighting.close();
	}
}
