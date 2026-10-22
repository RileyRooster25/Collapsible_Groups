package com.starskyxiii.collapsible_groups.compat.emi;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmiHeaderItemBatchContractTest {
	private static final String ROOT = "com/starskyxiii/collapsible_groups/compat/emi/";
	private static final String GRAPHICS = "net/minecraft/client/gui/GuiGraphics";

	@Test void preparationUsesOneLiveWorldNullEntityModelAndRejectsSpecialGuiReplacements() throws IOException {
		List<String> arguments = new ArrayList<>(), fields = new ArrayList<>();
		List<List<String>> modelArguments = new ArrayList<>();
		read(ROOT + "EmiHeaderItemBatch", new ClassVisitor(Opcodes.ASM9) {
			@Override public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				assertFalse(descriptor.contains("BakedModel") || descriptor.contains("ItemStack") || descriptor.contains("java/util/Map"),
					"rendered models and stack data must not be retained between header draws");
				return null;
			}
			@Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				if (!name.equals("prepare")) return null;
				return new MethodVisitor(Opcodes.ASM9) {
					@Override public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
						fields.add(owner + "." + name);
						if (owner.equals("net/minecraft/client/Minecraft") && name.equals("level")) arguments.add("level");
					}
					@Override public void visitInsn(int opcode) {
						if (opcode == Opcodes.ACONST_NULL) arguments.add("null");
						if (opcode == Opcodes.ICONST_0) arguments.add("zero");
					}
					@Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean itf) {
						if (owner.equals("net/minecraft/client/renderer/entity/ItemRenderer") && name.equals("getModel")) {
							modelArguments.add(List.copyOf(arguments.subList(arguments.size() - 3, arguments.size())));
						}
					}
				};
			}
		});
		assertEquals(List.of(List.of("level", "null", "zero")), modelArguments);
		assertTrue(fields.contains("net/minecraft/world/item/Items.TRIDENT"));
		assertTrue(fields.contains("net/minecraft/world/item/Items.SPYGLASS"));
	}

	@Test void bothItemsArePreparedBeforeSubmissionAndNativeDecorationsFollowTheBatch() throws IOException {
		List<String> calls = new ArrayList<>();
		List<String> constants = new ArrayList<>();
		read(ROOT + "EmiHeaderItemBatch", new ClassVisitor(Opcodes.ASM9) {
			@Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				if (!name.equals("tryRender")) return null;
				return new MethodVisitor(Opcodes.ASM9) {
					@Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean itf) {
						calls.add(owner + "." + name);
					}
					@Override public void visitLdcInsn(Object value) {
						if (value instanceof Type type) constants.add(type.getInternalName());
					}
				};
			}
		});
		int preparation = calls.indexOf(ROOT + "EmiHeaderItemBatch.prepare");
		int boundary = calls.indexOf(ROOT + "EmiHeaderBatchPolicy$Lighting.<init>");
		int geometry = calls.indexOf(ROOT + "EmiHeaderItemBatch.render");
		int close = calls.indexOf(ROOT + "EmiHeaderBatchPolicy$Lighting.close");
		int decorations = calls.indexOf(GRAPHICS + ".renderItemDecorations");
		assertTrue(preparation >= 0 && preparation < boundary && boundary < geometry && geometry < close && close < decorations);
		assertTrue(calls.contains("java/lang/Object.getClass")
			|| calls.contains("dev/emi/emi/api/stack/EmiIngredient.getClass"),
			"javac may reference Object.getClass through the ingredient interface");
		assertTrue(constants.contains("dev/emi/emi/api/stack/ItemEmiStack"));
	}

	@Test void headerRemainsOutsideNativeBatchableAndKeepsItsMarkerAfterTheItemHelper() throws IOException {
		List<String> calls = new ArrayList<>();
		read(ROOT + "GroupHeaderEmiStack", new ClassVisitor(Opcodes.ASM9) {
			@Override public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				assertEquals("dev/emi/emi/api/stack/EmiStack", superName);
				assertFalse(List.of(interfaces).contains("dev/emi/emi/screen/StackBatcher$Batchable"));
			}
			@Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				if (!name.equals("render")) return null;
				return new MethodVisitor(Opcodes.ASM9) {
					@Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean itf) {
						calls.add(owner + "." + name);
					}
				};
			}
		});
		int helper = calls.indexOf(ROOT + "EmiHeaderItemBatch.tryRender");
		int marker = calls.indexOf(GRAPHICS + ".drawString");
		assertTrue(helper >= 0 && helper < marker);
		assertTrue(calls.contains("dev/emi/emi/api/stack/EmiIngredient.render"),
			"ineligible native previews, fluids and custom ingredients must keep the original render path");
	}

	private static void read(String name, ClassVisitor visitor) throws IOException {
		try (InputStream input = EmiHeaderItemBatchContractTest.class.getClassLoader().getResourceAsStream(name + ".class")) {
			assertNotNull(input, name);
			new ClassReader(input).accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		}
	}
}
