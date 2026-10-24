package com.starskyxiii.collapsible_groups.skies;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Each item's place in its type, from the {@code extra.order} lists the build writes best first.
 * The first group listing an item wins.
 */
public final class TypeRanks {
	public record Rank(String groupId, int position, int total) {}

	/** One group's id and its extra block, so this class needs nothing from the game. */
	public record Source(String groupId, JsonObject extra) {}

	private final Map<String, Rank> ranks;

	private TypeRanks(Map<String, Rank> ranks) {
		this.ranks = ranks;
	}

	public static TypeRanks of(List<Source> groups) {
		Map<String, Rank> ranks = new HashMap<>();
		for (Source group : groups) {
			JsonElement order = group.extra().get("order");
			if (order == null || !order.isJsonArray()) continue;
			JsonArray members = order.getAsJsonArray();
			for (int i = 0; i < members.size(); i++) {
				JsonElement member = members.get(i);
				if (!member.isJsonPrimitive()) continue;
				ranks.putIfAbsent(member.getAsString(), new Rank(group.groupId(), i + 1, members.size()));
			}
		}
		return new TypeRanks(Map.copyOf(ranks));
	}

	public Rank rank(String itemId) {
		return ranks.get(itemId);
	}
}
