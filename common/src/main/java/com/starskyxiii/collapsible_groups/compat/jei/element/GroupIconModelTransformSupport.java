package com.starskyxiii.collapsible_groups.compat.jei.element;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;

import java.lang.reflect.Method;
import java.util.Set;

/** Only caches a class's transform capability, never a model or its rendered appearance. */
final class GroupIconModelTransformSupport {
	private static final Set<String> NATIVE_DEFAULTS = Set.of(
		"net.minecraftforge.client.extensions.IForgeBakedModel",
		"net.neoforged.neoforge.client.extensions.IBakedModelExtension"
	);
	private static final ClassValue<Boolean> SAFE_TRANSFORMS = new ClassValue<>() {
		@Override
		protected Boolean computeValue(Class<?> type) {
			return hasSafeTransform(type, NATIVE_DEFAULTS, ItemDisplayContext.class, PoseStack.class);
		}
	};

	private GroupIconModelTransformSupport() {}

	static boolean canBatch(BakedModel model) {
		return SAFE_TRANSFORMS.get(model.getClass());
	}

	static boolean hasSafeTransform(Class<?> type, Set<String> nativeDefaults, Class<?> context, Class<?> pose) {
		try {
			Method transform = type.getMethod("applyTransform", context, pose, boolean.class);
			// Platform defaults apply the normal GUI transform and return this model.
			// An override can return a different, custom renderer model; let vanilla call it once.
			return transform.isDefault() && nativeDefaults.contains(transform.getDeclaringClass().getName());
		} catch (NoSuchMethodException ignored) {
			// Fabric's vanilla BakedModel does not have the Forge/NeoForge extension.
			return true;
		} catch (SecurityException | LinkageError ignored) {
			return false;
		}
	}
}
