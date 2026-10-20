package com.starskyxiii.collapsible_groups.compat.jei.runtime;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeiOverlayInputAbiTest {
    @ParameterizedTest
    @ValueSource(strings = {"fabric", "forge", "neoforge"})
    void packagedRequiredInputHookResolvesExactlyOnceInJei(String loader) throws IOException {
        ClassNode mixin = read(System.getProperty("jeiAbi." + loader + ".mod"),
            "com/starskyxiii/collapsible_groups/mixin/MixinIngredientListOverlay");
        AnnotationNode hook = mixin.methods.stream()
            .filter(method -> method.name.equals("cg$wrapInputHandler"))
            .flatMap(method -> method.visibleAnnotations.stream())
            .filter(annotation -> annotation.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;"))
            .findFirst().orElseThrow();
        assertEquals(1, value(hook, "require"));
        assertEquals(1, value(hook, "allow"));
        List<?> selectors = (List<?>) value(hook, "method");
        ClassNode overlay = read(System.getProperty("jeiAbi." + loader + ".jei"),
            "mezz/jei/gui/overlay/IngredientListOverlay");
        assertEquals(1, overlay.methods.stream()
            .filter(method -> selectors.contains(method.name + method.desc)).count(),
            "The required input hook must resolve against the actual " + loader + " JEI artifact");
    }

    private static ClassNode read(String path, String name) throws IOException {
        assertNotNull(path);
        try (ZipFile jar = new ZipFile(path)) {
            var entry = jar.getEntry(name + ".class");
            assertNotNull(entry, path + ": " + name);
            try (var stream = jar.getInputStream(entry)) {
                ClassNode node = new ClassNode();
                new ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                return node;
            }
        }
    }

    private static Object value(AnnotationNode annotation, String key) {
        int index = annotation.values.indexOf(key);
        assertTrue(index >= 0, key);
        return annotation.values.get(index + 1);
    }
}
