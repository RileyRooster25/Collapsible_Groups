package com.starskyxiii.collapsible_groups.client.skies;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.List;

/**
 * Reserves the detail panel's rectangle between the screen's content and JEI's list, through JEI's public
 * gui handler API, so JEI never lays a slot under the panel. The rectangle depends on screen geometry only.
 */
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
				if (SkiesJei.asking()) return List.of();
				Rect2i area = SkiesDisplay.reservedArea(Minecraft.getInstance().screen);
				return area == null ? List.of() : List.of(area);
			}
		});
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		SkiesJei.setRuntime(jeiRuntime);
		SkiesDisplay.jeiStarted();
	}

	@Override
	public void onRuntimeUnavailable() {
		SkiesDisplay.jeiStopped();
		SkiesJei.setRuntime(null);
	}
}
