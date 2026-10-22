package com.starskyxiii.collapsible_groups.compat.emi;

import dev.emi.emi.api.stack.EmiIngredient;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class EmiProjectionCacheTest {
	@Test void twoIndexSidebarsKeepTheirOwnProjectionsAndSearchChangesStayLocal() {
		EmiProjectionController.Cache search = new EmiProjectionController.Cache();
		EmiProjectionController.Cache index = new EmiProjectionController.Cache();
		List<EmiIngredient> source = new ArrayList<>();
		var ready = ready(1, 3);
		AtomicInteger searchBuilds = new AtomicInteger();
		AtomicInteger indexBuilds = new AtomicInteger();
		var searchResult = search.project(source, true, "stone", ready, () -> result(searchBuilds));
		var indexResult = index.project(source, false, "", ready, () -> result(indexBuilds));
		for (int i = 0; i < 5; i++) {
			assertSame(searchResult, search.project(source, true, "stone", ready, () -> result(searchBuilds)));
			assertSame(indexResult, index.project(source, false, "", ready, () -> result(indexBuilds)));
		}
		assertNotSame(searchResult, search.project(source, true, "dirt", ready, () -> result(searchBuilds)));
		assertSame(indexResult, index.project(source, false, "", ready, () -> result(indexBuilds)));
		assertEquals(2, searchBuilds.get());
		assertEquals(1, indexBuilds.get());
	}

	@Test void expansionInvalidationRebuildsEverySidebar() {
		EmiProjectionController.Cache left = new EmiProjectionController.Cache();
		EmiProjectionController.Cache right = new EmiProjectionController.Cache();
		List<EmiIngredient> source = new ArrayList<>();
		var ready = ready(1, 2);
		AtomicInteger builds = new AtomicInteger();
		left.project(source, false, "", ready, () -> result(builds));
		right.project(source, true, "stone", ready, () -> result(builds));
		EmiProjectionController.clearCache();
		left.project(source, false, "", ready, () -> result(builds));
		right.project(source, true, "stone", ready, () -> result(builds));
		assertEquals(4, builds.get());
	}

	@Test void sourceIdentityEpochAndIndexRevisionEachInvalidateProjection() {
		EmiProjectionController.Cache cache = new EmiProjectionController.Cache();
		List<EmiIngredient> first = new ArrayList<>();
		List<EmiIngredient> replacement = new ArrayList<>();
		AtomicInteger builds = new AtomicInteger();
		cache.project(first, true, "", ready(1, 1), () -> result(builds));
		cache.project(replacement, true, "", ready(1, 1), () -> result(builds));
		cache.project(replacement, true, "", ready(2, 1), () -> result(builds));
		cache.project(replacement, true, "", ready(2, 2), () -> result(builds));
		assertEquals(4, builds.get());
	}

	@Test void pendingBootstrapOrIndexFallbackIsNeverMemoized() {
		EmiProjectionController.Cache cache = new EmiProjectionController.Cache();
		List<EmiIngredient> source = new ArrayList<>();
		AtomicInteger builds = new AtomicInteger();
		var pending = new EmiViewerAdapter.ProjectionCacheKey(1, true, false, 1);
		cache.project(source, true, "", pending, () -> result(builds));
		cache.project(source, true, "", pending, () -> result(builds));
		var published = cache.project(source, true, "", ready(1, 2), () -> result(builds));
		assertSame(published, cache.project(source, true, "", ready(1, 2), () -> result(builds)));
		assertEquals(3, builds.get());
	}

	@Test void invalidationDuringBuildCannotPublishAReusableStaleProjection() {
		EmiProjectionController.Cache cache = new EmiProjectionController.Cache();
		List<EmiIngredient> source = new ArrayList<>();
		AtomicInteger builds = new AtomicInteger();
		cache.project(source, true, "", ready(1, 1), () -> {
			EmiProjectionController.clearCache();
			return result(builds);
		});
		cache.project(source, true, "", ready(1, 1), () -> result(builds));
		assertEquals(2, builds.get());
	}

	private static EmiViewerAdapter.ProjectionCacheKey ready(long epoch, long revision) {
		return new EmiViewerAdapter.ProjectionCacheKey(epoch, true, true, revision);
	}

	private static List<? extends EmiIngredient> result(AtomicInteger builds) {
		builds.incrementAndGet();
		return new ArrayList<>();
	}
}
