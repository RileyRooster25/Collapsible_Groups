package com.starskyxiii.collapsible_groups.persistence;

import com.starskyxiii.collapsible_groups.group.GroupDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The group documents skies-aero-groups' build writes: parent and order live in extra and survive a save. */
class SkiesGroupJsonTest {
	private static final String PARENT = """
		{
		  "schema_version": 1,
		  "id": "sag_equipment_weapons",
		  "name": {"translate": "skies_aero_groups.group.sag_equipment_weapons", "fallback": "Weapons"},
		  "enabled": true,
		  "priority": 100,
		  "filter": {"type": "item", "id": "skies_aero_groups:nothing"},
		  "extra": {"parent": "sag_equipment", "order": ["sag_equipment_weapons_swords", "sag_equipment_weapons_bows"]}
		}
		""";

	private static final String LEAF = """
		{
		  "schema_version": 1,
		  "id": "sag_equipment_weapons_swords",
		  "name": {"translate": "skies_aero_groups.group.sag_equipment_weapons_swords", "fallback": "Swords"},
		  "enabled": true,
		  "priority": 100,
		  "filter": {"any": [{"type": "item", "id": "minecraft:iron_sword"}, {"type": "item", "id": "minecraft:netherite_sword"}]},
		  "extra": {"parent": "sag_equipment_weapons", "order": ["minecraft:netherite_sword", "minecraft:iron_sword"]}
		}
		""";

	@Test
	void parentAndOrderAreReadFromExtra() {
		GroupDefinition parent = GroupConfig.fromJsonChecked(PARENT);
		GroupDefinition leaf = GroupConfig.fromJsonChecked(LEAF);
		assertEquals("sag_equipment", parent.parentId());
		assertEquals(1, parent.memberRank("sag_equipment_weapons_bows"));
		assertEquals("sag_equipment_weapons", leaf.parentId());
		assertEquals(0, leaf.memberRank("minecraft:netherite_sword"));
		assertEquals(Integer.MAX_VALUE, leaf.memberRank("minecraft:wooden_sword"));
		assertTrue(leaf.hasMemberOrder());
		assertEquals("Swords", leaf.displayName().fallback());
	}

	@Test
	void parentAndOrderSurviveASave() {
		GroupDefinition leaf = GroupConfig.fromJsonChecked(LEAF);
		GroupDefinition again = GroupConfig.fromJsonChecked(GroupConfig.toJson(leaf));
		assertEquals("sag_equipment_weapons", again.parentId());
		assertEquals(0, again.memberRank("minecraft:netherite_sword"));
	}

	@Test
	void groupsWithoutExtraHaveNoParentOrOrder() {
		GroupDefinition plain = GroupConfig.fromJsonChecked("""
			{"id": "plain", "name": "Plain", "filter": {"type": "item", "id": "minecraft:stone"}}
			""");
		assertNull(plain.parentId());
		assertFalse(plain.hasMemberOrder());
	}
}
