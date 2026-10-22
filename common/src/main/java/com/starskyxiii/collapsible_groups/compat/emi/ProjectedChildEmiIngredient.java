package com.starskyxiii.collapsible_groups.compat.emi;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.screen.StackBatcher;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.List;
import java.util.Objects;

/** Display-only expanded child. Serialization deliberately unwraps to {@link #delegate}. */
public final class ProjectedChildEmiIngredient implements EmiIngredient, StackBatcher.Batchable {
	private final EmiIngredient delegate;
	private final String parentGroupId;
	private boolean unbatchable;

	public ProjectedChildEmiIngredient(EmiIngredient delegate, String parentGroupId) {
		this.delegate = Objects.requireNonNull(delegate, "delegate");
		this.parentGroupId = Objects.requireNonNull(parentGroupId, "parentGroupId");
	}

	public EmiIngredient delegate() { return delegate; }
	public String parentGroupId() { return parentGroupId; }

	@Override public List<EmiStack> getEmiStacks() { return delegate.getEmiStacks(); }
	@Override public EmiIngredient copy() { return wrap(delegate.copy()); }
	@Override public long getAmount() { return delegate.getAmount(); }
	@Override public EmiIngredient setAmount(long amount) { return wrap(delegate.copy().setAmount(amount)); }
	@Override public float getChance() { return delegate.getChance(); }
	@Override public EmiIngredient setChance(float chance) { return wrap(delegate.copy().setChance(chance)); }
	@Override public List<ClientTooltipComponent> getTooltip() { return delegate.getTooltip(); }
	@Override public boolean isSideLit() {
		return delegate instanceof StackBatcher.Batchable batchable && batchable.isSideLit();
	}
	@Override public boolean isUnbatchable() {
		return unbatchable || !(delegate instanceof StackBatcher.Batchable batchable) || batchable.isUnbatchable();
	}
	@Override public void setUnbatchable() { unbatchable = true; }

	@Override
	public void renderForBatch(MultiBufferSource buffers, GuiGraphics graphics, int x, int y, int z, float delta) {
		if (!(delegate instanceof StackBatcher.Batchable batchable)) {
			throw new IllegalStateException("Projected ingredient does not support EMI batching");
		}
		// EMI owns the version-specific coordinate transform and fallback policy.
		batchable.renderForBatch(buffers, graphics, x, y, z, delta);
	}

	private ProjectedChildEmiIngredient wrap(EmiIngredient value) {
		ProjectedChildEmiIngredient result = new ProjectedChildEmiIngredient(value, parentGroupId);
		result.unbatchable = unbatchable;
		return result;
	}

	@Override
	public void render(GuiGraphics graphics, int x, int y, float delta, int flags) {
		delegate.render(graphics, x, y, delta, flags);
	}
}
