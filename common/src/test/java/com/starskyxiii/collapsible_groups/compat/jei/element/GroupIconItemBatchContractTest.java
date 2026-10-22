package com.starskyxiii.collapsible_groups.compat.jei.element;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroupIconItemBatchContractTest {
	private static final String PREFIX = "com/starskyxiii/collapsible_groups/compat/jei/element/";
	private static final String GRAPHICS = "net/minecraft/client/gui/GuiGraphics";

	@Test
	void modelsUseTheLivePlayerContextAndAreNeverRetainedBetweenDraws() throws IOException {
		List<String> fields = new ArrayList<>();
		List<String> calls = new ArrayList<>();
		read(PREFIX + "GroupIconItemBatch", new ClassVisitor(Opcodes.ASM9) {
			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				fields.add(descriptor);
				return null;
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				if (!"renderItem".equals(name)) return null;
				return new MethodVisitor(Opcodes.ASM9) {
					@Override
					public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
						if (owner.equals("net/minecraft/client/Minecraft")) calls.add(name);
					}

					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
						if (owner.equals("net/minecraft/client/renderer/entity/ItemRenderer")) calls.add(name);
						if (owner.equals("net/minecraft/client/resources/model/BakedModel")) calls.add(name);
					}
				};
			}
		});
		assertTrue(calls.indexOf("level") >= 0 && calls.indexOf("level") < calls.indexOf("getModel"));
		assertTrue(calls.indexOf("player") >= 0 && calls.indexOf("player") < calls.indexOf("getModel"));
		assertTrue(calls.indexOf("getModel") < calls.indexOf("isCustomRenderer"));
		assertTrue(calls.indexOf("isCustomRenderer") < calls.indexOf("render"));
		assertEquals(1, calls.stream().filter("getModel"::equals).count());
		assertFalse(fields.stream().anyMatch(field -> field.contains("BakedModel")
			|| field.contains("ItemStack") || field.contains("java/util/")),
			"the batch must retain no model, stack, or cross-frame collection cache");
	}

	@Test
	void transformCapabilityIsCheckedBeforeBatchSubmissionAndFallbackKeepsItsBoundary() throws IOException {
		List<String> itemCalls = new ArrayList<>();
		read(PREFIX + "GroupIconItemBatch", new ClassVisitor(Opcodes.ASM9) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				if (!"renderItem".equals(name)) return null;
				return new MethodVisitor(Opcodes.ASM9) {
					@Override
					public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
						if (owner.equals("net/minecraft/world/item/Items")) itemCalls.add(owner + "." + name);
					}

					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
						itemCalls.add(owner + "." + name);
					}
				};
			}
		});
		int eligibility = itemCalls.indexOf(PREFIX + "GroupIconModelTransformSupport.canBatch");
		int prepare = itemCalls.indexOf(PREFIX + "GroupIconBatchLighting.prepare");
		assertTrue(eligibility >= 0 && eligibility < prepare,
			"custom transform models must be rejected before adding anything to the batch");
		for (String item : List.of("TRIDENT", "SPYGLASS")) {
			int replacement = itemCalls.indexOf("net/minecraft/world/item/Items." + item);
			assertTrue(replacement >= 0 && replacement < eligibility,
				"items whose GUI model is replaced must stay on the vanilla path");
		}

		List<String> layerCalls = new ArrayList<>();
		read(PREFIX + "GroupIconRenderer", new ClassVisitor(Opcodes.ASM9) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				if (!"renderIngredient".equals(name)) return null;
				return new MethodVisitor(Opcodes.ASM9) {
					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
						layerCalls.add(owner + "." + name);
					}
				};
			}
		});
		int batch = layerCalls.indexOf(PREFIX + "GroupIconItemBatch.renderItem");
		int barrier = layerCalls.indexOf(PREFIX + "GroupIconItemBatch.beforeFallback");
		int vanilla = layerCalls.indexOf(GRAPHICS + ".renderItem");
		assertTrue(batch >= 0 && batch < barrier && barrier < vanilla,
			"an unsupported item must flush and restore lighting before vanilla resolves its transform");
		assertFalse(itemCalls.stream().anyMatch(call -> call.endsWith(".applyTransform")),
			"the capability check must not apply a transform before vanilla applies it");
	}

	@Test
	void indicatorsStayBufferedAndTheGridOverridesTheJeiBatchEntry() throws IOException {
		List<String> indicatorCalls = new ArrayList<>();
		read(PREFIX + "GroupIconItemBatch", new ClassVisitor(Opcodes.ASM9) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				if (!"renderIndicator".equals(name)) return null;
				return new MethodVisitor(Opcodes.ASM9) {
					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
						indicatorCalls.add(owner + "." + name);
					}
				};
			}
		});
		assertTrue(indicatorCalls.contains("net/minecraft/client/gui/Font.drawInBatch"));
		assertFalse(indicatorCalls.contains(GRAPHICS + ".drawString"));
		assertFalse(indicatorCalls.contains(GRAPHICS + ".flush"));

		List<String> batchCalls = new ArrayList<>();
		read(PREFIX + "GroupIconRenderer", new ClassVisitor(Opcodes.ASM9) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				if (!"renderBatch".equals(name)) return null;
				assertEquals("(L" + GRAPHICS + ";Ljava/util/List;)V", descriptor);
				return new MethodVisitor(Opcodes.ASM9) {
					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
						batchCalls.add(owner + "." + name);
					}
				};
			}
		});
		assertTrue(batchCalls.contains(PREFIX + "GroupIconItemBatch.<init>"));
		assertTrue(batchCalls.contains(PREFIX + "GroupIconItemBatch.close"),
			"the batch must submit pending geometry before returning to JEI");
	}

	private static void read(String name, ClassVisitor visitor) throws IOException {
		try (InputStream stream = GroupIconItemBatchContractTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
			assertNotNull(stream, name);
			new ClassReader(stream).accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		}
	}
}
