package com.starskyxiii.collapsible_groups.mixin;

import com.starskyxiii.collapsible_groups.client.skies.SkiesListView;
import com.starskyxiii.collapsible_groups.compat.jei.element.PreRenderIngredientGridElement;
import com.starskyxiii.collapsible_groups.skies.ViewMode;
import mezz.jei.gui.overlay.elements.IElement;
import mezz.jei.gui.overlay.ingredients.IngredientListRenderer;
import mezz.jei.gui.overlay.ingredients.IngredientListSlot;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = IngredientListRenderer.class, remap = false)
public abstract class MixinIngredientListRenderer implements SkiesListView.ModeHolder {
	@Shadow
	@Final
	private List<IngredientListSlot> slots;
	@Unique private ViewMode cg$mode = ViewMode.SMALL;
	@Unique private int cg$firstRank = 1;

	@Override
	public void cg$setSkiesMode(ViewMode mode) {
		this.cg$mode = mode;
	}

	/** Skies view modes: Medium and Details draw the page themselves; Small leaves JEI's drawing alone. */
	@Inject(
		method = "render(Lnet/minecraft/client/gui/GuiGraphics;)V",
		at = @At("HEAD"),
		cancellable = true,
		require = 1
	)
	private void cg$drawViewMode(GuiGraphics guiGraphics, CallbackInfo ci) {
		if (this.cg$mode == ViewMode.SMALL) return;
		SkiesListView.render(guiGraphics, this.slots, this.cg$mode, this.cg$firstRank);
		ci.cancel();
	}

	@Inject(method = "set(ILjava/util/List;)V", at = @At("HEAD"), require = 0)
	private void cg$notePage(int startIndex, List<IElement<?>> ingredientList, CallbackInfo ci) {
		if (this.cg$mode != ViewMode.SMALL) this.cg$firstRank = SkiesListView.firstRank(ingredientList, startIndex);
	}

	@Inject(
		method = "render(Lnet/minecraft/client/gui/GuiGraphics;)V",
		at = @At("HEAD"),
		require = 1
	)
	private void cg$drawPreRenderBackgrounds(GuiGraphics guiGraphics, CallbackInfo ci) {
		for (IngredientListSlot slot : this.slots) {
			slot.getOptionalElement().ifPresent(element -> {
				if (element instanceof PreRenderIngredientGridElement preRenderElement) {
					var renderArea = slot.getRenderArea();
					preRenderElement.drawPreRender(guiGraphics, renderArea.getX(), renderArea.getY());
				}
			});
		}
	}
}
