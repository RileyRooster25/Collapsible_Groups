package com.starskyxiii.collapsible_groups.mixin;

import com.starskyxiii.collapsible_groups.client.skies.ForcedShift;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Screen.hasShiftDown() answers true only while the detail panel builds its own copy of an item's tooltip,
 * so lines mods show only with Shift held reach the panel. Every other caller sees the real key state.
 */
@Mixin(Screen.class)
public abstract class MixinScreenShift {
	@Inject(method = "hasShiftDown", at = @At("HEAD"), cancellable = true)
	private static void cg$panelForcesShift(CallbackInfoReturnable<Boolean> cir) {
		if (ForcedShift.active()) cir.setReturnValue(true);
	}
}
