package com.starskyxiii.collapsible_groups.mixin;

import mezz.jei.gui.input.GuiTextFieldFilter;
import mezz.jei.gui.overlay.ISearchField;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;

/**
 * JEI 15.74 exposes unremapped search-field methods, but Forge remaps the
 * matching EditBox overrides. Bridge the interface to those original overrides.
 */
@Mixin(value = GuiTextFieldFilter.class, remap = false)
@Implements(@Interface(iface = ISearchField.class, prefix = "cg$", remap = Interface.Remap.NONE))
public abstract class MixinGuiTextFieldFilter {

	@Intrinsic
	public void cg$setValue(String value) {
		((EditBox) (Object) this).setValue(value);
	}

	@Intrinsic
	public void cg$setFocused(boolean focused) {
		((EditBox) (Object) this).setFocused(focused);
	}
}
