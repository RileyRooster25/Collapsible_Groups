package com.starskyxiii.collapsible_groups.mixin;

import com.starskyxiii.collapsible_groups.client.skies.SkiesListView;
import com.starskyxiii.collapsible_groups.skies.ListLayout;
import com.starskyxiii.collapsible_groups.viewer.ViewerLifecycleCoordinator;
import mezz.jei.gui.overlay.ingredients.IngredientGrid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * View modes for JEI's ingredient list: after JEI lays out its 18px slots, Medium and Details replace them
 * with larger tiles or full-width rows and report how many fit, so JEI's own paging, scrolling, clicks and
 * tooltips follow the new slots. Small mode changes nothing. Every hook is optional (require = 0): on a
 * JEI without these methods the list simply stays JEI's grid.
 */
@Mixin(value = IngredientGrid.class, remap = false)
public abstract class MixinIngredientGrid {
	@Unique private int cg$columns = -1;
	@Unique private int cg$rows = -1;

	@Inject(method = "updateSlots(I)V", at = @At("TAIL"), require = 0)
	private void cg$applyViewMode(int smoothScrollRowPixelOffset, CallbackInfo ci) {
		ListLayout.Layout layout = ViewerLifecycleCoordinator.isJeiSelected()
			? SkiesListView.apply((IngredientGrid) (Object) this) : null;
		this.cg$columns = layout == null ? -1 : layout.columns();
		this.cg$rows = layout == null ? -1 : layout.rows();
	}

	@Inject(method = "getColumnCount()I", at = @At("HEAD"), cancellable = true, require = 0)
	private void cg$columnCount(CallbackInfoReturnable<Integer> cir) {
		if (this.cg$columns >= 0) cir.setReturnValue(this.cg$columns);
	}

	@Inject(method = "getRowCount()I", at = @At("HEAD"), cancellable = true, require = 0)
	private void cg$rowCount(CallbackInfoReturnable<Integer> cir) {
		if (this.cg$rows >= 0) cir.setReturnValue(this.cg$rows);
	}
}
