package com.starskyxiii.collapsible_groups.skies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The geometry of JEI's ingredient list in Medium and Details modes: where each tile or row sits inside
 * the list rectangle JEI computed, which of them are blocked by other mods' screen areas (the same rule
 * JEI uses for its own slots), and so how many entries fit on a page. Small mode is JEI's own grid and
 * has no layout here.
 */
public final class ListLayout {
	public static final int SLOT = 18;
	public static final int MEDIUM_WIDTH = 2 * SLOT;
	public static final int MEDIUM_ICON = 32;
	public static final int LABEL_LINES = 2;
	public static final float LABEL_SCALE = 0.75f;
	public static final int LABEL_LINE_HEIGHT = 7;
	public static final int MEDIUM_HEIGHT = 2 + MEDIUM_ICON + 1 + LABEL_LINES * LABEL_LINE_HEIGHT + 1;
	public static final int DETAILS_HEIGHT = SLOT;
	private static final int EXCLUSION_MARGIN = 2;
	private static final int ROW_PADDING = 2;
	private static final int MIN_GROUP_WIDTH = 12;

	private ListLayout() {}

	/** One tile or row; a blocked one is skipped when entries are placed. */
	public record Cell(GuiRect area, boolean blocked) {}

	/** The cells in reading order, and the grid shape JEI's scrolling uses. */
	public record Layout(ViewMode mode, int columns, int rows, List<Cell> cells) {
		/** How many entries fit on one page: the cells nothing blocks. */
		public int fits() {
			int count = 0;
			for (Cell cell : cells) {
				if (!cell.blocked()) count++;
			}
			return count;
		}
	}

	/** A point JEI keeps clear of slots (where a dragged ingredient was dropped), or none. */
	public record Point(int x, int y) {}

	/**
	 * The layout for a concrete mode inside the list rectangle, or null when the mode is Small or Auto,
	 * or when not one tile or row fits (the caller then keeps JEI's grid).
	 */
	public static Layout of(ViewMode mode, GuiRect area, Collection<GuiRect> exclusions, Point mouseExclusion) {
		if (area.isEmpty()) return null;
		int width;
		int height;
		switch (mode) {
			case MEDIUM -> {
				width = MEDIUM_WIDTH;
				height = MEDIUM_HEIGHT;
			}
			case DETAILS -> {
				width = area.width();
				height = DETAILS_HEIGHT;
			}
			default -> {
				return null;
			}
		}
		int columns = area.width() / width;
		int rows = area.height() / height;
		if (columns <= 0 || rows <= 0) return null;
		int left = area.x() + (area.width() - columns * width) / 2;
		List<Cell> cells = new ArrayList<>(columns * rows);
		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < columns; column++) {
				GuiRect cell = new GuiRect(left + column * width, area.y() + row * height, width, height);
				cells.add(new Cell(cell, blocked(cell, exclusions, mouseExclusion)));
			}
		}
		return new Layout(mode, columns, rows, List.copyOf(cells));
	}

	private static boolean blocked(GuiRect cell, Collection<GuiRect> exclusions, Point mouseExclusion) {
		GuiRect expanded = new GuiRect(cell.x() - EXCLUSION_MARGIN, cell.y() - EXCLUSION_MARGIN,
			cell.width() + 2 * EXCLUSION_MARGIN, cell.height() + 2 * EXCLUSION_MARGIN);
		for (GuiRect exclusion : exclusions) {
			if (expanded.intersects(exclusion)) return true;
		}
		return mouseExclusion != null && cell.contains(mouseExclusion.x(), mouseExclusion.y());
	}

	/** Where the parts of one Details row go. A group width of 0 means the mod group is not shown. */
	public record RowParts(int rankRight, int iconX, int nameX, int nameWidth, int groupRight, int groupWidth) {}

	/**
	 * Splits a Details row: the rank right-aligned in a column as wide as the widest rank on the page,
	 * the 16px icon, the name, and the mod group right-aligned, given at most two fifths of the text room.
	 */
	public static RowParts row(int rowX, int rowWidth, int rankColumnWidth, int groupTextWidth) {
		int rankRight = rowX + ROW_PADDING + rankColumnWidth;
		int iconX = rankRight + 3;
		int nameX = iconX + SLOT;
		int right = rowX + rowWidth - ROW_PADDING;
		int room = Math.max(0, right - nameX);
		int group = Math.min(groupTextWidth, room * 2 / 5);
		if (group < MIN_GROUP_WIDTH) group = 0;
		int name = group == 0 ? room : Math.max(0, room - group - 4);
		return new RowParts(rankRight, iconX, nameX, name, right, group);
	}
}
