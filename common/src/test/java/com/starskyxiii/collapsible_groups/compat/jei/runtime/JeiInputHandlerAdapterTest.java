package com.starskyxiii.collapsible_groups.compat.jei.runtime;

import mezz.jei.common.input.IInputTarget;
import mezz.jei.common.input.IInternalKeyMappings;
import mezz.jei.common.input.UserInput;
import mezz.jei.common.input.interaction.IInputInteraction;
import mezz.jei.common.input.interaction.InputAction;
import net.minecraft.client.gui.screens.Screen;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class JeiInputHandlerAdapterTest {
	@Test
	void visibleButtonHasPriorityAndHiddenOrUnhandledEventsFallThrough() {
		var button = new Target();
		var original = new Target();
		var visible = new AtomicBoolean(true);
		IInputTarget wrapped = JeiInputHandlerAdapter.wrap(button, original, visible::get);
		assertSame(button.action, wrapped.beginInput(null, null, null).orElseThrow());
		assertEquals(0, original.presses);
		button.accept = false;
		assertSame(original.action, wrapped.beginInput(null, null, null).orElseThrow());
		visible.set(false);
		button.accept = true;
		assertSame(original.action, wrapped.beginInput(null, null, null).orElseThrow());
		assertEquals(2, button.presses);
		assertTrue(wrapped.scroll(0, 0, 0, 1));
		assertEquals(0, button.scrolls);
		assertEquals(1, original.scrolls);
		wrapped.resetInput();
		assertEquals(1, button.resets);
		assertEquals(1, original.resets);
	}

	private static final class Target implements IInputTarget {
		final InputAction action = InputAction.run(() -> {});
		boolean accept = true;
		int presses, scrolls, resets;
		public Optional<IInputInteraction> beginInput(Screen screen, UserInput input, IInternalKeyMappings keys) {
			presses++;
			return accept ? Optional.of(action) : Optional.empty();
		}
		public boolean scroll(double x, double y, double dx, double dy) { scrolls++; return true; }
		public void resetInput() { resets++; }
	}
}
