package com.starskyxiii.collapsible_groups.client.skies;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/** Wires the Skies Aero detail panel and the stats file into the client. The one place that registers them. */
public final class SkiesClient {
	private SkiesClient() {}

	public static void register(IEventBus modBus, ModContainer container) {
		container.registerConfig(ModConfig.Type.CLIENT, SkiesConfig.SPEC, "collapsiblegroups/skies.toml");
		NeoForge.EVENT_BUS.addListener(SkiesDisplay::onClientTick);
		NeoForge.EVENT_BUS.addListener(SkiesDisplay::onScreenRender);
		// Ahead of JEI and the screen, but only consumed when the wheel is over the panel and it can scroll.
		NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, SkiesDisplay::onMouseScrolled);
		NeoForge.EVENT_BUS.addListener(SkiesDisplay::onScreenClosing);
		NeoForge.EVENT_BUS.addListener(SkiesClient::onTagsUpdated);
	}

	/** Tags arrive after joining a world; mining levels need them, so the stats are read then. */
	private static void onTagsUpdated(TagsUpdatedEvent event) {
		if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED || !SkiesConfig.writeStats()) return;
		Minecraft.getInstance().execute(StatsCollector::collectAndWrite);
	}
}
