package com.starskyxiii.collapsible_groups.client.skies;

import com.starskyxiii.collapsible_groups.skies.ViewMode;
import com.starskyxiii.collapsible_groups.skies.ViewModes;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * The chosen view mode for JEI's ingredient list and the way to make JEI lay its list out again when it
 * changes. Holds no JEI types, so the key handler can use it whether or not JEI is installed; the JEI
 * overlay hook supplies the relayout.
 */
public final class SkiesViewState {
	private static final long ANNOUNCE_MILLIS = 1500;

	private static volatile Runnable relayout = () -> {};
	private static volatile String announcement = "";
	private static volatile long announcedAt;

	private SkiesViewState() {}

	public static ViewMode chosen() {
		return SkiesConfig.viewMode();
	}

	public static ViewModes.AutoRule autoRule() {
		return SkiesConfig.autoRule();
	}

	/** Set by the JEI overlay hook: forces JEI to recompute its list layout for the current screen. */
	public static void setRelayout(Runnable action) {
		relayout = action == null ? () -> {} : action;
	}

	/** Lays JEI's list out again on the main thread, so a new mode or setting shows at once. */
	public static void requestRelayout() {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.execute(() -> relayout.run());
	}

	/** Moves to the next mode, stores it, relays the list out and says which mode is now in use. */
	static void cycle() {
		ViewMode next = chosen().next();
		SkiesConfig.setViewMode(next);
		requestRelayout();
		String text = "View: " + next.label();
		announcement = text;
		announcedAt = Util.getMillis();
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.screen == null) minecraft.gui.setOverlayMessage(Component.literal(text), false);
	}

	/** The mode message while it is still fresh, else empty. */
	static String announcement() {
		return Util.getMillis() - announcedAt < ANNOUNCE_MILLIS ? announcement : "";
	}
}
