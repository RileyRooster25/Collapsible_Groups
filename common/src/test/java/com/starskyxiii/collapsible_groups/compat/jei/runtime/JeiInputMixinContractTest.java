package com.starskyxiii.collapsible_groups.compat.jei.runtime;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class JeiInputMixinContractTest {
	@ParameterizedTest
	@ValueSource(strings = {"fabric", "neoforge"})
	void bookmarkHookUsesCurrentDescriptorAndRemovedInputMixinsStayUnregistered(String loader) throws Exception {
		Path root = Path.of(System.getProperty("collapsibleGroupsRoot"), loader);
		String source = Files.readString(root.resolve("src/main/java/com/starskyxiii/collapsible_groups/mixin/MixinBookmarkList.java"));
		assertTrue(source.contains("onElementBookmarked(Lmezz/jei/gui/overlay/elements/IElement;Lmezz/jei/common/input/UserInput;Lmezz/jei/gui/overlay/bookmarks/BookmarkOverlay;)Z"));
		assertTrue(source.contains("require = 1"));
		assertTrue(source.contains("allow = 1"));
		String config = Files.readString(root.resolve("src/main/resources/collapsible_groups." + loader + ".mixins.json"));
		assertTrue(config.contains("MixinBookmarkList"));
		assertFalse(config.contains("MixinJeiElement"));
		assertFalse(config.contains("MixinGuiTextFieldFilterAccessor"));
	}
}
