package com.starskyxiii.collapsible_groups.compat.emi;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.screen.StackBatcher;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectedChildEmiIngredientTest {
	@Test void preservesZeroAmountCopyChanceTooltipAndStackList() {
		List<EmiStack> stacks = List.of();
		List<ClientTooltipComponent> tooltip = List.of();
		FakeIngredient delegate = new FakeIngredient(0, 0.25f, stacks, tooltip);
		ProjectedChildEmiIngredient child = new ProjectedChildEmiIngredient(delegate, "group");

		assertEquals(0, child.getAmount());
		assertEquals(0.25f, child.getChance());
		assertSame(stacks, child.getEmiStacks());
		assertSame(tooltip, child.getTooltip());
		ProjectedChildEmiIngredient copy = assertInstanceOf(ProjectedChildEmiIngredient.class, child.copy());
		assertEquals("group", copy.parentGroupId());
		assertNotSame(delegate, copy.delegate());
	}

	@Test void amountAndChanceUpdatesRemainWrappedAndDoNotMutateDelegate() {
		FakeIngredient delegate = new FakeIngredient(7, 0.5f, List.of(), List.of());
		ProjectedChildEmiIngredient child = new ProjectedChildEmiIngredient(delegate, "fluid-group");

		ProjectedChildEmiIngredient amount = assertInstanceOf(ProjectedChildEmiIngredient.class, child.setAmount(81));
		ProjectedChildEmiIngredient chance = assertInstanceOf(ProjectedChildEmiIngredient.class, child.setChance(0.75f));
		assertEquals(81, amount.getAmount());
		assertEquals(0.75f, chance.getChance());
		assertEquals(7, child.getAmount());
		assertEquals(0.5f, child.getChance());
		assertEquals("fluid-group", amount.parentGroupId());
		assertEquals("fluid-group", chance.parentGroupId());
	}

	@Test void batchingPreservesDelegatePolicyAndAllRenderArguments() {
		BatchableIngredient delegate = new BatchableIngredient();
		ProjectedChildEmiIngredient child = new ProjectedChildEmiIngredient(delegate, "group");
		assertFalse(child.isUnbatchable());
		assertTrue(child.isSideLit());
		MultiBufferSource buffers = ignored -> null;
		child.renderForBatch(buffers, null, 13, -27, 9, 0.625f);
		assertSame(buffers, delegate.buffers);
		assertNull(delegate.graphics);
		assertEquals(13, delegate.x);
		assertEquals(-27, delegate.y);
		assertEquals(9, delegate.z);
		assertEquals(0.625f, delegate.delta);
		assertEquals(1, delegate.batchCalls);

		delegate.sideLit = false;
		delegate.unbatchable = true;
		assertFalse(child.isSideLit());
		assertTrue(child.isUnbatchable());
	}

	@Test void batchFailureStaysLocalAndFallbackStillDelegatesFlags() {
		BatchableIngredient delegate = new BatchableIngredient();
		ProjectedChildEmiIngredient child = new ProjectedChildEmiIngredient(delegate, "group");
		delegate.failure = new IllegalStateException("renderer failed");
		assertSame(delegate.failure, assertThrows(IllegalStateException.class,
			() -> child.renderForBatch(ignored -> null, null, 1, 2, 3, 0)));
		child.setUnbatchable();
		assertTrue(child.isUnbatchable());
		assertFalse(delegate.isUnbatchable());
		assertTrue(((ProjectedChildEmiIngredient) child.copy()).isUnbatchable());
		assertTrue(((ProjectedChildEmiIngredient) child.setAmount(5)).isUnbatchable());
		assertTrue(((ProjectedChildEmiIngredient) child.setChance(0.5f)).isUnbatchable());
		child.render(null, 3, 7, 0.25f, EmiIngredient.RENDER_AMOUNT);
		assertEquals(3, delegate.renderX);
		assertEquals(7, delegate.renderY);
		assertEquals(0.25f, delegate.renderDelta);
		assertEquals(EmiIngredient.RENDER_AMOUNT, delegate.renderFlags);
	}

	@Test void nonBatchableIngredientsRemainOnOrdinaryRenderPath() {
		FakeIngredient delegate = new FakeIngredient(1, 1, List.of(), List.of());
		ProjectedChildEmiIngredient child = new ProjectedChildEmiIngredient(delegate, "fluid-group");
		assertTrue(child.isUnbatchable());
		assertFalse(child.isSideLit());
		child.render(null, 17, 23, 0.5f, EmiIngredient.RENDER_ICON);
		assertEquals(17, delegate.renderX);
		assertEquals(23, delegate.renderY);
		assertEquals(0.5f, delegate.renderDelta);
		assertEquals(EmiIngredient.RENDER_ICON, delegate.renderFlags);
	}

	private static final class BatchableIngredient extends FakeIngredient implements StackBatcher.Batchable {
		private boolean sideLit = true;
		private boolean unbatchable;
		private MultiBufferSource buffers;
		private GuiGraphics graphics;
		private int x, y, z, batchCalls;
		private float delta;
		private RuntimeException failure;

		private BatchableIngredient() { super(1, 1, List.of(), List.of()); }
		@Override public EmiIngredient copy() { return new BatchableIngredient(); }
		@Override public boolean isSideLit() { return sideLit; }
		@Override public boolean isUnbatchable() { return unbatchable; }
		@Override public void setUnbatchable() { unbatchable = true; }
		@Override public void renderForBatch(MultiBufferSource buffers, GuiGraphics graphics,
			int x, int y, int z, float delta) {
			if (failure != null) throw failure;
			this.buffers = buffers;
			this.graphics = graphics;
			this.x = x;
			this.y = y;
			this.z = z;
			this.delta = delta;
			batchCalls++;
		}
	}

	private static class FakeIngredient implements EmiIngredient {
		private final long amount;
		private final float chance;
		private final List<EmiStack> stacks;
		private final List<ClientTooltipComponent> tooltip;
		int renderX, renderY, renderFlags;
		float renderDelta;

		private FakeIngredient(long amount, float chance, List<EmiStack> stacks,
			List<ClientTooltipComponent> tooltip) {
			this.amount = amount;
			this.chance = chance;
			this.stacks = stacks;
			this.tooltip = tooltip;
		}

		@Override public List<EmiStack> getEmiStacks() { return stacks; }
		@Override public EmiIngredient copy() { return new FakeIngredient(amount, chance, stacks, tooltip); }
		@Override public long getAmount() { return amount; }
		@Override public EmiIngredient setAmount(long value) { return new FakeIngredient(value, chance, stacks, tooltip); }
		@Override public float getChance() { return chance; }
		@Override public EmiIngredient setChance(float value) { return new FakeIngredient(amount, value, stacks, tooltip); }
		@Override public List<ClientTooltipComponent> getTooltip() { return tooltip; }
		@Override public void render(GuiGraphics graphics, int x, int y, float delta, int flags) {
			renderX = x;
			renderY = y;
			renderDelta = delta;
			renderFlags = flags;
		}
	}
}
