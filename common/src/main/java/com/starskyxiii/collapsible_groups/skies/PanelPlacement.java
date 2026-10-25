package com.starskyxiii.collapsible_groups.skies;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Where the detail panel docks: between the screen's content and JEI's ingredient list, in space the list
 * is not using, so the list keeps every column it would have without the panel. Pure geometry, in GUI pixels.
 *
 * <p>The JEI numbers mirror JEI's own layout (19.42 and 19.57 alike): the list's display area is everything
 * right of the gui's right edge, inset by a 6 px border margin (and 7 px more when JEI draws a background),
 * slots are 18 px, the columns are capped by JEI's max columns, and JEI blocks any slot that comes within
 * 2 px of an exclusion area. The search row is 20 px tall, 6 px above the bottom of the screen.
 */
public final class PanelPlacement {
	/** Space between the content's right edge and the panel; keeps recipe transfer buttons clear. */
	public static final int GAP = 4;
	/** JEI blocks a slot whose area grown by this much touches an exclusion area. */
	public static final int LIST_CLEARANCE = 2;
	/** Below this width there is no panel on the screen, and nothing is reserved. */
	public static final int MIN_WIDTH = 100;
	/** The default most width; skies.toml panelWidth changes it. */
	public static final int DEFAULT_MAX_WIDTH = 140;
	public static final int SLOT = 18;
	public static final int JEI_BORDER_MARGIN = 6;
	public static final int JEI_BACKGROUND_INSET = 7;
	public static final int JEI_SEARCH_HEIGHT = 20;
	/** Space kept between the panel's bottom and the search row. */
	public static final int SEARCH_CLEARANCE = 2;

	public enum Alignment { LEFT, CENTER, RIGHT }

	public record Rect(int x, int y, int width, int height) {
		public int right() {
			return x + width;
		}

		public int bottom() {
			return y + height;
		}
	}

	private PanelPlacement() {}

	/**
	 * The content's right edge: the gui's right edge, carried over any extra area the screen registers that
	 * touches it and overlaps the gui vertically (side tabs). Areas chained edge to edge are all taken in.
	 */
	public static int contentRight(int guiRight, int guiTop, int guiBottom, List<Rect> extraAreas) {
		int right = guiRight;
		boolean grew = true;
		while (grew) {
			grew = false;
			for (Rect area : extraAreas) {
				boolean touches = area.x() <= right && area.right() > right;
				boolean beside = area.y() < guiBottom && area.bottom() > guiTop;
				if (touches && beside) {
					right = area.right();
					grew = true;
				}
			}
		}
		return right;
	}

	/** The top of JEI's search row, when the search bar sits under the list. */
	public static int searchRowTop(int screenHeight) {
		return screenHeight - JEI_BORDER_MARGIN - JEI_SEARCH_HEIGHT;
	}

	/**
	 * Where JEI puts the list's first column when there is no panel; empty when the list has no column.
	 * Side tabs right of the gui (contentRight beyond guiRight) push the start right, as JEI's exclusion
	 * areas do.
	 */
	public static OptionalInt listLeftEdge(int screenWidth, int guiRight, int contentRight, int maxColumns,
										   boolean drawBackground, Alignment alignment) {
		int inset = JEI_BORDER_MARGIN + (drawBackground ? JEI_BACKGROUND_INSET : 0);
		int start = guiRight + inset;
		if (contentRight > guiRight) start = Math.max(start, contentRight + LIST_CLEARANCE);
		int end = screenWidth - inset;
		int columns = Math.min((end - start) / SLOT, maxColumns);
		if (end <= start || columns <= 0) return OptionalInt.empty();
		int width = columns * SLOT;
		return OptionalInt.of(switch (alignment) {
			case LEFT -> start;
			case CENTER -> start + Math.round((end - start - width) / 2f);
			case RIGHT -> end - width;
		});
	}

	/**
	 * The panel's reserved rectangle: from 4 px right of the content to 2 px before the list's first column,
	 * at most {@code maxWidth} wide, from the content's top to just above the search row. Empty when that
	 * leaves under {@link #MIN_WIDTH}, or no height.
	 */
	public static Optional<Rect> dockBetween(int contentRight, int contentTop, int searchRowTop, int listLeftEdge,
											 int maxWidth) {
		int x0 = contentRight + GAP;
		int free = (listLeftEdge - LIST_CLEARANCE) - x0;
		int width = Math.min(Math.max(maxWidth, MIN_WIDTH), free);
		int top = Math.max(0, contentTop);
		int height = (searchRowTop - SEARCH_CLEARANCE) - top;
		if (width < MIN_WIDTH || height <= 0) return Optional.empty();
		return Optional.of(new Rect(x0, top, width, height));
	}
}
