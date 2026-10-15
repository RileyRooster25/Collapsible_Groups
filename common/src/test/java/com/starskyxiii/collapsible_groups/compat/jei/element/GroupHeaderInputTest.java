package com.starskyxiii.collapsible_groups.compat.jei.element;

import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IJeiKeyMapping;
import mezz.jei.common.input.IInternalKeyMappings;
import mezz.jei.common.input.UserInput;
import mezz.jei.common.input.InputPhase;
import com.starskyxiii.collapsible_groups.persistence.GroupExpandState;
import com.starskyxiii.collapsible_groups.compat.jei.runtime.GroupRegistry;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class GroupHeaderInputTest {
	@Test
	void leftClickTogglesOnlyOnCompletionAndCancellationKeepsState() {
		String id = "input_regression";
		var changes = new AtomicInteger();
		var icon = new GroupIcon(id, "test.input", "Input", List.of());
		@SuppressWarnings("unchecked")
		ITypedIngredient<GroupIcon> typed = (ITypedIngredient<GroupIcon>) Proxy.newProxyInstance(
			ITypedIngredient.class.getClassLoader(), new Class<?>[]{ITypedIngredient.class}, (p, m, a) -> switch (m.getName()) {
				case "getType" -> GroupIcon.TYPE;
				case "getIngredient" -> icon;
				default -> throw new AssertionError(m);
			});
		IJeiKeyMapping left = (IJeiKeyMapping) Proxy.newProxyInstance(IJeiKeyMapping.class.getClassLoader(),
			new Class<?>[]{IJeiKeyMapping.class}, (p, m, a) -> ((com.mojang.blaze3d.platform.InputConstants.Key) a[0]).getValue() == 0);
		IInternalKeyMappings keys = (IInternalKeyMappings) Proxy.newProxyInstance(IInternalKeyMappings.class.getClassLoader(),
			new Class<?>[]{IInternalKeyMappings.class}, (p, m, a) -> left);
		UserInput press = UserInput.fromVanilla(1, 1, 0, InputPhase.PRESS).orElseThrow();
		UserInput release = UserInput.fromVanilla(1, 1, 0, InputPhase.RELEASE).orElseThrow();
		var header = new GroupHeaderElement(typed, Component.empty(), List.of(), changes::incrementAndGet);
		GroupExpandState.load(Set.of());
		try {
			var action = header.prepareAction(press, keys).orElseThrow();
			assertFalse(GroupRegistry.isExpandedById(id));
			assertEquals(0, changes.get());
			action.within((x, y) -> false).complete(release);
			action.when(() -> false).complete(release);
			assertFalse(GroupRegistry.isExpandedById(id));
			assertEquals(0, changes.get());
			action.complete(release);
			assertTrue(GroupRegistry.isExpandedById(id));
			assertEquals(1, changes.get());
			header.prepareAction(press, keys).orElseThrow().complete(release);
			assertFalse(GroupRegistry.isExpandedById(id));
			assertEquals(2, changes.get());
			UserInput other = UserInput.fromVanilla(1, 1, 1, InputPhase.PRESS).orElseThrow();
			assertTrue(header.prepareAction(other, keys).isEmpty());
		} finally { GroupExpandState.load(Set.of()); }
	}
}
