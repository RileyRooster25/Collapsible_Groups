package com.starskyxiii.collapsible_groups.compat.emi;

import com.starskyxiii.collapsible_groups.group.GroupDefinition;
import com.starskyxiii.collapsible_groups.group.filter.GroupFilter;
import com.starskyxiii.collapsible_groups.ingredient.IngredientView;
import com.starskyxiii.collapsible_groups.viewer.ViewerIngredient;
import com.starskyxiii.collapsible_groups.viewer.ViewerIngredientIdentity;
import com.starskyxiii.collapsible_groups.viewer.ViewerProjection;
import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GroupHeaderEmiStackTest {
	@Test void previewRenderersAreReusedImmutableAndKeepRenderingTheirIngredients() {
		List<List<Object>> renderCalls = new ArrayList<>();
		EmiIngredient delegate = (EmiIngredient) Proxy.newProxyInstance(EmiIngredient.class.getClassLoader(),
			new Class<?>[]{EmiIngredient.class}, (proxy, method, args) -> {
				if (method.getName().equals("render")) {
					renderCalls.add(java.util.Arrays.asList(args));
				}
				return null;
			});
		ViewerIngredient<EmiIngredient> child = new ViewerIngredient<>(
			new ViewerIngredientIdentity("item", "stone"), ViewerIngredient.Kind.ITEM, delegate,
			new IngredientView() {
				@Override public String ingredientType() { return "item"; }
				@Override public ResourceLocation resourceLocation() { return null; }
				@Override public boolean hasTag(ResourceLocation tagId) { return false; }
				@Override public boolean matchesExactStack(String encodedStack) { return false; }
			});
		GroupDefinition group = new GroupDefinition("group", "Group", true,
			new GroupFilter.Id("item", "minecraft:stone"));
		GroupHeaderEmiStack stack = new GroupHeaderEmiStack(new ViewerProjection.GroupHeader<>(
			group, List.of(child), 1, 0, 0, false, List.of(), List.of(child)));
		var entries = stack.previewEntries();
		assertSame(entries, stack.previewEntries());
		assertThrows(UnsupportedOperationException.class, entries::clear);
		assertTrue(renderCalls.isEmpty());
		entries.get(0).render(null, 11, 21);
		entries.get(0).render(null, 12, 22);
		assertEquals(2, renderCalls.size());
		assertEquals(java.util.Arrays.asList(null, 11, 21, 0f, EmiIngredient.RENDER_ICON), renderCalls.get(0));
		assertEquals(java.util.Arrays.asList(null, 12, 22, 0f, EmiIngredient.RENDER_ICON), renderCalls.get(1));
	}
}
