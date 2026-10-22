package com.starskyxiii.collapsible_groups.compat.jei.element;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupIconBatchLightingTest {
	@Test
	void sameLightingSharesOneBoundaryAndChangesFlushBeforeNewLighting() {
		List<String> events = new ArrayList<>();
		try (GroupIconBatchLighting lighting = lighting(events)) {
			lighting.prepare(false);
			lighting.prepare(false);
			lighting.prepare(true);
			lighting.prepare(true);
			lighting.prepare(false);
		}
		assertEquals(List.of("flush", "3d", "flush", "flat", "flush", "3d", "flush", "3d"), events);
	}

	@Test
	void thirdPartyBarrierSubmitsGeometryAndReestablishesLightingAfterTheRenderer() {
		List<String> events = new ArrayList<>();
		try (GroupIconBatchLighting lighting = lighting(events)) {
			lighting.prepare(true);
			lighting.beforeFallback();
			events.add("third-party");
			lighting.prepare(false);
		}
		assertEquals(List.of("flush", "flat", "flush", "3d", "third-party",
			"flush", "3d", "flush", "3d"), events);
	}

	@Test
	void closingRestoresLightingEvenWhenSubmissionFails() {
		AtomicBoolean restored = new AtomicBoolean();
		GroupIconBatchLighting lighting = new GroupIconBatchLighting(
			() -> { throw new IllegalStateException("submission failed"); },
			flat -> restored.set(!flat)
		);
		assertThrows(IllegalStateException.class, lighting::close);
		assertTrue(restored.get());
	}

	private static GroupIconBatchLighting lighting(List<String> events) {
		return new GroupIconBatchLighting(() -> events.add("flush"),
			flat -> events.add(flat ? "flat" : "3d"));
	}
}
