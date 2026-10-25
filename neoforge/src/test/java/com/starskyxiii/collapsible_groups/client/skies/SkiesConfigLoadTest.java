package com.starskyxiii.collapsible_groups.client.skies;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** The skies.toml written by earlier fork versions, and a missing one, must load without error. */
class SkiesConfigLoadTest {
	private static final String FIRST_VERSION_FILE = String.join("\n",
		"skiesDisplay = true",
		"writeStats = true",
		"panelWidth = 128",
		"");

	@Test
	void anOlderFileIsCorrectedWithoutError() {
		CommentedConfig config = new TomlParser().parse(FIRST_VERSION_FILE);
		assertDoesNotThrow(() -> SkiesConfig.SPEC.correct(config));
		assertEquals(128, config.<Integer>get("panelWidth"));
	}

	@Test
	void aFreshFileIsCorrectedWithoutError() {
		CommentedConfig config = CommentedConfig.inMemory();
		assertDoesNotThrow(() -> SkiesConfig.SPEC.correct(config));
		assertEquals(140, config.<Integer>get("panelWidth"));
		assertEquals(true, config.<Boolean>get("skiesDisplay"));
	}

	@Test
	void aWidthUnderTheMinimumIsCorrectedToIt() {
		CommentedConfig config = new TomlParser().parse("panelWidth = 80\n");
		assertDoesNotThrow(() -> SkiesConfig.SPEC.correct(config));
		assertEquals(100, config.<Integer>get("panelWidth"));
	}
}
