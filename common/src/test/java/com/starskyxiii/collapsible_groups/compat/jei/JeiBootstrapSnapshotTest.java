package com.starskyxiii.collapsible_groups.compat.jei;

import com.starskyxiii.collapsible_groups.group.GroupIconDefinition;
import com.starskyxiii.collapsible_groups.group.GroupRepositoryTestAccess;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class JeiBootstrapSnapshotTest {
	@Test
	void oneOwnedSourceReusesIdentityResolutionAcrossSearchesButNotAcrossGenerationsOrManagers() {
		var fixture = new Fixture("test:search_cache");
		var oxygen = fixture.typed("test:oxygen");
		var source = new JeiViewerAdapter.IngredientSourceSnapshot(List.of(oxygen));
		var manager = fixture.manager();
		var adapter = JeiViewerAdapter.instance();
		GroupRepositoryTestAccess.replace(List.of());
		try {
			var first = adapter.updateBootstrap(source, manager);
			int resolved = fixture.uidCalls.get();
			assertTrue(resolved > 0);
			for (int query = 0; query < 30; query++) {
				assertSame(first, adapter.updateBootstrap(source, manager));
			}
			assertEquals(resolved, fixture.uidCalls.get());

			var nextSource = new JeiViewerAdapter.IngredientSourceSnapshot(List.of(oxygen));
			var next = adapter.updateBootstrap(nextSource, manager);
			assertNotSame(first, next);
			assertEquals(resolved * 2, fixture.uidCalls.get());
			var nextManager = adapter.updateBootstrap(nextSource, fixture.manager());
			assertNotSame(next, nextManager);
			assertEquals(resolved * 3, fixture.uidCalls.get());

			adapter.unregisterRuntime();
			assertNotSame(first, adapter.updateBootstrap(source, manager));
			assertEquals(resolved * 4, fixture.uidCalls.get());
		} finally {
			adapter.unregisterRuntime();
			GroupRepositoryTestAccess.replace(List.of());
		}
	}

	@Test
	void snapshotsOwnListMembershipAndLegacyListCallsStillObserveCallerChanges() {
		var fixture = new Fixture("test:owned_source");
		var oxygen = fixture.typed("test:oxygen");
		var hydrogen = fixture.typed("test:hydrogen");
		List<ITypedIngredient<?>> mutable = new ArrayList<>(List.of(oxygen));
		var source = new JeiViewerAdapter.IngredientSourceSnapshot(mutable);
		var manager = fixture.manager();
		var adapter = JeiViewerAdapter.instance();
		GroupRepositoryTestAccess.replace(List.of());
		try {
			var first = adapter.updateBootstrap(mutable, manager);
			mutable.clear();
			mutable.add(hydrogen);
			assertEquals(List.of(oxygen), source.ingredients());
			assertThrows(UnsupportedOperationException.class, () -> source.ingredients().clear());
			var changed = adapter.updateBootstrap(mutable, manager);
			assertNotSame(first, changed);
			assertSame(hydrogen, changed.universe().ordered().get(0).entry());
			assertSame(oxygen, adapter.updateBootstrap(source, manager).universe().ordered().get(0).entry());
		} finally {
			adapter.unregisterRuntime();
			GroupRepositoryTestAccess.replace(List.of());
		}
	}

	@Test
	void headerIconsUseTheirCapturedProjectionContextAfterAnotherBootstrapIsPublished() {
		var fixture = new Fixture("test:captured_header");
		var oxygen = fixture.typed("test:oxygen");
		var manager = fixture.manager();
		var adapter = JeiViewerAdapter.instance();
		GroupRepositoryTestAccess.replace(List.of());
		try {
			var captured = adapter.updateBootstrap(new JeiViewerAdapter.IngredientSourceSnapshot(List.of(oxygen)), manager);
			adapter.updateBootstrap(new JeiViewerAdapter.IngredientSourceSnapshot(List.of()), manager);
			var icons = adapter.assembleHeaderIcons(List.of(
				new GroupIconDefinition(fixture.type.getUid(), "test:oxygen")), List.of(), captured);
			assertEquals(1, icons.size());
			assertSame(oxygen, icons.get(0));
		} finally {
			adapter.unregisterRuntime();
			GroupRepositoryTestAccess.replace(List.of());
		}
	}

	private static final class Fixture {
		private final IIngredientType<String> type;
		private final AtomicInteger uidCalls = new AtomicInteger();
		private final IIngredientHelper<?> helper;

		Fixture(String uid) {
			type = new IIngredientType<>() {
				public Class<? extends String> getIngredientClass() { return String.class; }
				public String getUid() { return uid; }
			};
			helper = (IIngredientHelper<?>) Proxy.newProxyInstance(IIngredientHelper.class.getClassLoader(),
				new Class<?>[]{IIngredientHelper.class}, (proxy, method, args) -> {
					Object value = args != null && args.length > 0 ? args[0] : null;
					if (value instanceof ITypedIngredient<?> typed) value = typed.getIngredient();
					return switch (method.getName()) {
						case "getUid", "getUniqueId" -> { uidCalls.incrementAndGet(); yield value; }
						case "getIdentifier", "getResourceLocation" -> new ResourceLocation((String) value);
						case "getDisplayName", "getErrorInfo", "copyIngredient" -> value;
						case "getIngredientType" -> type;
						case "getTagStream" -> java.util.stream.Stream.empty();
						default -> throw new AssertionError(method.getName());
					};
				});
		}

		IIngredientManager manager() {
			return (IIngredientManager) Proxy.newProxyInstance(IIngredientManager.class.getClassLoader(),
				new Class<?>[]{IIngredientManager.class}, (proxy, method, args) -> switch (method.getName()) {
					case "getRegisteredIngredientTypes" -> List.of(type);
					case "getIngredientHelper" -> helper;
					default -> throw new AssertionError(method.getName());
				});
		}

		ITypedIngredient<String> typed(String value) {
			return new ITypedIngredient<>() {
				public IIngredientType<String> getType() { return type; }
				public String getIngredient() { return value; }
				public ITypedIngredient<String> normalize(IIngredientHelper<String> ignored) { return this; }
			};
		}
	}
}
