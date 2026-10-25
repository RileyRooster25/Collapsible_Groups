package com.starskyxiii.collapsible_groups.client.skies;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Builds an item's tooltip lines as if Shift were held. Screen.hasShiftDown() (through MixinScreenShift)
 * answers true only on the thread doing this, and only for the one getTooltipLines call.
 */
public final class ForcedShift {
	private static volatile @Nullable Thread owner;

	private ForcedShift() {}

	/** True only inside {@link #tooltipLines} on the thread running it. */
	public static boolean active() {
		Thread current = owner;
		return current != null && current == Thread.currentThread();
	}

	static List<Component> tooltipLines(ItemStack stack) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		TooltipFlag flag = new ShiftHeld(minecraft.options.advancedItemTooltips, player != null && player.isCreative());
		owner = Thread.currentThread();
		try {
			return stack.getTooltipLines(Item.TooltipContext.of(minecraft.level), player, flag);
		} finally {
			owner = null;
		}
	}

	/** A flag that reports Shift held and asks for all information, as recipe viewers may. */
	private record ShiftHeld(boolean advanced, boolean creative) implements TooltipFlag {
		@Override
		public boolean isAdvanced() {
			return advanced;
		}

		@Override
		public boolean isCreative() {
			return creative;
		}

		@Override
		public boolean hasShiftDown() {
			return true;
		}

		@Override
		public boolean hasControlDown() {
			return Screen.hasControlDown();
		}

		@Override
		public boolean hasAltDown() {
			return Screen.hasAltDown();
		}

		@Override
		public boolean shouldDisplayAllInformation() {
			return true;
		}
	}
}
