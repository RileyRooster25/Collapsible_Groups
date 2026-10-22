package com.starskyxiii.collapsible_groups.compat.emi;

import dev.emi.emi.api.stack.EmiIngredient;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Conservative visual-no-op checks and submission boundaries for a single header draw. */
final class EmiHeaderBatchPolicy {
	record VisualState(boolean empty, boolean air, boolean foil, boolean bar,
		boolean cooldown, boolean emptyRemainder) {}
	record Layer(int index, int x, int y, int z) {}
	static final List<Layer> LAYERS = List.of(new Layer(1, 2, 0, 0), new Layer(0, 0, 2, 10));

	private EmiHeaderBatchPolicy() {}

	static boolean allows(int flags, VisualState state) {
		return (flags & EmiIngredient.RENDER_ICON) != 0
			&& (flags & EmiIngredient.RENDER_AMOUNT) == 0
			&& !state.empty() && !state.air() && !state.foil() && !state.bar() && !state.cooldown()
			&& ((flags & EmiIngredient.RENDER_REMAINDER) == 0 || state.emptyRemainder());
	}

	static boolean nativeTransform(Class<?> type, Set<String> defaults, Class<?> context, Class<?> pose) {
		try {
			Method transform = type.getMethod("applyTransform", context, pose, boolean.class);
			return transform.isDefault() && defaults.contains(transform.getDeclaringClass().getName());
		} catch (NoSuchMethodException ignored) {
			return true;
		} catch (SecurityException | LinkageError ignored) {
			return false;
		}
	}

	/** The public factory is inspected once; its handler results are compared on every draw. */
	static final class Decorations {
		private final Method factory;

		private Decorations(Method factory) { this.factory = factory; }

		static Decorations inspect(Class<?> api, Class<?> stackType) {
			try {
				Method factory = api.getMethod("of", stackType);
				if (!Modifier.isStatic(factory.getModifiers()) || factory.getReturnType() != api) {
					return new Decorations(null);
				}
				return new Decorations(factory);
			} catch (ReflectiveOperationException | SecurityException | LinkageError ignored) {
				return new Decorations(null);
			}
		}

		static Decorations unknown() { return new Decorations(null); }

		boolean empty(Object stack, Object emptyStack) {
			if (factory == null) return false;
			try {
				Object handler = factory.invoke(null, stack);
				Object empty = factory.invoke(null, emptyStack);
				return handler != null && handler == empty;
			} catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
				return false;
			}
		}
	}

	static final class Lighting implements AutoCloseable {
		private final Runnable flush;
		private final Consumer<Boolean> setFlat;
		private Boolean flat;

		Lighting(Runnable flush, Consumer<Boolean> setFlat) {
			this.flush = flush;
			this.setFlat = setFlat;
		}

		void prepare(boolean nextFlat) {
			if (flat == null || flat != nextFlat) {
				flush.run();
				setFlat.accept(nextFlat);
				flat = nextFlat;
			}
		}

		@Override
		public void close() {
			try {
				flush.run();
			} finally {
				setFlat.accept(false);
			}
		}
	}
}
