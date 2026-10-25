package com.starskyxiii.collapsible_groups.client.skies;

import com.starskyxiii.collapsible_groups.skies.ViewMode;
import com.starskyxiii.collapsible_groups.skies.ViewModes;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

/** The Skies Aero display settings, in config/collapsiblegroups/skies.toml. */
public final class SkiesConfig {
	public static final ModConfigSpec SPEC;
	public static final ModConfigSpec.BooleanValue SKIES_DISPLAY;
	public static final ModConfigSpec.BooleanValue WRITE_STATS;
	public static final ModConfigSpec.IntValue PANEL_WIDTH;
	public static final ModConfigSpec.ConfigValue<String> VIEW_MODE;
	public static final ModConfigSpec.ConfigValue<List<? extends String>> AUTO_MODES;
	public static final ModConfigSpec.IntValue AUTO_SMALL_ABOVE;
	public static final ModConfigSpec.IntValue AUTO_DETAILS_AT_MOST;
	// NeoForge asks the allowed values whether they contain a missing value (null); List.of throws on that, so the spec gets a copy.
	private static final List<String> MODE_KEYS = List.of("auto", "small", "medium", "details");
	private static final List<String> AUTO_MODE_KEYS = List.of("small", "medium", "details");

	static {
		ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
		SKIES_DISPLAY = builder
			.comment("Show the two-line item tooltip and the detail panel beside the JEI list.",
				"False restores the normal tooltips. Advanced tooltips (F3+H) always show the full tooltip.")
			.define("skiesDisplay", true);
		WRITE_STATS = builder
			.comment("Write every item's stats to skies-aero/item-stats.json in the instance when a world opens.")
			.define("writeStats", true);
		PANEL_WIDTH = builder
			.comment("Width of the detail panel, in GUI pixels.")
			.defineInRange("panelWidth", 128, 80, 240);
		VIEW_MODE = builder
			.comment("How JEI's ingredient list is drawn: auto, small (JEI's grid), medium (larger icons with names)",
				"or details (one row per entry). The view mode key cycles it. Auto picks by how many entries show.")
			.defineInList("viewMode", ViewMode.AUTO.key(), new ArrayList<>(MODE_KEYS));
		builder.comment("What Auto may choose, and where it switches.").push("customAuto");
		AUTO_MODES = builder
			.comment("The modes Auto may use: any of small, medium, details.")
			.defineListAllowEmpty("allowedModes", AUTO_MODE_KEYS, () -> "small",
				value -> value instanceof String text && AUTO_MODE_KEYS.contains(text));
		AUTO_SMALL_ABOVE = builder
			.comment("Auto shows small icons when more entries than this are showing.")
			.defineInRange("smallAbove", ViewModes.DEFAULT_SMALL_ABOVE, 0, 100000);
		AUTO_DETAILS_AT_MOST = builder
			.comment("Auto shows details when this many entries or fewer are showing; medium icons in between.")
			.defineInRange("detailsAtMost", ViewModes.DEFAULT_DETAILS_AT_MOST, 0, 100000);
		builder.pop();
		SPEC = builder.build();
	}

	private SkiesConfig() {}

	static boolean display() {
		return SPEC.isLoaded() && SKIES_DISPLAY.get();
	}

	static boolean writeStats() {
		return SPEC.isLoaded() && WRITE_STATS.get();
	}

	static int panelWidth() {
		return SPEC.isLoaded() ? PANEL_WIDTH.get() : 128;
	}

	static ViewMode viewMode() {
		return SPEC.isLoaded() ? ViewMode.parse(VIEW_MODE.get()) : ViewMode.AUTO;
	}

	/** Stores the chosen mode in the config file. */
	static void setViewMode(ViewMode mode) {
		if (!SPEC.isLoaded()) return;
		VIEW_MODE.set(mode.key());
		VIEW_MODE.save();
	}

	static ViewModes.AutoRule autoRule() {
		if (!SPEC.isLoaded()) return ViewModes.AutoRule.DEFAULT;
		List<String> modes = AUTO_MODES.get().stream().map(String::valueOf).toList();
		return ViewModes.AutoRule.of(modes, AUTO_SMALL_ABOVE.get(), AUTO_DETAILS_AT_MOST.get());
	}
}
