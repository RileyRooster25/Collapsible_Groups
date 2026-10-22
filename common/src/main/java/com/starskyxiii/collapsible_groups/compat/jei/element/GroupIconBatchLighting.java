package com.starskyxiii.collapsible_groups.compat.jei.element;

import java.util.function.Consumer;

/** Flushes pending geometry before changing the lighting used by a header item batch. */
final class GroupIconBatchLighting implements AutoCloseable {
	private final Runnable flush;
	private final Consumer<Boolean> setFlatLighting;
	private Boolean flatLighting;

	GroupIconBatchLighting(Runnable flush, Consumer<Boolean> setFlatLighting) {
		this.flush = flush;
		this.setFlatLighting = setFlatLighting;
	}

	void prepare(boolean flat) {
		if (flatLighting == null || flatLighting != flat) {
			flush.run();
			setFlatLighting.accept(flat);
			flatLighting = flat;
		}
	}

	/** Third-party renderers may change the lighting, so the next item must establish it again. */
	void beforeFallback() {
		try {
			flush.run();
		} finally {
			setFlatLighting.accept(false);
			flatLighting = null;
		}
	}

	@Override
	public void close() {
		try {
			flush.run();
		} finally {
			setFlatLighting.accept(false);
			flatLighting = null;
		}
	}
}
