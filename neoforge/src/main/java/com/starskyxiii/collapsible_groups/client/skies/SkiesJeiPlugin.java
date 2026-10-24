package com.starskyxiii.collapsible_groups.client.skies;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.List;

/** Reserves the detail panel's column, so JEI lays its list out beside the panel rather than under it. */
@JeiPlugin
public class SkiesJeiPlugin implements IModPlugin {
	@Override
	public ResourceLocation getPluginUid() {
		return ResourceLocation.fromNamespaceAndPath("collapsible_groups", "skies_panel");
	}

	@Override
	public void registerGuiHandlers(IGuiHandlerRegistration registration) {
		registration.addGlobalGuiHandler(new IGlobalGuiHandler() {
			@Override
			public Collection<Rect2i> getGuiExtraAreas() {
				Rect2i area = SkiesDisplay.panelArea(Minecraft.getInstance().screen);
				return area == null ? List.of() : List.of(area);
			}
		});
	}
}
