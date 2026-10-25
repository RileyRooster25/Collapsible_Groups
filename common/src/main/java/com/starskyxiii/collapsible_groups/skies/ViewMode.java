package com.starskyxiii.collapsible_groups.skies;

import java.util.Locale;

/** How JEI's ingredient list is drawn: JEI's grid, larger tiles with names, or one row per entry. */
public enum ViewMode {
	AUTO, SMALL, MEDIUM, DETAILS;

	/** The config spelling: lower case. */
	public String key() {
		return name().toLowerCase(Locale.ROOT);
	}

	/** The words shown to the player when the mode changes. */
	public String label() {
		return switch (this) {
			case AUTO -> "Auto";
			case SMALL -> "Small icons";
			case MEDIUM -> "Medium icons";
			case DETAILS -> "Details";
		};
	}

	/** The next mode when cycling with the key: Auto, Small, Medium, Details, then Auto again. */
	public ViewMode next() {
		ViewMode[] all = values();
		return all[(ordinal() + 1) % all.length];
	}

	/** Reads a config spelling, ignoring case; anything unknown is Auto. */
	public static ViewMode parse(String text) {
		if (text == null) return AUTO;
		for (ViewMode mode : values()) {
			if (mode.key().equalsIgnoreCase(text.trim())) return mode;
		}
		return AUTO;
	}
}
