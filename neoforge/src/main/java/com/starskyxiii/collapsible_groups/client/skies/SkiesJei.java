package com.starskyxiii.collapsible_groups.client.skies;

import com.starskyxiii.collapsible_groups.compat.jei.runtime.JeiRuntimeHolder;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

/**
 * The JEI questions the Skies display asks, through JEI's public runtime only. Loaded only when JEI is
 * installed (see {@link SkiesDisplay}), so nothing here is touched without it.
 */
final class SkiesJei {
	private static boolean asking;
	private static boolean lastShowing;

	private SkiesJei() {}

	/**
	 * The one predicate for the short tooltip and the panel: JEI's ingredient list is showing on this
	 * screen. JEI may ask for the panel's area while it is itself working this out; then the last answer
	 * is used instead of asking again.
	 */
	static boolean listShowing() {
		IJeiRuntime runtime = JeiRuntimeHolder.get();
		if (runtime == null) return false;
		if (asking) return lastShowing;
		asking = true;
		try {
			lastShowing = runtime.getIngredientListOverlay().isListDisplayed();
		} catch (RuntimeException failure) {
			lastShowing = false;
		} finally {
			asking = false;
		}
		return lastShowing;
	}

	/** The search box has the keyboard, so typed keys belong to it. */
	static boolean searchFocused() {
		IJeiRuntime runtime = JeiRuntimeHolder.get();
		return runtime != null && runtime.getIngredientListOverlay().hasKeyboardFocus();
	}

	/** The left and top edges of the screen's content as JEI knows them, or null for a screen JEI does not lay out beside. */
	static int @Nullable [] contentOrigin(Screen screen) {
		IJeiRuntime runtime = JeiRuntimeHolder.get();
		if (runtime == null || screen == null) return null;
		IGuiProperties properties = runtime.getScreenHelper().getGuiProperties(screen).orElse(null);
		if (properties == null) return null;
		return new int[] {properties.guiLeft(), properties.guiTop()};
	}
}
