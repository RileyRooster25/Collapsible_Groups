package com.starskyxiii.collapsible_groups.skies;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The mod group table, the stats file and type ranks: the Skies Aero data the fork reads and writes. */
class SkiesDataTest {
	@AfterEach
	void clearModGroups() {
		ModGroups.reload(null);
	}

	@Test
	void modGroupsFallBackToTheModNameWhenTheFileOrNamespaceIsMissing() {
		assertEquals("Mekanism Tools", ModGroups.name("mekanismtools", id -> "Mekanism Tools"));
		assertNull(ModGroups.reload("{\"schema\": 1, \"groups\": {\"mekanismtools\": \"Mekanism\"}}"));
		assertEquals("Mekanism", ModGroups.name("mekanismtools", id -> "Mekanism Tools"));
		assertEquals("Create", ModGroups.name("create", id -> "Create"));
		assertNotNull(ModGroups.reload("{\"schema\": 2, \"groups\": {}}"), "an unknown schema is refused by name");
		assertEquals("Mekanism Tools", ModGroups.name("mekanismtools", id -> "Mekanism Tools"));
	}

	@Test
	void aModThatBelongsToAFamilyShowsTheFamilyName() {
		assertNull(ModGroups.reload("{\"schema\": 1, \"groups\": {\"mekanism_weaponry\": \"Mekanism\", \"mekanism\": \"Mekanism\"}}"));
		assertEquals("Mekanism", ModGroups.name("mekanism_weaponry", id -> "Mekanism: Weaponry"));
		assertEquals("Mekanism", ModGroups.name("mekanism", id -> "Mekanism"));
	}

	@Test
	void footerListsDistinctModGroupsThenACount() {
		ModGroups.reload("{\"schema\": 1, \"groups\": {\"mekanismtools\": \"Mekanism\", \"mekanism\": \"Mekanism\"}}");
		assertEquals("Mekanism, a, b", ModGroups.footer(List.of("mekanism", "mekanismtools", "a", "b"), id -> id));
		assertEquals("a, b, c +2", ModGroups.footer(List.of("a", "b", "c", "d", "e", "a"), id -> id));
	}

	@Test
	void statsFileIsSortedVersionedAndStable() {
		Map<String, Map<String, Object>> items = new LinkedHashMap<>();
		items.put("test:sword", new LinkedHashMap<>(Map.of("attackSpeed", 1.6000000238418579, "attackDamage", 7.0, "unbreakable", true)));
		items.put("test:battery", Map.of("energyCapacity", 1000000));
		String expected = """
			{
			  "schema": 1,
			  "minecraft": "1.21.1",
			  "mods": {
			    "a": "1.0",
			    "b": "2.0"
			  },
			  "items": {
			    "test:battery": {
			      "energyCapacity": 1000000
			    },
			    "test:sword": {
			      "attackDamage": 7,
			      "attackSpeed": 1.6,
			      "unbreakable": true
			    }
			  }
			}
			""";
		assertEquals(expected, StatsFile.render("1.21.1", Map.of("b", "2.0", "a", "1.0"), items));
	}

	@Test
	void statsFileIsReplacedWhole(@TempDir Path directory) throws IOException {
		Path target = directory.resolve("skies-aero/item-stats.json");
		StatsFile.writeAtomically(target, "first");
		StatsFile.writeAtomically(target, "second");
		assertEquals("second", Files.readString(target));
		assertFalse(Files.exists(target.resolveSibling("item-stats.json.tmp")));
	}

	@Test
	void ranksComeFromEachGroupsOrderList() {
		JsonObject extra = new JsonObject();
		JsonArray order = new JsonArray();
		order.add("test:best");
		order.add("test:second");
		extra.add("order", order);
		TypeRanks ranks = TypeRanks.of(List.of(new TypeRanks.Source("swords", extra)));
		assertEquals(new TypeRanks.Rank("swords", 2, 2), ranks.rank("test:second"));
		assertNull(ranks.rank("test:unlisted"));
	}

	@Test
	void skiesPackageNeedsNothingFromTheGameRuntime() throws IOException {
		Path source = Path.of("src/main/java/com/starskyxiii/collapsible_groups/skies");
		assertTrue(Files.isDirectory(source), "run from the common project directory");
		try (Stream<Path> files = Files.list(source)) {
			for (Path file : files.toList()) {
				for (String line : Files.readAllLines(file)) {
					if (!line.startsWith("import ")) continue;
					assertFalse(line.contains("net.minecraft") || line.contains("net.neoforged") || line.contains("mezz.jei"),
						file.getFileName() + " imports " + line);
				}
			}
		}
	}
}
