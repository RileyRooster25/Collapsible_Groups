package com.starskyxiii.collapsible_groups.client.skies;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import java.util.function.Function;

/** Wires the Skies Aero display and the stats file into the client. The one place that registers them. */
public final class SkiesClient {
	/** Cycles the view mode of JEI's ingredient list. Unbound by default; set it in Controls. */
	static final KeyMapping CYCLE_VIEW_MODE = new KeyMapping("key.collapsible_groups.cycle_view_mode",
		KeyConflictContext.UNIVERSAL, InputConstants.UNKNOWN, "key.categories.collapsible_groups");

	private SkiesClient() {}

	public static void register(IEventBus modBus, ModContainer container) {
		container.registerConfig(ModConfig.Type.CLIENT, SkiesConfig.SPEC, "collapsiblegroups/skies.toml");
		modBus.addListener((RegisterClientTooltipComponentFactoriesEvent event) ->
			event.register(DividerTooltipComponent.class, Function.identity()));
		NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, SkiesDisplay::onGatherTooltip);
		NeoForge.EVENT_BUS.addListener(SkiesDisplay::onScreenRender);
		NeoForge.EVENT_BUS.addListener(SkiesDisplay::onScreenClosing);
		NeoForge.EVENT_BUS.addListener(SkiesClient::onTagsUpdated);
		modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(CYCLE_VIEW_MODE));
		modBus.addListener((ModConfigEvent.Reloading event) -> {
			if (event.getConfig().getSpec() == SkiesConfig.SPEC) SkiesViewState.requestRelayout();
		});
		NeoForge.EVENT_BUS.addListener(SkiesClient::onScreenKey);
		NeoForge.EVENT_BUS.addListener(SkiesClient::onClientTick);
		SkiesDisplay.subscribeToGroupChanges();
	}

	/** The view mode key on a screen (where JEI's list is), unless JEI's search box has the keyboard. */
	private static void onScreenKey(ScreenEvent.KeyPressed.Pre event) {
		if (CYCLE_VIEW_MODE.isUnbound()) return;
		if (!CYCLE_VIEW_MODE.isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode()))) return;
		if (SkiesDisplay.searchFocused()) return;
		SkiesViewState.cycle();
		event.setCanceled(true);
	}

	/** The view mode key in the world, with no screen open. */
	private static void onClientTick(ClientTickEvent.Post event) {
		while (CYCLE_VIEW_MODE.consumeClick()) SkiesViewState.cycle();
	}

	/** Tags arrive after joining a world; mining levels need them, so the stats are read then. */
	private static void onTagsUpdated(TagsUpdatedEvent event) {
		if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED || !SkiesConfig.writeStats()) return;
		Minecraft.getInstance().execute(StatsCollector::collectAndWrite);
	}
}
