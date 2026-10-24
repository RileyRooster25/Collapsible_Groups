package com.starskyxiii.collapsible_groups.client.skies;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** A one-pixel rule between the item name and the mod group in the short tooltip. */
public record DividerTooltipComponent(int width) implements TooltipComponent, ClientTooltipComponent {
	private static final int COLOR = 0x60FFFFFF;

	@Override
	public int getHeight() {
		return 4;
	}

	@Override
	public int getWidth(Font font) {
		return width;
	}

	@Override
	public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
		guiGraphics.fill(x, y + 1, x + width, y + 2, COLOR);
	}
}
