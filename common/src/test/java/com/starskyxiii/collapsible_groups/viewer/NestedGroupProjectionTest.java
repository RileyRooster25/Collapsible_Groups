package com.starskyxiii.collapsible_groups.viewer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.starskyxiii.collapsible_groups.group.GroupDefinition;
import com.starskyxiii.collapsible_groups.group.GroupTheme;
import com.starskyxiii.collapsible_groups.group.filter.Filters;
import com.starskyxiii.collapsible_groups.group.filter.GroupFilter;
import com.starskyxiii.collapsible_groups.ingredient.IngredientView;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Groups nested through extra.parent, and member order through extra.order. */
@SuppressWarnings("unchecked")
class NestedGroupProjectionTest {
	private static final GroupFilter NOTHING = Filters.itemId("test:nothing");

	private final ViewerIngredient<String> loose = item("loose");
	private final ViewerIngredient<String> swordA = item("sword_a");
	private final ViewerIngredient<String> pickA = item("pick_a");
	private final ViewerIngredient<String> gunA = item("gun_a");
	private final ViewerIngredient<String> swordB = item("sword_b");
	private final ViewerIngredient<String> gunB = item("gun_b");
	private final ViewerIngredient<String> pickB = item("pick_b");
	private final ViewerIngredient<String> shield = item("shield");
	private final List<ViewerIngredient<String>> filtered = List.of(loose, swordA, pickA, gunA, swordB, gunB, pickB, shield);

	private final List<GroupDefinition> groups = List.of(
		group("equipment", NOTHING, null, "tools", "weapons"),
		group("weapons", NOTHING, "equipment", "guns", "swords"),
		group("tools", Filters.any(Filters.itemId("test:pick_a"), Filters.itemId("test:pick_b")), "equipment"),
		group("swords", Filters.any(Filters.itemId("test:sword_a"), Filters.itemId("test:sword_b")), "weapons", "test:sword_b"),
		group("guns", Filters.any(Filters.itemId("test:gun_a"), Filters.itemId("test:gun_b")), "weapons"),
		group("shields", Filters.itemId("test:shield"), "equipment")
	);

	@Test
	void nestsHeadersUnderTheirParentInDeclaredOrder() {
		ViewerProjection<String> projection = project("", Set.of("equipment", "weapons", "tools", "swords", "guns"));

		assertEquals(2, projection.entries().size());
		assertEquals("loose", assertInstanceOf(ViewerProjection.IngredientEntry.class, projection.entries().get(0)).ingredient().entry());
		ViewerProjection.GroupHeader<String> equipment = header(projection.entries().get(1));
		assertEquals("equipment", equipment.group().id());
		assertEquals(List.of("tools", "weapons"), ids(equipment.subHeaders()));
		assertEquals(List.of("shield"), entries(equipment.children()), "a one-member group rolls up into its parent");
		ViewerProjection.GroupHeader<String> weapons = equipment.subHeaders().get(1);
		assertEquals(List.of("guns", "swords"), ids(weapons.subHeaders()));
		ViewerProjection.GroupHeader<String> swords = weapons.subHeaders().get(1);
		assertEquals(List.of("sword_b", "sword_a"), entries(swords.children()), "listed members come first");
		assertEquals(List.of("sword_b"), entries(swords.fallbackIconIngredients()), "the best member alone is the icon");
		assertEquals(7, equipment.totalItemCount());

		assertEquals(List.of("loose", "[equipment]", "[tools]", "pick_a", "pick_b", "[weapons]", "[guns]", "gun_a", "gun_b",
			"[swords]", "sword_b", "sword_a", "shield"), display(projection));
	}

	@Test
	void collapsingAnAncestorHidesEveryDescendant() {
		assertEquals(List.of("loose", "[equipment]"), display(project("", Set.of("weapons", "tools", "swords", "guns"))));
		assertEquals(List.of("loose", "[equipment]", "[tools]", "[weapons]", "shield"),
			display(project("", Set.of("equipment", "swords"))));
		assertEquals(List.of("loose", "[equipment]", "[tools]", "[weapons]", "shield"),
			display(project("", Set.of("equipment", "swords")).withExpansion(Set.of("equipment")::contains)));
	}

	@Test
	void searchShowsASmallTreeUnfolded() {
		ViewerProjection<String> projection = GroupProjectionEngine.project(new ViewerIngredientUniverse<>(filtered),
			new ViewerSearchSnapshot<>("query", List.of(swordA, swordB), true, 3), groups, id -> false);
		assertEquals(List.of("sword_a", "sword_b"), display(projection), "unfolded results keep the search order, as upstream");
	}

	@Test
	void aTreeWithOneVisibleFoldShowsThatFoldAlone() {
		ViewerProjection<String> projection = GroupProjectionEngine.project(new ViewerIngredientUniverse<>(filtered),
			new ViewerSearchSnapshot<>("", List.of(swordA, swordB), true, 3), groups, id -> false);
		assertEquals("swords", header(projection.entries().getFirst()).group().id());
	}

	@Test
	void badParentsAreTreatedAsTopLevel() {
		GroupTree tree = GroupTree.of(List.of(
			group("self", NOTHING, "self"),
			group("orphan", NOTHING, "missing"),
			group("cycle_a", NOTHING, "cycle_b"),
			group("cycle_b", NOTHING, "cycle_a"),
			group("child", NOTHING, "cycle_a")
		));
		assertNull(tree.parent("self"));
		assertNull(tree.parent("orphan"));
		assertNull(tree.parent("cycle_a"));
		assertNull(tree.parent("cycle_b"));
		assertEquals("cycle_a", tree.parent("child"));
	}

	@Test
	void groupsWithoutParentsKeepUpstreamProjection() {
		GroupDefinition flat = group("flat", Filters.any(Filters.itemId("test:gun_a"), Filters.itemId("test:gun_b")), null);
		ViewerProjection<String> projection = GroupProjectionEngine.project(new ViewerIngredientUniverse<>(filtered),
			new ViewerSearchSnapshot<>("", filtered, true, 3), List.of(flat), id -> true);
		assertEquals(List.of("loose", "sword_a", "pick_a", "[flat]", "gun_a", "gun_b", "sword_b", "pick_b", "shield"),
			display(projection));
		assertEquals(List.of("gun_a", "gun_b"), entries(header(projection.entries().get(3)).fallbackIconIngredients()));
	}

	private ViewerProjection<String> project(String query, Set<String> expanded) {
		return GroupProjectionEngine.project(new ViewerIngredientUniverse<>(filtered),
			new ViewerSearchSnapshot<>(query, filtered, true, 3), groups, expanded::contains);
	}

	private static GroupDefinition group(String id, GroupFilter filter, String parent, String... order) {
		JsonObject extra = new JsonObject();
		if (parent != null) extra.addProperty("parent", parent);
		if (order.length > 0) {
			JsonArray list = new JsonArray();
			for (String member : order) list.add(member);
			extra.add("order", list);
		}
		return new GroupDefinition(id, id, true, filter, List.of(), GroupTheme.EMPTY, 0, extra);
	}

	private static ViewerProjection.GroupHeader<String> header(ViewerProjection.Entry<String> entry) {
		return assertInstanceOf(ViewerProjection.GroupHeader.class, entry);
	}

	private static List<String> display(ViewerProjection<String> projection) {
		return projection.displayEntries().stream().map(entry -> switch (entry) {
			case ViewerProjection.DisplayHeader<String> header -> "[" + header.header().group().id() + "]";
			case ViewerProjection.DisplayIngredient<String> ingredient -> ingredient.ingredient().entry();
		}).toList();
	}

	private static List<String> ids(List<ViewerProjection.GroupHeader<String>> headers) {
		return headers.stream().map(header -> header.group().id()).toList();
	}

	private static List<String> entries(List<ViewerIngredient<String>> ingredients) {
		return ingredients.stream().map(ViewerIngredient::entry).toList();
	}

	private static ViewerIngredient<String> item(String path) {
		String id = "test:" + path;
		return new ViewerIngredient<>(new ViewerIngredientIdentity("item", id), ViewerIngredient.Kind.ITEM, path,
			new FakeIngredientView("item", ResourceLocation.parse(id)));
	}

	private record FakeIngredientView(String ingredientType, ResourceLocation resourceLocation) implements IngredientView {
		@Override
		public boolean hasTag(ResourceLocation tagId) {
			return false;
		}

		@Override
		public boolean matchesExactStack(String encodedStack) {
			return false;
		}

		@Override
		public boolean matchesDecodedExactStack(ItemStack decoded) {
			return false;
		}
	}
}
