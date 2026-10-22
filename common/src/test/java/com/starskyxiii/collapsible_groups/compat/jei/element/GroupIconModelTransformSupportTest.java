package com.starskyxiii.collapsible_groups.compat.jei.element;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupIconModelTransformSupportTest {
	private static final Set<String> NATIVE_DEFAULTS = Set.of(NativeDefault.class.getName());

	@Test
	void modelsWithoutPlatformTransformAndInheritedNativeDefaultsCanBatch() {
		assertTrue(safe(FabricModel.class));
		assertTrue(safe(OrdinaryModel.class));
		assertTrue(safe(InheritedModel.class));
	}

	@Test
	void classOverridesAndCustomInterfaceDefaultsMustUseVanillaFallback() {
		assertFalse(safe(CustomModel.class));
		assertFalse(safe(CustomInterfaceModel.class));
		assertFalse(safe(UnknownDefaultModel.class));
	}

	@Test
	void inspectingCapabilityDoesNotInvokeTheTransform() {
		CustomModel.calls = 0;
		assertFalse(safe(CustomModel.class));
		org.junit.jupiter.api.Assertions.assertEquals(0, CustomModel.calls);
	}

	private static boolean safe(Class<?> type) {
		return GroupIconModelTransformSupport.hasSafeTransform(type, NATIVE_DEFAULTS, Object.class, Object.class);
	}

	public interface NativeDefault {
		default Object applyTransform(Object context, Object pose, boolean leftHand) { return this; }
	}

	public interface CustomDefault extends NativeDefault {
		@Override default Object applyTransform(Object context, Object pose, boolean leftHand) {
			throw new AssertionError("capability checks must not run transforms");
		}
	}

	public interface UnknownDefault {
		default Object applyTransform(Object context, Object pose, boolean leftHand) { return this; }
	}

	public static class FabricModel {}
	public static class OrdinaryModel implements NativeDefault {}
	public static class InheritedModel extends OrdinaryModel {}
	public static class CustomInterfaceModel implements CustomDefault {}
	public static class UnknownDefaultModel implements UnknownDefault {}
	public static class CustomModel extends OrdinaryModel {
		static int calls;
		@Override public Object applyTransform(Object context, Object pose, boolean leftHand) {
			calls++;
			throw new AssertionError("capability checks must not run transforms");
		}
	}
}
