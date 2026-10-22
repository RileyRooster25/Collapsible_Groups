package com.starskyxiii.collapsible_groups.compat.emi;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.starskyxiii.collapsible_groups.viewer.ViewerIngredient;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.ItemEmiStack;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Set;
import org.joml.Matrix4f;

/** A single header's two native item icons; no rendered data is retained between calls. */
final class EmiHeaderItemBatch {
	private static final Set<String> NATIVE_TRANSFORMS = Set.of(
		"net.minecraftforge.client.extensions.IForgeBakedModel",
		"net.neoforged.neoforge.client.extensions.IBakedModelExtension"
	);
	private static final ClassValue<Boolean> SAFE_TRANSFORMS = new ClassValue<>() {
		@Override protected Boolean computeValue(Class<?> type) {
			return EmiHeaderBatchPolicy.nativeTransform(type, NATIVE_TRANSFORMS,
				ItemDisplayContext.class, PoseStack.class);
		}
	};
	private static final EmiHeaderBatchPolicy.Decorations DECORATIONS = findDecorations();
	private record Prepared(ItemStack stack, BakedModel model) {}

	private EmiHeaderItemBatch() {}

	/** Returns false before drawing anything if either preview requires the native fallback. */
	static boolean tryRender(GuiGraphics graphics, List<ViewerIngredient<EmiIngredient>> previews, int flags) {
		if (previews.size() != 2) return false;
		Minecraft minecraft = Minecraft.getInstance();
		Prepared[] prepared = new Prepared[2];
		for (EmiHeaderBatchPolicy.Layer layer : EmiHeaderBatchPolicy.LAYERS) {
			EmiIngredient ingredient = previews.get(layer.index()).entry();
			if (ingredient.getClass() != ItemEmiStack.class) return false;
			prepared[layer.index()] = prepare((ItemEmiStack) ingredient, flags, minecraft);
			if (prepared[layer.index()] == null) return false;
		}

		try (EmiHeaderBatchPolicy.Lighting lighting = new EmiHeaderBatchPolicy.Lighting(graphics::flush, flat -> {
			if (flat) Lighting.setupForFlatItems();
			else Lighting.setupFor3DItems();
		})) {
			for (EmiHeaderBatchPolicy.Layer layer : EmiHeaderBatchPolicy.LAYERS) {
				Prepared item = prepared[layer.index()];
				lighting.prepare(!item.model().usesBlockLight());
				render(graphics, minecraft.getItemRenderer(), item, layer);
			}
		}
		// Eligibility proved these decorations have no visible content. Keep the native calls,
		// including their platform render-state handling, in the same back/front poses.
		for (EmiHeaderBatchPolicy.Layer layer : EmiHeaderBatchPolicy.LAYERS) {
			graphics.pose().pushPose();
			try {
				graphics.pose().translate(0, 0, layer.z());
				graphics.renderItemDecorations(minecraft.font, prepared[layer.index()].stack(),
					layer.x(), layer.y(), "");
			} finally {
				graphics.pose().popPose();
			}
		}
		return true;
	}

	private static Prepared prepare(ItemEmiStack ingredient, int flags, Minecraft minecraft) {
		ItemStack stack = ingredient.getItemStack();
		if (stack.isEmpty() || stack.is(Items.AIR)) return null;
		boolean cooldown = minecraft.player != null && minecraft.player.getCooldowns().isOnCooldown(stack.getItem());
		var state = new EmiHeaderBatchPolicy.VisualState(stack.isEmpty(), stack.is(Items.AIR), stack.hasFoil(),
			stack.isBarVisible(), cooldown, ingredient.getRemainder().isEmpty());
		if (!EmiHeaderBatchPolicy.allows(flags, state) || !DECORATIONS.empty(stack, ItemStack.EMPTY)) return null;
		// Vanilla substitutes another GUI model for these after getModel(), so use its whole path.
		if (stack.is(Items.TRIDENT) || stack.is(Items.SPYGLASS)) return null;
		BakedModel model = minecraft.getItemRenderer().getModel(stack, minecraft.level, null, 0);
		if (model.isCustomRenderer() || !SAFE_TRANSFORMS.get(model.getClass())) return null;
		return new Prepared(stack, model);
	}

	private static void render(GuiGraphics graphics, ItemRenderer renderer, Prepared item, EmiHeaderBatchPolicy.Layer layer) {
		graphics.pose().pushPose();
		try {
			graphics.pose().translate(layer.x() + 8, layer.y() + 8, layer.z() + 150);
			graphics.pose().mulPoseMatrix(new Matrix4f().scaling(1.0F, -1.0F, 1.0F));
			graphics.pose().scale(16.0F, 16.0F, 16.0F);
			renderer.render(item.stack(), ItemDisplayContext.GUI, false, graphics.pose(), graphics.bufferSource(),
				0xF000F0, OverlayTexture.NO_OVERLAY, item.model());
		} catch (Throwable throwable) {
			CrashReport report = CrashReport.forThrowable(throwable, "Rendering item");
			CrashReportCategory category = report.addCategory("Item being rendered");
			category.setDetail("Item Type", () -> String.valueOf(item.stack().getItem()));
			category.setDetail("Item Foil", () -> String.valueOf(item.stack().hasFoil()));
			throw new ReportedException(report);
		} finally {
			graphics.pose().popPose();
		}
	}

	private static EmiHeaderBatchPolicy.Decorations findDecorations() {
		Class<?> selected = null;
		for (String name : List.of("net.minecraftforge.client.ItemDecoratorHandler",
			"net.neoforged.neoforge.client.ItemDecoratorHandler")) {
			try {
				Class<?> api = Class.forName(name, false, EmiHeaderItemBatch.class.getClassLoader());
				if (selected != null) return EmiHeaderBatchPolicy.Decorations.unknown();
				selected = api;
			} catch (ClassNotFoundException ignored) {
				// A missing known API is normal on the other loader.
			} catch (SecurityException | LinkageError ignored) {
				return EmiHeaderBatchPolicy.Decorations.unknown();
			}
		}
		return selected == null ? EmiHeaderBatchPolicy.Decorations.unknown()
			: EmiHeaderBatchPolicy.Decorations.inspect(selected, ItemStack.class);
	}
}
