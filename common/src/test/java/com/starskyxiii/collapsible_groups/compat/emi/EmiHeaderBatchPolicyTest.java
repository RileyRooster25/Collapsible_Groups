package com.starskyxiii.collapsible_groups.compat.emi;

import dev.emi.emi.api.stack.EmiIngredient;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class EmiHeaderBatchPolicyTest {
	private static final EmiHeaderBatchPolicy.VisualState PLAIN =
		new EmiHeaderBatchPolicy.VisualState(false, false, false, false, false, true);
	private static final int PREVIEW_FLAGS = -1 & ~EmiIngredient.RENDER_AMOUNT;

	@Test void flagsAndEveryVisibleDecorationGateMustPermitTheWholePreview() {
		assertTrue(EmiHeaderBatchPolicy.allows(PREVIEW_FLAGS, PLAIN));
		assertFalse(EmiHeaderBatchPolicy.allows(PREVIEW_FLAGS & ~EmiIngredient.RENDER_ICON, PLAIN));
		assertFalse(EmiHeaderBatchPolicy.allows(PREVIEW_FLAGS | EmiIngredient.RENDER_AMOUNT, PLAIN));
		for (var state : List.of(
			new EmiHeaderBatchPolicy.VisualState(true, false, false, false, false, true),
			new EmiHeaderBatchPolicy.VisualState(false, true, false, false, false, true),
			new EmiHeaderBatchPolicy.VisualState(false, false, true, false, false, true),
			new EmiHeaderBatchPolicy.VisualState(false, false, false, true, false, true),
			new EmiHeaderBatchPolicy.VisualState(false, false, false, false, true, true),
			new EmiHeaderBatchPolicy.VisualState(false, false, false, false, false, false))) {
			assertFalse(EmiHeaderBatchPolicy.allows(PREVIEW_FLAGS, state), state.toString());
		}
		var remainder = new EmiHeaderBatchPolicy.VisualState(false, false, false, false, false, false);
		assertTrue(EmiHeaderBatchPolicy.allows(PREVIEW_FLAGS & ~EmiIngredient.RENDER_REMAINDER, remainder));
	}

	@Test void decoratorFactoriesAreUnknownUntilVerifiedAndHandlerResultsAreNeverMemoized() {
		Object air = new Object(), ordinary = new Object(), registered = new Object();
		FakeDecorators.lookup.clear();
		FakeDecorators.fail = false;
		var gate = EmiHeaderBatchPolicy.Decorations.inspect(FakeDecorators.class, Object.class);
		assertFalse(EmiHeaderBatchPolicy.Decorations.unknown().empty(ordinary, air));
		assertFalse(EmiHeaderBatchPolicy.Decorations.inspect(Object.class, Object.class).empty(ordinary, air));
		assertTrue(gate.empty(ordinary, air));
		FakeDecorators.lookup.put(registered, new FakeDecorators());
		assertFalse(gate.empty(registered, air));

		// Native init allocates one distinct handler per registered item, including AIR.
		FakeDecorators.lookup.put(air, new FakeDecorators());
		assertFalse(gate.empty(ordinary, air));
		assertFalse(gate.empty(registered, air));
		FakeDecorators.lookup.clear();
		assertTrue(gate.empty(ordinary, air));
		FakeDecorators.fail = true;
		try { assertFalse(gate.empty(ordinary, air)); }
		finally { FakeDecorators.fail = false; }
		assertFalse(EmiHeaderBatchPolicy.Decorations.inspect(WrongFactory.class, Object.class).empty(ordinary, air));
	}

	@Test void onlyInheritedKnownNativeTransformDefaultsAreEligibleWithoutInvokingThem() {
		Set<String> known = Set.of(NativeTransform.class.getName());
		assertTrue(transform(NoTransform.class, known));
		assertTrue(transform(OrdinaryModel.class, known));
		assertFalse(transform(CustomModel.class, known));
		assertFalse(transform(CustomDefaultModel.class, known));
	}

	@Test void geometryKeepsBackFrontOrderAndFlushesBeforeNativeDecorationsAndMarker() {
		List<String> events = new ArrayList<>();
		try (var lighting = new EmiHeaderBatchPolicy.Lighting(() -> events.add("flush"),
			flat -> events.add(flat ? "flat" : "3d"))) {
			for (var layer : EmiHeaderBatchPolicy.LAYERS) {
				lighting.prepare(false);
				events.add("geometry:" + layer);
			}
		}
		for (var layer : EmiHeaderBatchPolicy.LAYERS) events.add("decorations:" + layer);
		events.add("marker");
		assertEquals(List.of(
			"flush", "3d",
			"geometry:Layer[index=1, x=2, y=0, z=0]",
			"geometry:Layer[index=0, x=0, y=2, z=10]",
			"flush", "3d",
			"decorations:Layer[index=1, x=2, y=0, z=0]",
			"decorations:Layer[index=0, x=0, y=2, z=10]", "marker"
		), events);
	}

	@Test void lightingChangesSubmitTheOldBatchBeforeNewLightingAndRestoreOnSubmissionFailure() {
		List<String> events = new ArrayList<>();
		try (var lighting = new EmiHeaderBatchPolicy.Lighting(() -> events.add("flush"),
			flat -> events.add(flat ? "flat" : "3d"))) {
			lighting.prepare(true);
			events.add("flat geometry");
			lighting.prepare(false);
			events.add("3d geometry");
		}
		assertEquals(List.of("flush", "flat", "flat geometry", "flush", "3d", "3d geometry", "flush", "3d"), events);
		AtomicBoolean restored = new AtomicBoolean();
		var failing = new EmiHeaderBatchPolicy.Lighting(
			() -> { throw new IllegalStateException("submission"); }, flat -> restored.set(!flat));
		assertThrows(IllegalStateException.class, failing::close);
		assertTrue(restored.get());
	}

	private static boolean transform(Class<?> type, Set<String> known) {
		return EmiHeaderBatchPolicy.nativeTransform(type, known, Object.class, Object.class);
	}

	public static final class FakeDecorators {
		private static final FakeDecorators EMPTY = new FakeDecorators();
		private static final Map<Object, FakeDecorators> lookup = new IdentityHashMap<>();
		private static boolean fail;
		public static FakeDecorators of(Object stack) {
			if (fail) throw new IllegalStateException("factory");
			return lookup.getOrDefault(stack, EMPTY);
		}
	}
	public static final class WrongFactory { public static Object of(Object stack) { return new Object(); } }
	public interface NativeTransform {
		default Object applyTransform(Object context, Object pose, boolean left) { return this; }
	}
	public interface CustomDefault extends NativeTransform {
		@Override default Object applyTransform(Object context, Object pose, boolean left) {
			throw new AssertionError("must not invoke transforms during eligibility checks");
		}
	}
	public static class NoTransform {}
	public static class OrdinaryModel implements NativeTransform {}
	public static class CustomModel extends OrdinaryModel {
		@Override public Object applyTransform(Object context, Object pose, boolean left) {
			throw new AssertionError("must not invoke transforms during eligibility checks");
		}
	}
	public static class CustomDefaultModel implements CustomDefault {}
}
