package com.starskyxiii.collapsible_groups.viewer;

import com.starskyxiii.collapsible_groups.group.GroupDefinition;
import com.starskyxiii.collapsible_groups.group.GroupIconDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Structured group projection ready for translation into viewer-specific elements. */
public record ViewerProjection<E>(
	List<Entry<E>> entries,
	Map<ViewerIngredientIdentity, String> ownership
) {
	public ViewerProjection {
		entries = List.copyOf(entries);
		ownership = Collections.unmodifiableMap(new LinkedHashMap<>(ownership));
	}

	public List<DisplayEntry<E>> displayEntries() {
		List<DisplayEntry<E>> result = new ArrayList<>();
		for (Entry<E> entry : entries) {
			switch (entry) {
				case IngredientEntry<E> ingredient -> result.add(
					new DisplayIngredient<>(ingredient.ingredient(), Optional.empty())
				);
				case GroupHeader<E> header -> addHeader(header, result);
			}
		}
		return List.copyOf(result);
	}

	/** A header, then, when it is expanded, its sub-group headers (recursively) and its own members. */
	private static <E> void addHeader(GroupHeader<E> header, List<DisplayEntry<E>> result) {
		result.add(new DisplayHeader<>(header));
		if (!header.expanded()) return;
		for (GroupHeader<E> sub : header.subHeaders()) addHeader(sub, result);
		for (ViewerIngredient<E> child : header.children()) {
			result.add(new DisplayIngredient<>(child, Optional.of(header.group().id())));
		}
	}

	public ViewerProjection<E> withExpansion(GroupExpansionState expansionState) {
		List<Entry<E>> updated = new ArrayList<>(entries.size());
		for (Entry<E> entry : entries) {
			updated.add(switch (entry) {
				case IngredientEntry<E> ingredient -> ingredient;
				case GroupHeader<E> header -> header.withExpansion(expansionState);
			});
		}
		return new ViewerProjection<>(updated, ownership);
	}

	public sealed interface Entry<E> permits IngredientEntry, GroupHeader {}

	public record IngredientEntry<E>(ViewerIngredient<E> ingredient) implements Entry<E> {
		public IngredientEntry {
			Objects.requireNonNull(ingredient, "ingredient");
		}
	}

	/**
	 * One group fold. {@code children} are the group's own members; {@code subHeaders} are the folds
	 * of groups nested under it, shown before its own members when it is expanded.
	 */
	public record GroupHeader<E>(
		GroupDefinition group,
		List<ViewerIngredient<E>> children,
		int itemCount,
		int fluidCount,
		int genericCount,
		boolean expanded,
		List<GroupIconDefinition> iconIds,
		List<ViewerIngredient<E>> fallbackIconIngredients,
		List<GroupHeader<E>> subHeaders
	) implements Entry<E> {
		public GroupHeader {
			Objects.requireNonNull(group, "group");
			children = List.copyOf(children);
			iconIds = List.copyOf(iconIds);
			fallbackIconIngredients = List.copyOf(fallbackIconIngredients);
			subHeaders = List.copyOf(subHeaders);
			if (itemCount + fluidCount + genericCount != children.size()) {
				throw new IllegalArgumentException("Ingredient counts must equal child count");
			}
		}

		public GroupHeader(
			GroupDefinition group,
			List<ViewerIngredient<E>> children,
			int itemCount,
			int fluidCount,
			int genericCount,
			boolean expanded,
			List<GroupIconDefinition> iconIds,
			List<ViewerIngredient<E>> fallbackIconIngredients
		) {
			this(group, children, itemCount, fluidCount, genericCount, expanded, iconIds, fallbackIconIngredients, List.of());
		}

		public GroupHeader<E> withExpansion(GroupExpansionState expansionState) {
			List<GroupHeader<E>> subs = new ArrayList<>(subHeaders.size());
			for (GroupHeader<E> sub : subHeaders) subs.add(sub.withExpansion(expansionState));
			return new GroupHeader<>(group, children, itemCount, fluidCount, genericCount,
				expansionState.isExpanded(group.id()), iconIds, fallbackIconIngredients, subs);
		}

		public GroupHeader<E> withFallbackIcons(List<ViewerIngredient<E>> icons) {
			return new GroupHeader<>(group, children, itemCount, fluidCount, genericCount, expanded, iconIds, icons, subHeaders);
		}

		/** Every member of this fold and of the folds nested under it, sub-groups first. */
		public List<ViewerIngredient<E>> allIngredients() {
			List<ViewerIngredient<E>> all = new ArrayList<>();
			for (GroupHeader<E> sub : subHeaders) all.addAll(sub.allIngredients());
			all.addAll(children);
			return all;
		}

		public int totalItemCount() {
			int total = itemCount;
			for (GroupHeader<E> sub : subHeaders) total += sub.totalItemCount();
			return total;
		}

		public int totalFluidCount() {
			int total = fluidCount;
			for (GroupHeader<E> sub : subHeaders) total += sub.totalFluidCount();
			return total;
		}

		public int totalGenericCount() {
			int total = genericCount;
			for (GroupHeader<E> sub : subHeaders) total += sub.totalGenericCount();
			return total;
		}
	}

	public sealed interface DisplayEntry<E> permits DisplayIngredient, DisplayHeader {}

	public record DisplayIngredient<E>(
		ViewerIngredient<E> ingredient,
		Optional<String> parentGroupId
	) implements DisplayEntry<E> {
		public DisplayIngredient {
			Objects.requireNonNull(ingredient, "ingredient");
			Objects.requireNonNull(parentGroupId, "parentGroupId");
		}
	}

	public record DisplayHeader<E>(GroupHeader<E> header) implements DisplayEntry<E> {
		public DisplayHeader {
			Objects.requireNonNull(header, "header");
		}
	}
}
