package com.starskyxiii.collapsible_groups.skies;

import java.util.Optional;

/**
 * Where the detail panel sits: the left column of the screen, at its left edge, top-aligned with the
 * screen's content (a container, or JEI's recipe screen) and running to the bottom so it can grow downward.
 * Nothing is placed when the space left of the content is narrower than the panel plus a gap each side.
 */
public final class PanelPlacement {
	public static final int GAP = 2;

	private PanelPlacement() {}

	/** The rectangle the panel owns, or empty when it does not fit left of the content. */
	public static Optional<GuiRect> leftColumn(int screenWidth, int screenHeight, int contentLeft, int contentTop,
		int panelWidth) {
		if (panelWidth <= 0 || screenWidth <= 0 || screenHeight <= 0) return Optional.empty();
		int left = Math.min(contentLeft, screenWidth);
		if (left < panelWidth + 2 * GAP) return Optional.empty();
		int top = Math.max(GAP, Math.min(contentTop, screenHeight - GAP));
		int height = screenHeight - GAP - top;
		if (height <= 0) return Optional.empty();
		return Optional.of(new GuiRect(GAP, top, panelWidth, height));
	}
}
