package com.starskyxiii.collapsible_groups.compat.emi;

import dev.emi.emi.api.stack.EmiIngredient;

import java.util.List;
import java.util.function.Supplier;

/** Small last-mile cache used because ScreenSpace asks for the same list during layout, render and input. */
public final class EmiProjectionController {
	private static volatile long revision;

	private EmiProjectionController() {}

	public static List<? extends EmiIngredient> project(
		Cache cache, List<? extends EmiIngredient> original, boolean searchPanel
	) {
		String text = searchPanel ? dev.emi.emi.api.EmiApi.getSearchText() : "";
		EmiViewerAdapter.ProjectionCacheKey currentReadiness = EmiViewerAdapter.instance().projectionCacheKey();
		return cache.project(original, searchPanel, text, currentReadiness,
			() -> EmiViewerAdapter.instance().projectIndex(original, searchPanel));
	}

	static boolean shouldMemoize(EmiViewerAdapter.ProjectionCacheKey key) {
		return key.bootstrapReady() && key.indexReady();
	}

	public static synchronized void clearCache() {
		revision++;
	}

	/** Owned by one ScreenSpace, so different INDEX sidebars cannot evict one another. */
	public static final class Cache {
		private Object originalIdentity;
		private int originalSize;
		private int fingerprint;
		private boolean search;
		private String searchText = "";
		private EmiViewerAdapter.ProjectionCacheKey readinessKey;
		private long cachedRevision = -1;
		private List<? extends EmiIngredient> projected;

		synchronized List<? extends EmiIngredient> project(List<? extends EmiIngredient> original,
			boolean searchPanel, String text, EmiViewerAdapter.ProjectionCacheKey currentReadiness,
			Supplier<List<? extends EmiIngredient>> projector) {
			long currentRevision = revision;
			int currentFingerprint = fingerprint(original);
			if (cachedRevision == currentRevision && originalIdentity == original && originalSize == original.size()
				&& fingerprint == currentFingerprint && search == searchPanel
				&& searchText.equals(text) && currentReadiness.equals(readinessKey)
				&& projected != null) return projected;
			List<? extends EmiIngredient> result = projector.get();
			originalIdentity = original;
			originalSize = original.size();
			fingerprint = currentFingerprint;
			search = searchPanel;
			searchText = text;
			readinessKey = currentReadiness;
			cachedRevision = currentRevision;
			// A raw fallback while bootstrap/index is unavailable is transient and must never be memoized.
			projected = shouldMemoize(currentReadiness) ? result : null;
			return result;
		}
	}

	private static int fingerprint(List<? extends EmiIngredient> list) {
		int hash = list.size();
		if (!list.isEmpty()) {
			hash = 31 * hash + System.identityHashCode(list.get(0));
			hash = 31 * hash + System.identityHashCode(list.get(list.size() - 1));
		}
		return hash;
	}
}
