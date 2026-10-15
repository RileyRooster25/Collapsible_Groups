package com.starskyxiii.collapsible_groups.compat.jei.runtime;

import mezz.jei.common.input.IInputTarget;
import mezz.jei.common.input.handlers.InputGroup;

import java.util.function.BooleanSupplier;

final class JeiInputHandlerAdapter {
	private JeiInputHandlerAdapter() {}

	static IInputTarget wrap(IInputTarget groupsButton, IInputTarget original, BooleanSupplier visible) {
		return new InputGroup("IngredientListOverlay_withGroups",
			new InputGroup.Child(visible, groupsButton),
			new InputGroup.Child(() -> true, original));
	}
}
