package com.starskyxiii.collapsible_groups.client.skies;

import com.mojang.datafixers.util.Either;
import com.starskyxiii.collapsible_groups.group.GroupChangeEvent;
import com.starskyxiii.collapsible_groups.group.GroupDefinition;
import com.starskyxiii.collapsible_groups.group.GroupRepository;
import com.starskyxiii.collapsible_groups.platform.Services;
import com.starskyxiii.collapsible_groups.skies.ModGroups;
import com.starskyxiii.collapsible_groups.skies.PanelPlacement;
import com.starskyxiii.collapsible_groups.skies.TypeRanks;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The short tooltip (item name, a divider, the mod group) and the fixed-width detail panel in the left
 * column of any screen where JEI's ingredient list is showing (inventories, machines, JEI's recipe screen),
 * which shows the last hovered item's full tooltip, its rank in its type,
 * and its mod group with the exact mod beneath.
 */
public final class SkiesDisplay {
	private static final int GAP = PanelPlacement.GAP;
	private static final int PADDING = 4;
	private static final boolean JEI_LOADED = ModList.get().isLoaded("jei");
	private static final int BACKGROUND = 0xF0100010;
	private static final int BORDER_TOP = 0x505000FF;
	private static final int BORDER_BOTTOM = 0x5028007F;
	private static final int TEXT = 0xFFFFFFFF;
	private static final int RANK = 0xFFFFAA00;
	private static final int MOD_GROUP = 0xFF5555FF;
	private static final int EXACT_MOD = 0xFF8888AA;

	private static ItemStack hovered = ItemStack.EMPTY;
	private static List<FormattedText> fullLines = List.of();
	private static volatile @Nullable TypeRanks ranks;

	private SkiesDisplay() {}

	static void subscribeToGroupChanges() {
		for (GroupChangeEvent.Kind kind : GroupChangeEvent.Kind.values()) {
			GroupChangeEvent.subscribe(kind, () -> ranks = null);
		}
	}

	/**
	 * The column the panel owns: the left edge of the screen, top-aligned with the screen's content, down
	 * to the bottom. Null when JEI's list is not showing, or the space left of the content is too narrow.
	 */
	static @Nullable Rect2i panelArea(@Nullable Screen screen) {
		if (screen == null || !SkiesConfig.display() || !listShowing()) return null;
		int[] origin = SkiesJei.contentOrigin(screen);
		if (origin == null) return null;
		return PanelPlacement.leftColumn(screen.width, screen.height, origin[0], origin[1], SkiesConfig.panelWidth())
			.map(area -> new Rect2i(area.x(), area.y(), area.width(), area.height()))
			.orElse(null);
	}

	/** JEI's search box has the keyboard. */
	static boolean searchFocused() {
		return JEI_LOADED && SkiesJei.searchFocused();
	}

	/** JEI's ingredient list is showing on the current screen: the one test for the tooltip and the panel. */
	static boolean listShowing() {
		return JEI_LOADED && SkiesJei.listShowing();
	}

	static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
		ItemStack stack = event.getItemStack();
		Minecraft minecraft = Minecraft.getInstance();
		if (stack.isEmpty() || minecraft.options.advancedItemTooltips || panelArea(minecraft.screen) == null) return;
		List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
		if (elements.isEmpty()) return;

		List<FormattedText> lines = new ArrayList<>();
		for (Either<FormattedText, TooltipComponent> element : elements) element.left().ifPresent(lines::add);
		String exactMod = exactMod(stack);
		if (lines.size() > 1 && lines.getLast().getString().equals(exactMod)) lines.removeLast();
		hovered = stack.copy();
		fullLines = List.copyOf(lines);

		FormattedText name = elements.getFirst().left().orElse(stack.getHoverName());
		Component group = Component.literal(modGroup(stack)).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC);
		Font font = minecraft.font;
		int width = Math.max(font.width(name), font.width(group));
		elements.clear();
		elements.add(Either.left(name));
		elements.add(Either.right(new DividerTooltipComponent(width)));
		elements.add(Either.left(group));
	}

	static void onScreenRender(ScreenEvent.Render.Post event) {
		drawAnnouncement(event);
		Rect2i area = panelArea(event.getScreen());
		if (area == null || hovered.isEmpty()) return;
		GuiGraphics graphics = event.getGuiGraphics();
		Font font = Minecraft.getInstance().font;
		int inner = area.getWidth() - 2 * PADDING;

		List<Line> lines = new ArrayList<>();
		for (int i = 0; i < fullLines.size(); i++) {
			for (FormattedCharSequence part : font.split(fullLines.get(i), inner)) lines.add(new Line(part, TEXT, 1f));
			if (i == 0) lines.add(Line.SPACER);
		}
		TypeRanks.Rank rank = ranks().rank(itemId(hovered));
		if (rank != null) {
			lines.add(Line.SPACER);
			Component text = Component.literal(rank.position() + " of " + rank.total() + " in " + groupName(rank.groupId()));
			for (FormattedCharSequence part : font.split(text, inner)) lines.add(new Line(part, RANK, 1f));
		}
		lines.add(Line.SPACER);
		for (FormattedCharSequence part : font.split(Component.literal(modGroup(hovered)), inner)) lines.add(new Line(part, MOD_GROUP, 1f));
		String exact = exactMod(hovered);
		if (!exact.equals(modGroup(hovered))) {
			for (FormattedCharSequence part : font.split(Component.literal(exact), (int) (inner / 0.75f))) {
				lines.add(new Line(part, EXACT_MOD, 0.75f));
			}
		}

		int height = 2 * PADDING;
		for (Line line : lines) height += line.height(font);
		int x = area.getX();
		int y = area.getY();
		int right = x + area.getWidth();
		int bottom = Math.min(y + height, area.getY() + area.getHeight());

		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 200);
		graphics.fill(x, y, right, bottom, BACKGROUND);
		graphics.fillGradient(x, y, x + 1, bottom, BORDER_TOP, BORDER_BOTTOM);
		graphics.fillGradient(right - 1, y, right, bottom, BORDER_TOP, BORDER_BOTTOM);
		graphics.fill(x, y, right, y + 1, BORDER_TOP);
		graphics.fill(x, bottom - 1, right, bottom, BORDER_BOTTOM);
		int lineY = y + PADDING;
		for (Line line : lines) {
			if (lineY + line.height(font) > bottom - PADDING) break;
			if (line.text() != null) {
				graphics.pose().pushPose();
				graphics.pose().translate(x + PADDING, lineY, 0);
				graphics.pose().scale(line.scale(), line.scale(), 1f);
				graphics.drawString(font, line.text(), 0, 0, line.color(), true);
				graphics.pose().popPose();
			}
			lineY += line.height(font);
		}
		graphics.pose().popPose();
	}

	/** The view mode message, briefly, at the top of the screen after the view mode key is pressed. */
	private static void drawAnnouncement(ScreenEvent.Render.Post event) {
		String text = SkiesViewState.announcement();
		if (text.isEmpty()) return;
		Font font = Minecraft.getInstance().font;
		GuiGraphics graphics = event.getGuiGraphics();
		int width = font.width(text);
		int x = (event.getScreen().width - width) / 2;
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 400);
		graphics.fill(x - 4, GAP, x + width + 4, GAP + font.lineHeight + 4, BACKGROUND);
		graphics.drawString(font, text, x, GAP + 2, TEXT, true);
		graphics.pose().popPose();
	}

	static void onScreenClosing(ScreenEvent.Closing event) {
		hovered = ItemStack.EMPTY;
		fullLines = List.of();
	}

	private record Line(@Nullable FormattedCharSequence text, int color, float scale) {
		static final Line SPACER = new Line(null, 0, 1f);

		int height(Font font) {
			return text == null ? 3 : (int) Math.ceil((font.lineHeight + 1) * scale);
		}
	}

	private static TypeRanks ranks() {
		TypeRanks current = ranks;
		if (current == null) {
			List<TypeRanks.Source> sources = new ArrayList<>();
			for (GroupDefinition group : GroupRepository.getAllIncludingScripted()) {
				if (group.hasMemberOrder()) sources.add(new TypeRanks.Source(group.id(), group.extra()));
			}
			current = TypeRanks.of(sources);
			ranks = current;
		}
		return current;
	}

	private static String groupName(String groupId) {
		for (GroupDefinition group : GroupRepository.getAllIncludingScripted()) {
			if (group.id().equals(groupId)) return group.name();
		}
		return groupId;
	}

	private static String itemId(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	private static String modGroup(ItemStack stack) {
		return ModGroups.name(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace(), Services.PLATFORM::getModName);
	}

	private static String exactMod(ItemStack stack) {
		return Services.PLATFORM.getModName(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
	}
}
