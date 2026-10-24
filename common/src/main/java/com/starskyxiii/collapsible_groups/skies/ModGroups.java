package com.starskyxiii.collapsible_groups.skies;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Which family of mods (a mod group, named by its main mod) each mod namespace belongs to, read from
 * {@code assets/collapsible_groups/skies/mod_groups.json}: {@code {"schema": 1, "groups": {"<namespace>": "<name>"}}}.
 * A namespace the file does not name shows the mod's own display name.
 */
public final class ModGroups {
	public static final String RESOURCE_PATH = "skies/mod_groups.json";
	public static final int SCHEMA = 1;
	private static final int FOOTER_LIMIT = 3;

	private static volatile Map<String, String> groups = Map.of();

	private ModGroups() {}

	/** Replaces the table from the file's text; null (no file) empties it. Returns a problem, or null. */
	public static String reload(String json) {
		if (json == null) {
			groups = Map.of();
			return null;
		}
		try {
			groups = parse(json);
			return null;
		} catch (RuntimeException failure) {
			groups = Map.of();
			return failure.getMessage();
		}
	}

	static Map<String, String> parse(String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		JsonElement schema = root.get("schema");
		if (schema == null || schema.getAsInt() != SCHEMA) {
			throw new IllegalArgumentException("unsupported mod group schema " + schema + ", expected " + SCHEMA);
		}
		Map<String, String> parsed = new LinkedHashMap<>();
		JsonElement entries = root.get("groups");
		if (entries != null && entries.isJsonObject()) {
			for (Map.Entry<String, JsonElement> entry : entries.getAsJsonObject().entrySet()) {
				if (entry.getValue().isJsonPrimitive()) parsed.put(entry.getKey(), entry.getValue().getAsString());
			}
		}
		return Map.copyOf(parsed);
	}

	/** The mod group name for a namespace, else the mod's own display name from {@code modName}. */
	public static String name(String namespace, UnaryOperator<String> modName) {
		String group = groups.get(namespace);
		return group != null ? group : modName.apply(namespace);
	}

	/** Distinct mod group names for the namespaces, in order: the first three, then "+N" for the rest. */
	public static String footer(Collection<String> namespaces, UnaryOperator<String> modName) {
		Set<String> names = new LinkedHashSet<>();
		for (String namespace : namespaces) names.add(name(namespace, modName));
		List<String> shown = new ArrayList<>(names);
		if (shown.size() <= FOOTER_LIMIT) return String.join(", ", shown);
		return String.join(", ", shown.subList(0, FOOTER_LIMIT)) + " +" + (shown.size() - FOOTER_LIMIT);
	}
}
