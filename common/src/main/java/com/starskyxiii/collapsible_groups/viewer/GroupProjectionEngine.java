package com.starskyxiii.collapsible_groups.viewer;

import com.starskyxiii.collapsible_groups.group.GroupDefinition;
import com.starskyxiii.collapsible_groups.group.GroupCatalog;
import com.starskyxiii.collapsible_groups.internal.query.IngredientCatalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Computes viewer-independent ownership and collapsible group projection. */
public final class GroupProjectionEngine {
	private GroupProjectionEngine() {}

	public static <E> ViewerProjection<E> project(
		ViewerIngredientUniverse<E> universe,
		ViewerSearchSnapshot<E> search,
		List<GroupDefinition> groups,
		GroupExpansionState expansionState
	) {
		GroupCandidateIndex candidates = buildCandidateIndex(universe, groups);
		return project(universe, search, groups, expansionState, candidates);
	}

	public static <E> ViewerProjection<E> project(
		ViewerIngredientUniverse<E> universe,
		ViewerSearchSnapshot<E> search,
		List<GroupDefinition> groups,
		GroupExpansionState expansionState,
		GroupCandidateIndex candidates
	) {
		return project(universe, search, groups, expansionState, resolveOwnership(candidates, groups));
	}

	public static <E> ViewerProjection<E> project(
		ViewerIngredientUniverse<E> universe,
		ViewerSearchSnapshot<E> search,
		List<GroupDefinition> groups,
		GroupExpansionState expansionState,
		Map<ViewerIngredientIdentity, String> ownership
	) {
		Map<String, GroupDefinition> groupsById = new LinkedHashMap<>();
		for (GroupDefinition group : groups) groupsById.put(group.id(), group);
		Map<ViewerIngredientIdentity, GroupDefinition> owners = new LinkedHashMap<>();
		ownership.forEach((identity, groupId) -> {
			GroupDefinition group = groupsById.get(groupId);
			if (group != null && com.starskyxiii.collapsible_groups.group.GroupRepository.isActive(group)) owners.put(identity, group);
		});

		Map<String, GroupDefinition> ownedGroups = new LinkedHashMap<>();
		Map<String, List<ViewerIngredient<E>>> items = new LinkedHashMap<>();
		Map<String, List<ViewerIngredient<E>>> fluids = new LinkedHashMap<>();
		Map<String, List<ViewerIngredient<E>>> generic = new LinkedHashMap<>();
		Map<ViewerIngredientIdentity, Integer> positions = new HashMap<>();
		for (ViewerIngredient<E> ingredient : search.filteredResults()) {
			positions.putIfAbsent(ingredient.identity(), positions.size());
			GroupDefinition owner = owners.get(ingredient.identity());
			if (owner == null) continue;
			ownedGroups.putIfAbsent(owner.id(), owner);
			Map<String, List<ViewerIngredient<E>>> bucket = switch (ingredient.kind()) {
				case ITEM -> items;
				case FLUID -> fluids;
				case GENERIC -> generic;
			};
			bucket.computeIfAbsent(owner.id(), ignored -> new ArrayList<>()).add(ingredient);
		}
		for (GroupDefinition group : ownedGroups.values()) {
			if (!group.hasMemberOrder()) continue;
			Comparator<ViewerIngredient<E>> byRank = memberOrder(group, positions);
			for (Map<String, List<ViewerIngredient<E>>> bucket : List.of(items, fluids, generic)) {
				List<ViewerIngredient<E>> members = bucket.get(group.id());
				if (members != null) members.sort(byRank);
			}
		}

		GroupTree tree = GroupTree.of(groups);
		NestedBuilder<E> nested = new NestedBuilder<>(tree, search, expansionState, items, fluids, generic, positions);
		List<ViewerProjection.Entry<E>> projected = new ArrayList<>();
		Set<String> emittedHeaders = new HashSet<>();
		for (ViewerIngredient<E> ingredient : search.filteredResults()) {
			GroupDefinition owner = owners.get(ingredient.identity());
			if (owner != null && tree.nested(owner.id())) {
				String root = tree.root(owner.id());
				if (emittedHeaders.add(root)) projected.addAll(nested.build(root));
				continue;
			}
			if (owner != null) {
				List<ViewerIngredient<E>> itemChildren = items.getOrDefault(owner.id(), List.of());
				List<ViewerIngredient<E>> fluidChildren = fluids.getOrDefault(owner.id(), List.of());
				List<ViewerIngredient<E>> genericChildren = generic.getOrDefault(owner.id(), List.of());
				int childCount = itemChildren.size() + fluidChildren.size() + genericChildren.size();
				if (childCount >= 2) {
					if (shouldUngroupForSearch(search, childCount)) {
						projected.add(new ViewerProjection.IngredientEntry<>(ingredient));
						continue;
					}
					if (emittedHeaders.add(owner.id())) {
						List<ViewerIngredient<E>> children = new ArrayList<>(childCount);
						children.addAll(itemChildren);
						children.addAll(fluidChildren);
						children.addAll(genericChildren);
						projected.add(new ViewerProjection.GroupHeader<>(
							ownedGroups.get(owner.id()),
							children,
							itemChildren.size(),
							fluidChildren.size(),
							genericChildren.size(),
							expansionState.isExpanded(owner.id()),
							owner.iconIds(),
							fallbackIcons(owner, children)
						));
					}
					continue;
				}
			}
			projected.add(new ViewerProjection.IngredientEntry<>(ingredient));
		}

		return new ViewerProjection<>(projected, ownership);
	}

	public static <E> Map<ViewerIngredientIdentity, String> buildOwnership(
		IngredientCatalog<ViewerIngredient<E>, ViewerIngredientIdentity> universe,
		List<GroupDefinition> groups
	) {
		return resolveOwnership(buildCandidateIndex(universe, groups), groups);
	}

	public static <E> GroupCandidateIndex buildCandidateIndex(
		IngredientCatalog<ViewerIngredient<E>, ViewerIngredientIdentity> universe,
		List<GroupDefinition> groups
	) {
		List<GroupDefinition> priorityOrder = GroupCatalog.orderByPriority(groups);
		List<GroupDefinition> itemGroups = priorityOrder.stream().filter(GroupDefinition::hasItemFilters).toList();
		List<GroupDefinition> fluidGroups = priorityOrder.stream().filter(GroupDefinition::hasFluidFilters).toList();
		List<GroupDefinition> genericGroups = priorityOrder.stream().filter(GroupDefinition::hasGenericFilters).toList();
		Map<ViewerIngredientIdentity, List<String>> candidates = new LinkedHashMap<>();
		Map<String, GroupDefinition> snapshot = new LinkedHashMap<>();
		Map<String, String> failures = new LinkedHashMap<>();
		priorityOrder.forEach(group -> snapshot.put(group.id(), group));
		long edges = 0;
		int max = 0;
		for (ViewerIngredient<E> ingredient : universe.ordered()) {
			List<GroupDefinition> applicable = switch (ingredient.kind()) {
				case ITEM -> itemGroups;
				case FLUID -> fluidGroups;
				case GENERIC -> genericGroups;
			};
			List<String> matches = new ArrayList<>();
			for (GroupDefinition group : applicable) {
				if (GroupEvaluations.evaluate(group, ingredient.view(), failures)
					== com.starskyxiii.collapsible_groups.group.filter.CompiledFilter.Evaluation.MATCH) matches.add(group.id());
			}
			if (!matches.isEmpty()) candidates.put(ingredient.identity(), List.copyOf(matches));
			edges += matches.size();
			max = Math.max(max, matches.size());
		}
		return GroupCandidateIndex.completed(candidates, snapshot, edges, universe.ordered().size(), max, universe, failures);
	}

	public static Map<ViewerIngredientIdentity, String> resolveOwnership(
		GroupCandidateIndex candidates,
		List<GroupDefinition> groups
	) {
		Map<String, GroupDefinition> current = new LinkedHashMap<>();
		for (GroupDefinition group : groups) current.put(group.id(), group);
		Map<ViewerIngredientIdentity, String> result = new LinkedHashMap<>();
		candidates.candidates().forEach((identity, groupIds) -> {
			for (String groupId : groupIds) {
				GroupDefinition group = current.get(groupId);
				if (group != null && com.starskyxiii.collapsible_groups.group.GroupRepository.isActive(group)) {
					result.put(identity, groupId);
					break;
				}
			}
		});
		return Collections.unmodifiableMap(new LinkedHashMap<>(result));
	}

	/** A group's own member order first, then the viewer's order. */
	private static <E> Comparator<ViewerIngredient<E>> memberOrder(
		GroupDefinition group, Map<ViewerIngredientIdentity, Integer> positions
	) {
		return Comparator.<ViewerIngredient<E>>comparingInt(ingredient -> group.memberRank(memberId(ingredient)))
			.thenComparingInt(ingredient -> positions.getOrDefault(ingredient.identity(), Integer.MAX_VALUE));
	}

	private static String memberId(ViewerIngredient<?> ingredient) {
		var location = ingredient.view().resourceLocation();
		return location == null ? ingredient.identity().valueId() : location.toString();
	}

	/** With an explicit order the first member alone is the icon, so the best item shows; otherwise the first two. */
	private static <E> List<ViewerIngredient<E>> fallbackIcons(GroupDefinition group, List<ViewerIngredient<E>> members) {
		int count = group.hasMemberOrder() ? 1 : 2;
		return members.subList(0, Math.min(count, members.size()));
	}

	/**
	 * Builds the fold of a group that sits in a tree. Nested folds come before the group's own members.
	 * A group whose content is a single entry hands that entry to its parent instead of folding it; during
	 * a search, a group with fewer members (nested ones included) than the threshold is shown unfolded.
	 */
	private static final class NestedBuilder<E> {
		private final GroupTree tree;
		private final ViewerSearchSnapshot<E> search;
		private final GroupExpansionState expansionState;
		private final Map<String, List<ViewerIngredient<E>>> items;
		private final Map<String, List<ViewerIngredient<E>>> fluids;
		private final Map<String, List<ViewerIngredient<E>>> generic;
		private final Map<ViewerIngredientIdentity, Integer> positions;

		NestedBuilder(GroupTree tree, ViewerSearchSnapshot<E> search, GroupExpansionState expansionState,
			Map<String, List<ViewerIngredient<E>>> items, Map<String, List<ViewerIngredient<E>>> fluids,
			Map<String, List<ViewerIngredient<E>>> generic, Map<ViewerIngredientIdentity, Integer> positions) {
			this.tree = tree;
			this.search = search;
			this.expansionState = expansionState;
			this.items = items;
			this.fluids = fluids;
			this.generic = generic;
			this.positions = positions;
		}

		List<ViewerProjection.Entry<E>> build(String groupId) {
			GroupDefinition group = tree.group(groupId);
			List<ViewerProjection.GroupHeader<E>> subs = new ArrayList<>();
			List<ViewerIngredient<E>> members = new ArrayList<>();
			members.addAll(items.getOrDefault(groupId, List.of()));
			members.addAll(fluids.getOrDefault(groupId, List.of()));
			members.addAll(generic.getOrDefault(groupId, List.of()));
			for (String child : tree.children(groupId)) {
				for (ViewerProjection.Entry<E> entry : build(child)) {
					switch (entry) {
						case ViewerProjection.GroupHeader<E> header -> subs.add(header);
						case ViewerProjection.IngredientEntry<E> ingredient -> members.add(ingredient.ingredient());
					}
				}
			}
			subs.sort(Comparator.<ViewerProjection.GroupHeader<E>>comparingInt(sub -> group.memberRank(sub.group().id()))
				.thenComparingInt(this::firstPosition));
			Comparator<ViewerIngredient<E>> byKind = Comparator.comparingInt(ingredient -> ingredient.kind().ordinal());
			members.sort(byKind.thenComparing(memberOrder(group, positions)));

			int total = members.size();
			for (ViewerProjection.GroupHeader<E> sub : subs) total += sub.allIngredients().size();
			if (total == 0) return List.of();
			if (shouldUngroupForSearch(search, total)) {
				List<ViewerProjection.Entry<E>> flat = new ArrayList<>(total);
				for (ViewerProjection.GroupHeader<E> sub : subs) {
					for (ViewerIngredient<E> ingredient : sub.allIngredients()) flat.add(new ViewerProjection.IngredientEntry<>(ingredient));
				}
				for (ViewerIngredient<E> ingredient : members) flat.add(new ViewerProjection.IngredientEntry<>(ingredient));
				return flat;
			}
			if (subs.size() + members.size() == 1) {
				return List.of(subs.isEmpty() ? new ViewerProjection.IngredientEntry<>(members.getFirst()) : subs.getFirst());
			}
			int itemCount = 0;
			int fluidCount = 0;
			for (ViewerIngredient<E> member : members) {
				if (member.kind() == ViewerIngredient.Kind.ITEM) itemCount++;
				else if (member.kind() == ViewerIngredient.Kind.FLUID) fluidCount++;
			}
			List<ViewerIngredient<E>> icons = subs.isEmpty() ? fallbackIcons(group, members) : subs.getFirst().fallbackIconIngredients();
			return List.of(new ViewerProjection.GroupHeader<>(group, members, itemCount, fluidCount,
				members.size() - itemCount - fluidCount, expansionState.isExpanded(groupId), group.iconIds(), icons, subs));
		}

		private int firstPosition(ViewerProjection.GroupHeader<E> header) {
			int first = Integer.MAX_VALUE;
			for (ViewerIngredient<E> ingredient : header.allIngredients()) {
				first = Math.min(first, positions.getOrDefault(ingredient.identity(), Integer.MAX_VALUE));
			}
			return first;
		}
	}

	private static boolean shouldUngroupForSearch(ViewerSearchSnapshot<?> search, int childCount) {
		return search.ungroupSmallGroups()
			&& search.searchActive()
			&& search.ungroupThreshold() > 0
			&& childCount >= 2
			&& childCount < search.ungroupThreshold();
	}
}
