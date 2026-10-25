package com.starskyxiii.collapsible_groups.client.skies;

import com.starskyxiii.collapsible_groups.skies.PanelPlacement;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Everything the panel asks JEI, through JEI's public API only: the gui rectangle of the open screen,
 * the extra areas other handlers register on it, whether the list shows, and the ingredient under the mouse.
 * Only loaded once JEI has started this plugin.
 */
final class SkiesJei {
	/** How often, in client ticks, a screen's reserved rectangle is worked out again (side tabs can change). */
	private static final int RECHECK_TICKS = 20;

	private static volatile @Nullable IJeiRuntime runtime;
	/** True while this class asks JEI for the screen's extra areas, so the panel's own handler answers none. */
	private static boolean asking;
	private static @Nullable Key cachedKey;
	private static @Nullable Rect2i cachedArea;
	private static long cachedTick = Long.MIN_VALUE;
	private static @Nullable ListConfig listConfig;

	private SkiesJei() {}

	static void setRuntime(@Nullable IJeiRuntime value) {
		runtime = value;
		cachedKey = null;
		cachedArea = null;
	}

	static boolean asking() {
		return asking;
	}

	/**
	 * The panel's reserved rectangle on this screen, or null for no panel. Worked out from the screen's geometry
	 * only (never from the hovered item or whether the list shows), and kept until the geometry changes.
	 */
	static @Nullable Rect2i reservedArea(@Nullable Screen screen, int maxWidth, long tick) {
		IJeiRuntime jei = runtime;
		if (jei == null || screen == null) return null;
		Optional<IGuiProperties> found = jei.getScreenHelper().getGuiProperties(screen);
		if (found.isEmpty()) return null;
		IGuiProperties gui = found.get();
		Key key = new Key(screen, gui.screenWidth(), gui.screenHeight(), gui.guiLeft(), gui.guiTop(), gui.guiXSize(),
			gui.guiYSize(), maxWidth);
		if (key.equals(cachedKey) && tick - cachedTick < RECHECK_TICKS && tick >= cachedTick) return cachedArea;
		cachedKey = key;
		cachedTick = tick;
		cachedArea = compute(jei, screen, gui, maxWidth);
		return cachedArea;
	}

	private static @Nullable Rect2i compute(IJeiRuntime jei, Screen screen, IGuiProperties gui, int maxWidth) {
		List<PanelPlacement.Rect> extras = new ArrayList<>();
		asking = true;
		try {
			jei.getScreenHelper().getGuiExclusionAreas(screen)
				.forEach(area -> extras.add(new PanelPlacement.Rect(area.getX(), area.getY(), area.getWidth(), area.getHeight())));
		} finally {
			asking = false;
		}
		int contentRight = PanelPlacement.contentRight(gui.guiRight(), gui.guiTop(), gui.guiBottom(), extras);
		ListConfig config = listConfig();
		OptionalInt listLeft = PanelPlacement.listLeftEdge(gui.screenWidth(), gui.guiRight(), contentRight,
			config.maxColumns(), config.drawBackground(), config.alignment());
		if (listLeft.isEmpty()) return null;
		return PanelPlacement.dockBetween(contentRight, gui.guiTop(), PanelPlacement.searchRowTop(gui.screenHeight()),
				listLeft.getAsInt(), maxWidth)
			.map(rect -> new Rect2i(rect.x(), rect.y(), rect.width(), rect.height()))
			.orElse(null);
	}

	/** Whether JEI's ingredient list is drawn now. Used only to decide drawing, never the reserved rectangle. */
	static boolean listDisplayed() {
		IJeiRuntime jei = runtime;
		return jei != null && jei.getIngredientListOverlay().isListDisplayed();
	}

	/** The item under the mouse in a recipe screen, JEI's list or the bookmarks; empty when none. */
	static ItemStack hoveredItem(Screen screen) {
		IJeiRuntime jei = runtime;
		if (jei == null) return ItemStack.EMPTY;
		if (screen instanceof IRecipesGui recipes) {
			Optional<ItemStack> inRecipe = recipes.getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
			if (inRecipe.isPresent() && !inRecipe.get().isEmpty()) return inRecipe.get();
		}
		ItemStack inList = jei.getIngredientListOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
		if (inList != null && !inList.isEmpty()) return inList;
		ItemStack inBookmarks = jei.getBookmarkOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
		if (inBookmarks != null && !inBookmarks.isEmpty()) return inBookmarks;
		return ItemStack.EMPTY;
	}

	/** JEI's list settings from config/jei/jei-client.ini, read again only when the file changes. */
	private static ListConfig listConfig() {
		Path file = FMLPaths.CONFIGDIR.get().resolve("jei").resolve("jei-client.ini");
		long modified;
		try {
			modified = Files.exists(file) ? Files.getLastModifiedTime(file).toMillis() : 0L;
		} catch (IOException e) {
			modified = 0L;
		}
		ListConfig current = listConfig;
		if (current != null && current.modified() == modified) return current;
		current = ListConfig.read(file, modified);
		listConfig = current;
		return current;
	}

	/** Screens do not override equals, so the screen part compares by identity. */
	private record Key(Screen screen, int width, int height, int guiLeft, int guiTop, int guiWidth, int guiHeight,
					   int maxWidth) {
	}

	/** The [ingredientList] settings that move the list's left edge. JEI's defaults when the file has none. */
	record ListConfig(int maxColumns, boolean drawBackground, PanelPlacement.Alignment alignment, long modified) {
		static ListConfig read(Path file, long modified) {
			int maxColumns = 9;
			boolean drawBackground = false;
			PanelPlacement.Alignment alignment = PanelPlacement.Alignment.RIGHT;
			try {
				if (Files.exists(file)) {
					String section = "";
					for (String raw : Files.readAllLines(file)) {
						String line = raw.strip();
						if (line.isEmpty() || line.startsWith("#")) continue;
						if (line.startsWith("[") && line.endsWith("]")) {
							section = line.substring(1, line.length() - 1).strip();
							continue;
						}
						int equals = line.indexOf('=');
						if (!section.equals("ingredientList") || equals < 0) continue;
						String name = line.substring(0, equals).strip();
						String value = line.substring(equals + 1).strip();
						switch (name) {
							case "maxColumns" -> maxColumns = Math.max(1, Integer.parseInt(value));
							case "drawBackground" -> drawBackground = Boolean.parseBoolean(value);
							case "horizontalAlignment" -> alignment = PanelPlacement.Alignment.valueOf(value.toUpperCase(Locale.ROOT));
							default -> { }
						}
					}
				}
			} catch (IOException | RuntimeException e) {
				return new ListConfig(9, false, PanelPlacement.Alignment.RIGHT, modified);
			}
			return new ListConfig(maxColumns, drawBackground, alignment, modified);
		}
	}
}
