package com.starskyxiii.collapsible_groups.client.skies;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/** The skies.toml written by the first fork version must still load after new settings were added. */
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
	}

	@Test
	void aFreshFileIsCorrectedWithoutError() {
		CommentedConfig config = CommentedConfig.inMemory();
		assertDoesNotThrow(() -> SkiesConfig.SPEC.correct(config));
	}
}
