package com.starskyxiii.collapsible_groups.skies;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The panel's place, the view mode choice, the tile and row geometry and name clipping. */
class SkiesLayoutTest {
	private static final int CHAR = 6;

	@Test
	void panelSitsAtTheLeftEdgeTopAlignedWithTheContent() {
		Optional<GuiRect> area = PanelPlacement.leftColumn(480, 270, 152, 52, 128);
		assertEquals(Optional.of(new GuiRect(2, 52, 128, 270 - 2 - 52)), area);
	}

	@Test
	void panelIsLeftOutWhenTheLeftSpaceIsTooNarrow() {
		assertTrue(PanelPlacement.leftColumn(480, 270, 128 + 3, 52, 128).isEmpty(), "needs a gap each side");
		assertTrue(PanelPlacement.leftColumn(480, 270, 128 + 4, 52, 128).isPresent());
		assertTrue(PanelPlacement.leftColumn(480, 270, 0, 0, 128).isEmpty());
	}

	@Test
	void panelNeverStartsAboveTheScreenTop() {
		assertEquals(2, PanelPlacement.leftColumn(480, 270, 200, -10, 128).orElseThrow().y());
	}

	@Test
	void autoPicksByHowManyEntriesShow() {
		ViewModes.AutoRule rule = ViewModes.AutoRule.DEFAULT;
		assertEquals(ViewMode.SMALL, ViewModes.resolve(ViewMode.AUTO, 61, rule));
		assertEquals(ViewMode.MEDIUM, ViewModes.resolve(ViewMode.AUTO, 60, rule));
		assertEquals(ViewMode.MEDIUM, ViewModes.resolve(ViewMode.AUTO, 13, rule));
		assertEquals(ViewMode.DETAILS, ViewModes.resolve(ViewMode.AUTO, 12, rule));
		assertEquals(ViewMode.DETAILS, ViewModes.resolve(ViewMode.AUTO, 0, rule));
	}

	@Test
	void aChosenModeIsKeptWhateverTheCount() {
		assertEquals(ViewMode.DETAILS, ViewModes.resolve(ViewMode.DETAILS, 5000, ViewModes.AutoRule.DEFAULT));
		assertEquals(ViewMode.SMALL, ViewModes.resolve(ViewMode.SMALL, 1, ViewModes.AutoRule.DEFAULT));
	}

	@Test
	void autoFallsBackToTheNearestAllowedMode() {
		ViewModes.AutoRule noMedium = ViewModes.AutoRule.of(List.of("small", "details"), 60, 12);
		assertEquals(ViewMode.SMALL, ViewModes.resolve(ViewMode.AUTO, 30, noMedium));
		ViewModes.AutoRule noSmall = ViewModes.AutoRule.of(List.of("Medium", "details", "nonsense"), 60, 12);
		assertEquals(ViewMode.MEDIUM, ViewModes.resolve(ViewMode.AUTO, 500, noSmall));
		ViewModes.AutoRule detailsOnly = new ViewModes.AutoRule(Set.of(ViewMode.DETAILS), 60, 12);
		assertEquals(ViewMode.DETAILS, ViewModes.resolve(ViewMode.AUTO, 500, detailsOnly));
		ViewModes.AutoRule none = ViewModes.AutoRule.of(List.of(), 60, 12);
		assertEquals(ViewMode.SMALL, ViewModes.resolve(ViewMode.AUTO, 5, none));
		ViewModes.AutoRule custom = ViewModes.AutoRule.of(List.of("small", "medium", "details"), 100, 3);
		assertEquals(ViewMode.MEDIUM, ViewModes.resolve(ViewMode.AUTO, 90, custom));
		assertEquals(ViewMode.DETAILS, ViewModes.resolve(ViewMode.AUTO, 3, custom));
	}

	@Test
	void modesParseAndCycle() {
		assertEquals(ViewMode.MEDIUM, ViewMode.parse(" Medium "));
		assertEquals(ViewMode.AUTO, ViewMode.parse("huge"));
		assertEquals(ViewMode.SMALL, ViewMode.AUTO.next());
		assertEquals(ViewMode.AUTO, ViewMode.DETAILS.next());
	}

	@Test
	void mediumTilesAreTwoSlotsWideAndFillTheListRectangle() {
		GuiRect list = new GuiRect(300, 20, 9 * 18, 12 * 18);
		ListLayout.Layout layout = ListLayout.of(ViewMode.MEDIUM, list, List.of(), null);
		assertEquals(4, layout.columns());
		assertEquals(216 / ListLayout.MEDIUM_HEIGHT, layout.rows());
		assertEquals(layout.columns() * layout.rows(), layout.fits());
		GuiRect first = layout.cells().getFirst().area();
		assertEquals(new GuiRect(300 + (162 - 4 * 36) / 2, 20, 36, ListLayout.MEDIUM_HEIGHT), first);
		assertEquals(first.right(), layout.cells().get(1).area().x(), "tiles sit side by side");
	}

	@Test
	void detailsHasOneFullWidthRowPerEntry() {
		GuiRect list = new GuiRect(300, 20, 162, 10 * 18);
		ListLayout.Layout layout = ListLayout.of(ViewMode.DETAILS, list, List.of(), null);
		assertEquals(1, layout.columns());
		assertEquals(10, layout.rows());
		assertEquals(10, layout.fits());
		assertEquals(new GuiRect(300, 38, 162, 18), layout.cells().get(1).area());
	}

	@Test
	void cellsUnderAnotherModsAreaOrTheDropPointAreBlocked() {
		GuiRect list = new GuiRect(0, 0, 100, 5 * 18);
		ListLayout.Layout layout = ListLayout.of(ViewMode.DETAILS, list, List.of(new GuiRect(90, 40, 30, 10)),
			new ListLayout.Point(5, 80));
		List<Boolean> blocked = layout.cells().stream().map(ListLayout.Cell::blocked).toList();
		assertEquals(List.of(false, false, true, false, true), blocked);
		assertEquals(3, layout.fits());
	}

	@Test
	void noLayoutWhenNothingFitsOrTheModeIsJeisGrid() {
		assertNull(ListLayout.of(ViewMode.MEDIUM, new GuiRect(0, 0, 30, 200), List.of(), null));
		assertNull(ListLayout.of(ViewMode.DETAILS, new GuiRect(0, 0, 100, 10), List.of(), null));
		assertNull(ListLayout.of(ViewMode.SMALL, new GuiRect(0, 0, 100, 100), List.of(), null));
		assertNull(ListLayout.of(ViewMode.AUTO, new GuiRect(0, 0, 100, 100), List.of(), null));
	}

	@Test
	void detailsRowGivesTheModGroupAtMostTwoFifthsAndHidesItWhenTiny() {
		ListLayout.RowParts parts = ListLayout.row(100, 160, 12, 200);
		assertEquals(114, parts.rankRight());
		assertEquals(117, parts.iconX());
		assertEquals(135, parts.nameX());
		int room = 258 - 135;
		assertEquals(room * 2 / 5, parts.groupWidth());
		assertEquals(room - parts.groupWidth() - 4, parts.nameWidth());
		assertEquals(258, parts.groupRight());
		ListLayout.RowParts narrow = ListLayout.row(0, 50, 6, 40);
		assertEquals(0, narrow.groupWidth());
		assertEquals(50 - 2 - narrow.nameX(), narrow.nameWidth());
	}

	@Test
	void namesWrapAtSpacesAndEndWithAnEllipsisWhenCut() {
		assertEquals(List.of("Iron Ingot"), TextClip.lines("Iron Ingot", 100, 2, SkiesLayoutTest::width));
		assertEquals(List.of("Diamond", "Pickaxe"), TextClip.lines("Diamond Pickaxe", 60, 2, SkiesLayoutTest::width));
		List<String> cut = TextClip.lines("Netherite Upgrade Smithing Template", 60, 2, SkiesLayoutTest::width);
		assertEquals(2, cut.size());
		assertEquals("Netherite", cut.get(0));
		assertTrue(cut.get(1).endsWith(TextClip.ELLIPSIS));
		for (String line : cut) assertTrue(width(line) <= 60, line);
		assertEquals("Superc...", TextClip.line("Supercalifragilistic", 54, SkiesLayoutTest::width));
		assertFalse(TextClip.lines("", 60, 2, SkiesLayoutTest::width).iterator().hasNext());
	}

	private static int width(String text) {
		return text.length() * CHAR;
	}
}
