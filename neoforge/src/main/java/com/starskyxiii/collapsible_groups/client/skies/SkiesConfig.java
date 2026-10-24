package com.starskyxiii.collapsible_groups.client.skies;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The Skies Aero display settings, in config/collapsiblegroups/skies.toml. */
public final class SkiesConfig {
	public static final ModConfigSpec SPEC;
	public static final ModConfigSpec.BooleanValue SKIES_DISPLAY;
	public static final ModConfigSpec.BooleanValue WRITE_STATS;
	public static final ModConfigSpec.IntValue PANEL_WIDTH;

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
}
