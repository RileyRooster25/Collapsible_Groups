package com.starskyxiii.collapsible_groups.skies;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;

/**
 * The item stats file the build reads: {@code {"schema": 1, "minecraft": ..., "mods": {id: version},
 * "items": {id: {stat: value}}}}, every key sorted. A stat that cannot be known is absent, never zero.
 */
public final class StatsFile {
	public static final int SCHEMA = 1;
	public static final String RELATIVE_PATH = "skies-aero/item-stats.json";

	private StatsFile() {}

	public static String render(String minecraftVersion, Map<String, String> modVersions,
		Map<String, ? extends Map<String, ?>> items) {
		JsonObject root = new JsonObject();
		root.addProperty("schema", SCHEMA);
		root.addProperty("minecraft", minecraftVersion);
		JsonObject mods = new JsonObject();
		new TreeMap<>(modVersions).forEach(mods::addProperty);
		root.add("mods", mods);
		JsonObject itemsJson = new JsonObject();
		new TreeMap<>(items).forEach((id, stats) -> {
			JsonObject entry = new JsonObject();
			new TreeMap<String, Object>(stats).forEach((stat, value) -> {
				switch (value) {
					case Boolean flag -> entry.addProperty(stat, flag);
					case Number number -> entry.addProperty(stat, round(number));
					case null -> { }
					default -> entry.addProperty(stat, value.toString());
				}
			});
			itemsJson.add(id, entry);
		});
		root.add("items", itemsJson);
		return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root) + "\n";
	}

	/** Whole numbers stay whole; others keep four decimals, so the file is stable between runs. */
	private static Number round(Number number) {
		double value = number.doubleValue();
		if (value == Math.rint(value) && Math.abs(value) < 1e15) return (long) value;
		return Math.round(value * 10_000d) / 10_000d;
	}

	/** Writes to a temporary file beside the target, then renames it over the target. */
	public static void writeAtomically(Path target, String content) throws IOException {
		Files.createDirectories(target.getParent());
		Path temp = target.resolveSibling(target.getFileName() + ".tmp");
		Files.writeString(temp, content, StandardCharsets.UTF_8);
		try {
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException unsupported) {
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
