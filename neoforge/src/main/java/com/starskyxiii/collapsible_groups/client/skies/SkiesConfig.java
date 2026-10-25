package com.starskyxiii.collapsible_groups.client.skies;

import com.starskyxiii.collapsible_groups.skies.PanelPlacement;
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
			.comment("Show the detail panel between the screen and the JEI list, with the hovered item's full tooltip.",
				"False turns the panel off and reserves no space beside the list.")
			.define("skiesDisplay", true);
		WRITE_STATS = builder
			.comment("Write every item's stats to skies-aero/item-stats.json in the instance when a world opens.")
			.define("writeStats", true);
		PANEL_WIDTH = builder
			.comment("The most width of the detail panel, in GUI pixels. It is narrower when less room is free,",
				"and there is no panel when under " + PanelPlacement.MIN_WIDTH + " pixels are free.")
			.defineInRange("panelWidth", PanelPlacement.DEFAULT_MAX_WIDTH, PanelPlacement.MIN_WIDTH, 240);
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
		return SPEC.isLoaded() ? PANEL_WIDTH.get() : PanelPlacement.DEFAULT_MAX_WIDTH;
	}
}
