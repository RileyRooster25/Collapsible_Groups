package com.starskyxiii.collapsible_groups.client.skies;

import com.starskyxiii.collapsible_groups.platform.Services;
import com.starskyxiii.collapsible_groups.skies.ModGroups;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The detail panel (step 1a, placement F): docked between the screen's content and JEI's list, it shows the
 * hovered item's full tooltip, Shift lines included and mod name lines left out, then the mod group and,
 * beneath in smaller text, the exact mod when it differs. The hover tooltip itself is left as it was.
 *
 * <p>The reserved rectangle is fixed for the screen; the visible box is drawn only as tall as its text,
 * clips when longer, and the mouse wheel over it scrolls it.
 */
public final class SkiesDisplay {
	/** Above the screen and JEI's list, below tooltips (drawn at 400). */
	private static final int Z = 200;
	private static final int INSET = 4;
	private static final int LINE = 10;
	private static final int NAME_EXTRA = 2;
	private static final int SCROLLBAR_ROOM = 3;
	private static final int FOOT_ONE_LINE = 15;
	private static final int FOOT_TWO_LINES = 23;
	private static final int REBUILD_TICKS = 10;
	private static final int SCROLL_STEP = LINE;
	private static final float SMALL = 0.75f;
	private static final int RULE = 0x24FFFFFF;
	private static final int MOD_GROUP = 0xFF5555FF;
	private static final int EXACT_MOD = 0xFF8F8FD8;
	private static final int TRACK = 0x1AFFFFFF;
	private static final int THUMB = 0xA6C8BEFF;

	private static volatile boolean jeiPresent;
	private static long tick;

	private static ItemStack shown = ItemStack.EMPTY;
	private static boolean stale;
	private static long builtTick = Long.MIN_VALUE;
	private static List<Component> lines = List.of();
	private static List<Row> rows = List.of();
	private static int rowsWidth = -1;
	private static String group = "";
	private static String exact = "";
	private static int scroll;
	private static int maxScroll;
	private static @Nullable Rect2i box;

	private SkiesDisplay() {}

	static void jeiStarted() {
		jeiPresent = true;
	}

	static void jeiStopped() {
		jeiPresent = false;
		clear();
	}

	/** The reserved rectangle on this screen, or null for no panel (switched off, no JEI, or no room). */
	static @Nullable Rect2i reservedArea(@Nullable Screen screen) {
		if (!jeiPresent || !SkiesConfig.display()) return null;
		return SkiesJei.reservedArea(screen, SkiesConfig.panelWidth(), tick);
	}

	static void onClientTick(ClientTickEvent.Post event) {
		tick++;
	}

	static void onScreenRender(ScreenEvent.Render.Post event) {
		box = null;
		Screen screen = event.getScreen();
		Rect2i area = reservedArea(screen);
		if (area == null || !SkiesJei.listDisplayed()) return;
		follow(hoveredItem(screen));
		if (shown.isEmpty()) return;
		if ((stale && builtTick != tick) || tick - builtTick >= REBUILD_TICKS || tick < builtTick) rebuild();
		draw(event.getGuiGraphics(), area);
	}

	static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
		Rect2i current = box;
		if (current == null || maxScroll <= 0 || event.getScrollDeltaY() == 0) return;
		if (!current.contains((int) event.getMouseX(), (int) event.getMouseY())) return;
		int step = event.getScrollDeltaY() > 0 ? -SCROLL_STEP : SCROLL_STEP;
		scroll = Math.max(0, Math.min(maxScroll, scroll + step));
		event.setCanceled(true);
	}

	static void onScreenClosing(ScreenEvent.Closing event) {
		clear();
	}

	private static void clear() {
		shown = ItemStack.EMPTY;
		lines = List.of();
		rows = List.of();
		rowsWidth = -1;
		stale = false;
		scroll = 0;
		maxScroll = 0;
		box = null;
	}

	/** The item under the mouse: the screen's own slot first, then JEI's recipe slots, list and bookmarks. */
	private static ItemStack hoveredItem(Screen screen) {
		if (screen instanceof AbstractContainerScreen<?> container) {
			Slot slot = container.getSlotUnderMouse();
			if (slot != null && slot.hasItem()) return slot.getItem();
		}
		return SkiesJei.hoveredItem(screen);
	}

	/** Keeps the last hovered item; a new one is built on the next tick at the latest, never every frame. */
	private static void follow(ItemStack hovered) {
		if (hovered.isEmpty() || ItemStack.isSameItemSameComponents(hovered, shown)) return;
		if (!ItemStack.isSameItem(hovered, shown)) scroll = 0;
		shown = hovered.copy();
		stale = true;
	}

	private static void rebuild() {
		builtTick = tick;
		stale = false;
		String namespace = BuiltInRegistries.ITEM.getKey(shown.getItem()).getNamespace();
		exact = Services.PLATFORM.getModName(namespace);
		group = ModGroups.name(namespace, Services.PLATFORM::getModName);
		List<Component> built = safeTooltipLines(shown);
		List<Component> kept = new ArrayList<>(built.size());
		for (int i = 0; i < built.size(); i++) {
			Component line = built.get(i);
			if (i > 0 && isModName(line)) continue;
			kept.add(line);
		}
		lines = List.copyOf(kept);
		rowsWidth = -1;
	}

	private static boolean isModName(Component line) {
		String plain = ChatFormatting.stripFormatting(line.getString());
		if (plain == null) return false;
		plain = plain.strip();
		return !plain.isEmpty() && (plain.equals(exact) || plain.equals(group));
	}

	private static void draw(GuiGraphics graphics, Rect2i area) {
		Font font = Minecraft.getInstance().font;
		int ix = area.getX() + INSET;
		int iw = area.getWidth() - 2 * INSET;
		int textWidth = iw - SCROLLBAR_ROOM;
		if (rowsWidth != textWidth) {
			rows = wrap(font, textWidth);
			rowsWidth = textWidth;
		}
		boolean differs = !exact.equals(group);
		int footH = differs ? FOOT_TWO_LINES : FOOT_ONE_LINE;
		int total = 0;
		for (Row row : rows) total += row.height();
		int boxH = Math.min(area.getHeight(), INSET + total + 2 + footH + INSET);
		int x = area.getX();
		int y = area.getY();
		int iy = y + INSET;
		int fy = y + boxH - INSET - footH;
		int viewH = Math.max(0, fy - 2 - iy);
		maxScroll = Math.max(0, total - viewH);
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		box = new Rect2i(x, y, area.getWidth(), boxH);

		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, Z);
		TooltipRenderUtil.renderTooltipBackground(graphics, x + INSET, y + INSET, area.getWidth() - 2 * INSET, boxH - 2 * INSET, 0);

		graphics.enableScissor(ix - 1, iy, ix + iw + 1, iy + viewH);
		int rowY = iy - scroll;
		for (Row row : rows) {
			if (row.text() != null && rowY + LINE > iy && rowY < iy + viewH) {
				graphics.drawString(font, row.text(), ix, rowY, 0xFFFFFFFF, true);
			}
			rowY += row.height();
		}
		graphics.disableScissor();

		if (maxScroll > 0) {
			int barX = x + area.getWidth() - 5;
			int thumbH = Math.max(8, Math.round((float) viewH * viewH / total));
			int thumbY = iy + Math.round((viewH - thumbH) * (scroll / (float) maxScroll));
			graphics.fill(barX, iy, barX + 2, iy + viewH, TRACK);
			graphics.fill(barX, thumbY, barX + 2, thumbY + thumbH, THUMB);
		}

		graphics.fill(ix, fy, ix + iw, fy + 1, RULE);
		Component groupText = Component.literal(font.plainSubstrByWidth(group, iw)).withStyle(ChatFormatting.ITALIC);
		graphics.drawString(font, groupText, ix, fy + 4, MOD_GROUP, true);
		if (differs) {
			graphics.pose().pushPose();
			graphics.pose().translate(ix, fy + 15, 0);
			graphics.pose().scale(SMALL, SMALL, 1f);
			graphics.drawString(font, font.plainSubstrByWidth(exact, (int) (iw / SMALL)), 0, 0, EXACT_MOD, true);
			graphics.pose().popPose();
		}
		graphics.pose().popPose();
	}

	/** The tooltip lines wrapped to the panel: the name, a little space, then every other line. */
	private static List<Row> wrap(Font font, int width) {
		List<Row> wrapped = new ArrayList<>();
		for (int i = 0; i < lines.size(); i++) {
			Component line = lines.get(i);
			List<FormattedCharSequence> parts = line.getString().isEmpty() ? List.of() : font.split(line, width);
			if (parts.isEmpty()) {
				wrapped.add(new Row(null, LINE));
			} else {
				for (FormattedCharSequence part : parts) wrapped.add(new Row(part, LINE));
			}
			if (i == 0) {
				Row last = wrapped.removeLast();
				wrapped.add(new Row(last.text(), last.height() + NAME_EXTRA));
			}
		}
		return List.copyOf(wrapped);
	}

	private record Row(@Nullable FormattedCharSequence text, int height) {
	}

	private static final java.util.Set<String> FAILED_TOOLTIPS = new java.util.HashSet<>();

	/**
	 * The panel's lines for an item: with Shift forced first; if a mod's tooltip code throws there, the
	 * ordinary lines; if that throws too, the name alone. A failure is logged once per item and never
	 * reaches the screen, so one broken tooltip cannot crash the game.
	 */
	private static List<Component> safeTooltipLines(ItemStack stack) {
		try {
			return ForcedShift.tooltipLines(stack);
		} catch (RuntimeException | LinkageError forced) {
			logOnce(stack, "with Shift", forced);
		}
		try {
			Minecraft minecraft = Minecraft.getInstance();
			return stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(minecraft.level), minecraft.player,
				minecraft.options.advancedItemTooltips ? net.minecraft.world.item.TooltipFlag.ADVANCED : net.minecraft.world.item.TooltipFlag.NORMAL);
		} catch (RuntimeException | LinkageError plain) {
			logOnce(stack, "without Shift", plain);
			return List.of(stack.getHoverName());
		}
	}

	private static void logOnce(ItemStack stack, String how, Throwable failure) {
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()) + " " + how;
		if (FAILED_TOOLTIPS.add(id)) {
			com.starskyxiii.collapsible_groups.Constants.LOG.warn("Detail panel: the tooltip of {} failed; showing less", id, failure);
		}
	}
}
