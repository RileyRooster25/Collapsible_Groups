package com.starskyxiii.collapsible_groups.skies;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Decides which concrete mode the list is drawn in. A chosen mode other than Auto is used as it is.
 * Auto looks at how many entries JEI is showing: Small above {@code smallAbove}, Details at
 * {@code detailsAtMost} or fewer, Medium between. When the picked mode is not allowed, the nearest
 * allowed mode toward Small is used, then toward Details; with nothing allowed it is Small.
 */
public final class ViewModes {
	public static final int DEFAULT_SMALL_ABOVE = 60;
	public static final int DEFAULT_DETAILS_AT_MOST = 12;
	private static final List<ViewMode> BY_DENSITY = List.of(ViewMode.SMALL, ViewMode.MEDIUM, ViewMode.DETAILS);

	private ViewModes() {}

	/** What Auto may choose and where it switches. */
	public record AutoRule(Set<ViewMode> allowed, int smallAbove, int detailsAtMost) {
		public static final AutoRule DEFAULT = new AutoRule(
			EnumSet.of(ViewMode.SMALL, ViewMode.MEDIUM, ViewMode.DETAILS), DEFAULT_SMALL_ABOVE, DEFAULT_DETAILS_AT_MOST);

		public AutoRule {
			EnumSet<ViewMode> copy = EnumSet.noneOf(ViewMode.class);
			for (ViewMode mode : allowed) {
				if (mode != ViewMode.AUTO) copy.add(mode);
			}
			allowed = Set.copyOf(copy);
		}

		/** Reads the allowed modes from config spellings; unknown words and "auto" are ignored. */
		public static AutoRule of(List<String> allowed, int smallAbove, int detailsAtMost) {
			EnumSet<ViewMode> modes = EnumSet.noneOf(ViewMode.class);
			for (String word : allowed) {
				for (ViewMode mode : BY_DENSITY) {
					if (mode.key().equalsIgnoreCase(word == null ? "" : word.trim())) modes.add(mode);
				}
			}
			return new AutoRule(modes, smallAbove, detailsAtMost);
		}
	}

	/** The concrete mode (never Auto) for the chosen mode and the number of entries showing. */
	public static ViewMode resolve(ViewMode chosen, int entries, AutoRule rule) {
		if (chosen != ViewMode.AUTO) return chosen;
		ViewMode picked;
		if (entries > rule.smallAbove()) picked = ViewMode.SMALL;
		else if (entries <= rule.detailsAtMost()) picked = ViewMode.DETAILS;
		else picked = ViewMode.MEDIUM;
		if (rule.allowed().contains(picked)) return picked;
		int index = BY_DENSITY.indexOf(picked);
		for (int i = index - 1; i >= 0; i--) {
			if (rule.allowed().contains(BY_DENSITY.get(i))) return BY_DENSITY.get(i);
		}
		for (int i = index + 1; i < BY_DENSITY.size(); i++) {
			if (rule.allowed().contains(BY_DENSITY.get(i))) return BY_DENSITY.get(i);
		}
		return ViewMode.SMALL;
	}
}
