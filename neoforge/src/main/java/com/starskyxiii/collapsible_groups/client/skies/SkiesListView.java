package com.starskyxiii.collapsible_groups.client.skies;

import com.starskyxiii.collapsible_groups.Constants;
import com.starskyxiii.collapsible_groups.compat.jei.element.GroupIcon;
import com.starskyxiii.collapsible_groups.compat.jei.element.PreRenderIngredientGridElement;
import com.starskyxiii.collapsible_groups.compat.jei.runtime.JeiRuntimeHolder;
import com.starskyxiii.collapsible_groups.compat.jei.ui.GroupThemeResolver;
import com.starskyxiii.collapsible_groups.platform.Services;
import com.starskyxiii.collapsible_groups.skies.GuiRect;
import com.starskyxiii.collapsible_groups.skies.ListLayout;
import com.starskyxiii.collapsible_groups.skies.ModGroups;
import com.starskyxiii.collapsible_groups.skies.TextClip;
import com.starskyxiii.collapsible_groups.skies.ViewMode;
import com.starskyxiii.collapsible_groups.skies.ViewModes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.common.util.ImmutablePoint2i;
import mezz.jei.common.util.ImmutableRect2i;
import mezz.jei.gui.overlay.elements.IElement;
import mezz.jei.gui.overlay.ingredients.IngredientGrid;
import mezz.jei.gui.overlay.ingredients.IngredientListRenderer;
import mezz.jei.gui.overlay.ingredients.IngredientListSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Medium and Details modes in JEI's ingredient list. JEI keeps doing everything it already does (paging,
 * the mouse wheel, page arrows, tooltips, left click for recipes, right click for uses, the bookmark key,
 * dragging), because the list's slots are replaced by larger tiles or full-width rows of the same kind:
 * each JEI slot simply covers more of the screen. This class lays those slots out (the geometry comes from
 * {@link ListLayout}) and draws them; Small mode leaves JEI's grid untouched.
 */
public final class SkiesListView {
	private static final int SLOT_PADDING = 1;
	private static final int NAME = 0xFFFFFFFF;
	private static final int RANK = 0xFFA0A0A0;
	private static final int MOD_GROUP = 0xFF8888AA;
	private static final Map<IElement<?>, Labels> LABELS = new IdentityHashMap<>();

	private static @Nullable List<IElement<?>> elements;
	private static @Nullable List<IElement<?>> countedList;
	private static int countedSize = -1;
	private static int countedVisible;
	private static @Nullable GridFields fields;
	private static boolean fieldsFailed;
	private static boolean drawFailureLogged;

	private SkiesListView() {}

	/** Implemented on JEI's list renderer by the renderer hook: which mode that renderer draws in. */
	public interface ModeHolder {
		void cg$setSkiesMode(ViewMode mode);
	}

	/** Called with every list JEI's ingredient filter hands out, so Auto can count what is showing. */
	public static void noteElements(@Nullable List<IElement<?>> list) {
		elements = list;
	}

	/** How many entries JEI is showing after its search filter: the visible elements of its list. */
	static int visibleCount() {
		List<IElement<?>> list = elements;
		if (list == null) return 0;
		if (list != countedList || list.size() != countedSize) {
			int visible = 0;
			for (IElement<?> element : list) {
				if (element.isVisible()) visible++;
			}
			countedList = list;
			countedSize = list.size();
			countedVisible = visible;
		}
		return countedVisible;
	}

	/**
	 * Lays the grid's slots out for the current mode, right after JEI laid out its own. Returns the layout
	 * (for the grid's column and row counts), or null when JEI's grid stays: Small mode, the bookmark list,
	 * a mode that does not fit, or a JEI whose grid this cannot read.
	 */
	public static @Nullable ListLayout.Layout apply(IngredientGrid grid) {
		GridFields access = fields();
		if (access == null) return null;
		try {
			if (!access.searchable(grid)) return null;
			IngredientListRenderer renderer = access.renderer(grid);
			ViewMode mode = ViewModes.resolve(SkiesViewState.chosen(), visibleCount(), SkiesViewState.autoRule());
			ListLayout.Layout layout = mode == ViewMode.SMALL ? null
				: ListLayout.of(mode, rect(access.area(grid)), rects(access.exclusions(grid)), point(access.mouseExclusion(grid)));
			LABELS.clear();
			if (layout == null) {
				if (renderer instanceof ModeHolder holder) holder.cg$setSkiesMode(ViewMode.SMALL);
				return null;
			}
			renderer.clear();
			for (ListLayout.Cell cell : layout.cells()) {
				GuiRect area = cell.area();
				IngredientListSlot slot = new IngredientListSlot(area.x(), area.y(), area.width(), area.height(), SLOT_PADDING);
				slot.setBlocked(cell.blocked());
				renderer.add(slot);
			}
			access.setVisibleSlotCount(grid, layout.fits());
			if (renderer instanceof ModeHolder holder) holder.cg$setSkiesMode(layout.mode());
			return layout;
		} catch (ReflectiveOperationException | RuntimeException failure) {
			fieldsFailed = true;
			fields = null;
			Constants.LOG.warn("[CollapsibleGroups] View modes are off: could not lay out JEI's list: {}", failure.toString());
			return null;
		}
	}

	/** The rank of the first entry on the page: one more than the visible entries before it. */
	public static int firstRank(List<IElement<?>> list, int startIndex) {
		int rank = 1;
		int end = Math.min(startIndex, list.size());
		for (int i = 0; i < end; i++) {
			if (list.get(i).isVisible()) rank++;
		}
		LABELS.clear();
		return rank;
	}

	/** Draws the page in Medium or Details mode, in place of JEI's batch of 16px icons. */
	public static void render(GuiGraphics graphics, List<IngredientListSlot> slots, ViewMode mode, int firstRank) {
		IJeiRuntime runtime = JeiRuntimeHolder.get();
		if (runtime == null) return;
		IIngredientManager manager = runtime.getIngredientManager();
		Font font = Minecraft.getInstance().font;
		int filled = 0;
		for (IngredientListSlot slot : slots) {
			if (!slot.isBlocked() && slot.getElement() != null) filled++;
		}
		int rankWidth = font.width(String.valueOf(firstRank + Math.max(0, filled - 1)));
		int rank = firstRank;
		for (IngredientListSlot slot : slots) {
			if (slot.isBlocked()) continue;
			IElement<?> element = slot.getElement();
			if (element == null) continue;
			ImmutableRect2i area = slot.getArea();
			try {
				if (mode == ViewMode.MEDIUM) drawTile(graphics, font, manager, element, area);
				else drawRow(graphics, font, manager, element, area, rank, rankWidth);
			} catch (RuntimeException | LinkageError failure) {
				if (!drawFailureLogged) {
					drawFailureLogged = true;
					Constants.LOG.warn("[CollapsibleGroups] Could not draw a list entry: {}", failure.toString());
				}
			}
			rank++;
		}
	}

	private static void drawTile(GuiGraphics graphics, Font font, IIngredientManager manager, IElement<?> element,
		ImmutableRect2i area) {
		int iconX = area.x() + (area.width() - ListLayout.MEDIUM_ICON) / 2;
		int iconY = area.y() + 2;
		drawIcon(graphics, manager, element, iconX, iconY, 2f);
		Labels labels = labels(font, manager, element, ViewMode.MEDIUM, area.width(), 0);
		int lineY = iconY + ListLayout.MEDIUM_ICON + 1;
		for (String line : labels.nameLines()) {
			float width = font.width(line) * ListLayout.LABEL_SCALE;
			graphics.pose().pushPose();
			graphics.pose().translate(area.x() + (area.width() - width) / 2f, lineY, 200);
			graphics.pose().scale(ListLayout.LABEL_SCALE, ListLayout.LABEL_SCALE, 1f);
			graphics.drawString(font, line, 0, 0, labels.nameColor(), true);
			graphics.pose().popPose();
			lineY += ListLayout.LABEL_LINE_HEIGHT;
		}
	}

	private static void drawRow(GuiGraphics graphics, Font font, IIngredientManager manager, IElement<?> element,
		ImmutableRect2i area, int rank, int rankWidth) {
		Labels labels = labels(font, manager, element, ViewMode.DETAILS, area.width(), rankWidth);
		ListLayout.RowParts parts = labels.parts();
		int x = area.x();
		int textY = area.y() + (area.height() - font.lineHeight) / 2 + 1;
		String rankText = String.valueOf(rank);
		graphics.drawString(font, rankText, x + parts.rankRight() - font.width(rankText), textY, RANK, true);
		drawIcon(graphics, manager, element, x + parts.iconX(), area.y() + 1, 1f);
		if (!labels.nameLines().isEmpty()) {
			graphics.drawString(font, labels.nameLines().getFirst(), x + parts.nameX(), textY, labels.nameColor(), true);
		}
		if (!labels.group().isEmpty()) {
			graphics.drawString(font, labels.group(), x + parts.groupRight() - font.width(labels.group()), textY, MOD_GROUP, true);
		}
	}

	private static void drawIcon(GuiGraphics graphics, IIngredientManager manager, IElement<?> element, int x, int y, float scale) {
		if (element instanceof PreRenderIngredientGridElement background) {
			graphics.pose().pushPose();
			graphics.pose().translate(x, y, 0);
			graphics.pose().scale(scale, scale, 1f);
			background.drawPreRender(graphics, 0, 0);
			graphics.pose().popPose();
		}
		drawIngredient(graphics, manager, element.getTypedIngredient(), x, y, scale);
	}

	private static <T> void drawIngredient(GuiGraphics graphics, IIngredientManager manager, ITypedIngredient<T> typed,
		int x, int y, float scale) {
		IIngredientRenderer<T> renderer = manager.getIngredientRenderer(typed.getType());
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0);
		graphics.pose().scale(scale, scale, 1f);
		renderer.render(graphics, typed.getIngredient());
		graphics.pose().popPose();
	}

	/**
	 * The clipped text for an entry, worked out once per page rather than every frame. Row parts are
	 * relative to the row's left edge.
	 */
	private record Labels(List<String> nameLines, int nameColor, String group, ListLayout.@Nullable RowParts parts,
		int width, int rankWidth) {}

	private static Labels labels(Font font, IIngredientManager manager, IElement<?> element, ViewMode mode, int width,
		int rankWidth) {
		Labels cached = LABELS.get(element);
		if (cached != null && cached.width() == width && cached.rankWidth() == rankWidth
			&& (cached.parts() == null) == (mode == ViewMode.MEDIUM)) {
			return cached;
		}
		ITypedIngredient<?> typed = element.getTypedIngredient();
		String name;
		String group;
		int color = NAME;
		if (typed.getIngredient() instanceof GroupIcon icon) {
			name = icon.resolvedDisplayName();
			group = "";
			color = 0xFF000000 | GroupThemeResolver.groupNameColor(icon.groupId());
		} else {
			name = displayName(manager, typed);
			group = ModGroups.name(modId(manager, typed), Services.PLATFORM::getModName);
		}
		Labels labels;
		if (mode == ViewMode.MEDIUM) {
			int room = (int) ((width - 2) / ListLayout.LABEL_SCALE);
			labels = new Labels(TextClip.lines(name, room, ListLayout.LABEL_LINES, font::width), color, "", null, width, rankWidth);
		} else {
			ListLayout.RowParts parts = ListLayout.row(0, width, rankWidth, group.isEmpty() ? 0 : font.width(group));
			String shownGroup = parts.groupWidth() > 0 ? TextClip.line(group, parts.groupWidth(), font::width) : "";
			String clipped = TextClip.line(name, parts.nameWidth(), font::width);
			labels = new Labels(clipped.isEmpty() ? List.of() : List.of(clipped), color, shownGroup, parts, width, rankWidth);
		}
		LABELS.put(element, labels);
		return labels;
	}

	private static <T> String displayName(IIngredientManager manager, ITypedIngredient<T> typed) {
		IIngredientHelper<T> helper = manager.getIngredientHelper(typed.getType());
		return helper.getDisplayName(typed.getIngredient());
	}

	private static <T> String modId(IIngredientManager manager, ITypedIngredient<T> typed) {
		IIngredientHelper<T> helper = manager.getIngredientHelper(typed.getType());
		return helper.getDisplayModId(typed.getIngredient());
	}

	private static GuiRect rect(ImmutableRect2i area) {
		return new GuiRect(area.x(), area.y(), area.width(), area.height());
	}

	private static List<GuiRect> rects(Set<ImmutableRect2i> areas) {
		List<GuiRect> list = new ArrayList<>(areas.size());
		for (ImmutableRect2i area : areas) list.add(rect(area));
		return list;
	}

	private static ListLayout.@Nullable Point point(@Nullable ImmutablePoint2i point) {
		return point == null ? null : new ListLayout.Point(point.x(), point.y());
	}

	private static @Nullable GridFields fields() {
		if (fields != null || fieldsFailed) return fields;
		try {
			fields = new GridFields(
				field("searchable"), field("ingredientListRenderer"), field("area"),
				field("guiExclusionAreas"), field("mouseExclusionPoint"), field("visibleSlotCount"));
		} catch (ReflectiveOperationException | RuntimeException failure) {
			fieldsFailed = true;
			Constants.LOG.warn("[CollapsibleGroups] View modes are off: this JEI's list grid is not the one they were built for: {}",
				failure.toString());
		}
		return fields;
	}

	private static Field field(String name) throws NoSuchFieldException {
		Field field = IngredientGrid.class.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	/**
	 * JEI's private grid state, read by name. Read rather than shadowed, so a JEI that renamed one turns
	 * view modes off with a log line instead of stopping the game from starting.
	 */
	private record GridFields(Field searchable, Field renderer, Field area, Field exclusions, Field mouseExclusion,
		Field visibleSlotCount) {
		boolean searchable(IngredientGrid grid) throws IllegalAccessException {
			return searchable.getBoolean(grid);
		}

		IngredientListRenderer renderer(IngredientGrid grid) throws IllegalAccessException {
			return (IngredientListRenderer) renderer.get(grid);
		}

		ImmutableRect2i area(IngredientGrid grid) throws IllegalAccessException {
			return (ImmutableRect2i) area.get(grid);
		}

		@SuppressWarnings("unchecked")
		Set<ImmutableRect2i> exclusions(IngredientGrid grid) throws IllegalAccessException {
			return (Set<ImmutableRect2i>) exclusions.get(grid);
		}

		@Nullable ImmutablePoint2i mouseExclusion(IngredientGrid grid) throws IllegalAccessException {
			return (ImmutablePoint2i) mouseExclusion.get(grid);
		}

		void setVisibleSlotCount(IngredientGrid grid, int count) throws IllegalAccessException {
			visibleSlotCount.setInt(grid, count);
		}
	}
}
