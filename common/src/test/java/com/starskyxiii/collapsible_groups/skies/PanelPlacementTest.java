package com.starskyxiii.collapsible_groups.skies;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Placement F: the panel docks between the content and JEI's list, only in space the list is not using. */
class PanelPlacementTest {
	private static final int GUI_W = 176;
	private static final int GUI_H = 166;

	private record Screen(int width, int height, int guiLeft, int guiTop) {
		static Screen inventory(int width, int height) {
			return new Screen(width, height, (width - GUI_W) / 2, (height - GUI_H) / 2);
		}

		int guiRight() {
			return guiLeft + GUI_W;
		}

		int guiBottom() {
			return guiTop + GUI_H;
		}
	}

	private static Optional<PanelPlacement.Rect> place(Screen screen, List<PanelPlacement.Rect> extraAreas) {
		int contentRight = PanelPlacement.contentRight(screen.guiRight(), screen.guiTop(), screen.guiBottom(), extraAreas);
		OptionalInt listLeft = PanelPlacement.listLeftEdge(screen.width(), screen.guiRight(), contentRight, 9, false,
			PanelPlacement.Alignment.RIGHT);
		if (listLeft.isEmpty()) return Optional.empty();
		return PanelPlacement.dockBetween(contentRight, screen.guiTop(), PanelPlacement.searchRowTop(screen.height()),
			listLeft.getAsInt(), PanelPlacement.DEFAULT_MAX_WIDTH);
	}

	@Test
	void theListLeftEdgeIsWhereJeiPutsNineRightAlignedColumns() {
		Screen screen = Screen.inventory(960, 540);
		OptionalInt left = PanelPlacement.listLeftEdge(960, screen.guiRight(), screen.guiRight(), 9, false,
			PanelPlacement.Alignment.RIGHT);
		assertEquals(960 - 6 - 9 * 18, left.getAsInt());
		OptionalInt withBackground = PanelPlacement.listLeftEdge(960, screen.guiRight(), screen.guiRight(), 9, true,
			PanelPlacement.Alignment.RIGHT);
		assertEquals(960 - 13 - 9 * 18, withBackground.getAsInt());
	}

	@Test
	void docksAtTheMostWidthWhenTheGapIsWide() {
		Screen screen = Screen.inventory(960, 540);
		PanelPlacement.Rect panel = place(screen, List.of()).orElseThrow();
		assertEquals(screen.guiRight() + 4, panel.x());
		assertEquals(140, panel.width());
		assertEquals(screen.guiTop(), panel.y());
		assertEquals(540 - 26 - 2, panel.bottom());
		int listLeft = 960 - 6 - 9 * 18;
		assertTrue(panel.right() <= listLeft - 2, "the list keeps all nine columns");
	}

	@Test
	void docksNarrowerWhenTheGapIsBetweenTheMinimumAndTheMost() {
		Screen screen = Screen.inventory(764, 480);
		PanelPlacement.Rect panel = place(screen, List.of()).orElseThrow();
		assertEquals(120, panel.width());
		assertEquals(764 - 6 - 9 * 18 - 2, panel.right(), "the panel ends 2 px before the first column");
	}

	@Test
	void noPanelWhenTheGapIsUnderTheMinimum() {
		assertTrue(place(Screen.inventory(640, 360), List.of()).isEmpty());
		assertTrue(place(Screen.inventory(480, 270), List.of()).isEmpty());
		assertTrue(PanelPlacement.dockBetween(100, 20, 300, 100 + 4 + 2 + 99, 140).isEmpty());
		assertEquals(100, PanelPlacement.dockBetween(100, 20, 300, 100 + 4 + 2 + 100, 140).orElseThrow().width());
	}

	@Test
	void sideTabsMoveThePanelRightOfTheTabs() {
		Screen screen = Screen.inventory(960, 540);
		PanelPlacement.Rect tabs = new PanelPlacement.Rect(screen.guiRight(), screen.guiTop() + 10, 22, 60);
		PanelPlacement.Rect panel = place(screen, List.of(tabs)).orElseThrow();
		assertEquals(screen.guiRight() + 22 + 4, panel.x());
		assertEquals(140, panel.width());
	}

	@Test
	void sideTabsCanLeaveTooLittleRoomForAPanel() {
		Screen screen = Screen.inventory(764, 480);
		PanelPlacement.Rect tabs = new PanelPlacement.Rect(screen.guiRight(), screen.guiTop() + 10, 22, 60);
		assertTrue(place(screen, List.of(tabs)).isEmpty());
	}

	@Test
	void chainedTabsAreAllTakenInAndAreasAwayFromTheEdgeAreNot() {
		PanelPlacement.Rect first = new PanelPlacement.Rect(200, 50, 20, 20);
		PanelPlacement.Rect second = new PanelPlacement.Rect(220, 80, 30, 20);
		PanelPlacement.Rect apart = new PanelPlacement.Rect(260, 50, 30, 20);
		PanelPlacement.Rect above = new PanelPlacement.Rect(200, 0, 50, 10);
		assertEquals(250, PanelPlacement.contentRight(200, 40, 200, List.of(second, first, apart, above)));
	}
}
