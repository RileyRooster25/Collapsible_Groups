package com.starskyxiii.collapsible_groups.client.skies;

import com.starskyxiii.collapsible_groups.Constants;
import com.starskyxiii.collapsible_groups.skies.StatsFile;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

/**
 * Reads every registered item's stats in game (they exist only while it runs) and writes them to
 * {@link StatsFile#RELATIVE_PATH} in the instance, for the build to rank items by.
 */
final class StatsCollector {
	private static final double PLAYER_BASE_DAMAGE = 1.0;
	private static final double PLAYER_BASE_SPEED = 4.0;
	private static final TagKey<Block> NEEDS_NETHERITE =
		TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("neoforge", "needs_netherite_tool"));

	private StatsCollector() {}

	/** Collects on the calling (main) thread, writes the file off it. */
	static void collectAndWrite() {
		long start = System.nanoTime();
		Map<String, Map<String, Object>> items = new TreeMap<>();
		for (Item item : BuiltInRegistries.ITEM) {
			ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
			try {
				Map<String, Object> stats = stats(item.getDefaultInstance());
				if (!stats.isEmpty()) items.put(id.toString(), stats);
			} catch (RuntimeException | LinkageError failure) {
				Constants.LOG.debug("Skipping stats for {}: {}", id, failure.toString());
			}
		}
		Map<String, String> mods = new TreeMap<>();
		ModList.get().getMods().forEach(mod -> mods.put(mod.getModId(), mod.getVersion().toString()));
		String content = StatsFile.render(SharedConstants.getCurrentVersion().getName(), mods, items);
		Path target = FMLPaths.GAMEDIR.get().resolve(StatsFile.RELATIVE_PATH);
		long millis = (System.nanoTime() - start) / 1_000_000;
		CompletableFuture.runAsync(() -> {
			try {
				StatsFile.writeAtomically(target, content);
				Constants.LOG.info("[CollapsibleGroups] Wrote stats for {} items to {} in {} ms", items.size(), target, millis);
			} catch (Exception failure) {
				Constants.LOG.warn("Could not write {}: {}", target, failure.toString());
			}
		});
	}

	private static Map<String, Object> stats(ItemStack stack) {
		Map<String, Object> stats = new LinkedHashMap<>();
		ItemAttributeModifiers modifiers = stack.getAttributeModifiers();
		addAttribute(stats, "attackDamage", modifiers, Attributes.ATTACK_DAMAGE, true, PLAYER_BASE_DAMAGE);
		addAttribute(stats, "attackSpeed", modifiers, Attributes.ATTACK_SPEED, true, PLAYER_BASE_SPEED);
		addAttribute(stats, "armor", modifiers, Attributes.ARMOR, false, 0);
		addAttribute(stats, "toughness", modifiers, Attributes.ARMOR_TOUGHNESS, false, 0);

		var tool = stack.get(DataComponents.TOOL);
		if (tool != null) {
			float speed = 0;
			for (BlockState state : List.of(Blocks.STONE.defaultBlockState(), Blocks.DEEPSLATE.defaultBlockState(),
				Blocks.OAK_LOG.defaultBlockState(), Blocks.DIRT.defaultBlockState(), Blocks.SAND.defaultBlockState())) {
				speed = Math.max(speed, stack.getDestroySpeed(state));
			}
			if (speed > 1) stats.put("miningSpeed", speed);
			miningLevel(stack).ifPresent(level -> stats.put("miningLevel", level));
		}

		int durability = stack.getMaxDamage();
		boolean equipment = tool != null || stats.containsKey("attackDamage") || stats.containsKey("armor");
		if (durability > 0) stats.put("durability", durability);
		if (stack.has(DataComponents.UNBREAKABLE) || (equipment && durability <= 0)) stats.put("unbreakable", true);

		IEnergyStorage energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
		if (energy != null) {
			int capacity = energy.getMaxEnergyStored();
			if (capacity > 0) stats.put("energyCapacity", capacity);
			int receive = energy.canReceive() ? energy.receiveEnergy(Integer.MAX_VALUE, true) : 0;
			if (receive > 0) stats.put("energyReceive", receive);
		}
		return stats;
	}

	/**
	 * Additive modifiers only, summed over the slots that count: main hand for weapons, any slot for
	 * armour. Absent when the item has no modifier for the attribute.
	 */
	private static void addAttribute(Map<String, Object> stats, String name, ItemAttributeModifiers modifiers,
		Holder<Attribute> attribute, boolean mainHand, double base) {
		double total = 0;
		boolean found = false;
		for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
			if (!entry.attribute().equals(attribute)) continue;
			if (entry.modifier().operation() != AttributeModifier.Operation.ADD_VALUE) continue;
			if (mainHand && !(entry.slot() == EquipmentSlotGroup.MAINHAND || entry.slot() == EquipmentSlotGroup.HAND
				|| entry.slot() == EquipmentSlotGroup.ANY)) continue;
			total += entry.modifier().amount();
			found = true;
		}
		if (found) stats.put(name, base + total);
	}

	/** 1 wood, 2 stone, 3 iron, 4 diamond, 5 netherite: the hardest block it harvests. Absent if it cannot mine stone. */
	private static Optional<Integer> miningLevel(ItemStack stack) {
		if (!stack.isCorrectToolForDrops(Blocks.STONE.defaultBlockState())) return Optional.empty();
		int level = 1;
		if (stack.isCorrectToolForDrops(Blocks.IRON_ORE.defaultBlockState())) level = 2;
		if (level == 2 && stack.isCorrectToolForDrops(Blocks.DIAMOND_ORE.defaultBlockState())) level = 3;
		if (level == 3 && stack.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState())) level = 4;
		if (level == 4) {
			for (Holder<Block> block : BuiltInRegistries.BLOCK.getTagOrEmpty(NEEDS_NETHERITE)) {
				if (stack.isCorrectToolForDrops(block.value().defaultBlockState())) {
					level = 5;
					break;
				}
			}
		}
		return Optional.of(level);
	}
}
