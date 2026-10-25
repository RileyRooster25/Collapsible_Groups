package com.starskyxiii.collapsible_groups.skies;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** Fits a name into a width and a number of lines, wrapping at spaces and ending with "..." when cut. */
public final class TextClip {
	public static final String ELLIPSIS = "...";

	private TextClip() {}

	/** One line: the text, or as much of it as fits followed by "...". */
	public static String line(String text, int maxWidth, ToIntFunction<String> width) {
		List<String> lines = lines(text, maxWidth, 1, width);
		return lines.isEmpty() ? "" : lines.getFirst();
	}

	/** At most {@code maxLines} lines, each no wider than {@code maxWidth}; the last ends with "..." if cut. */
	public static List<String> lines(String text, int maxWidth, int maxLines, ToIntFunction<String> width) {
		List<String> lines = new ArrayList<>();
		String rest = text == null ? "" : text.strip();
		if (rest.isEmpty() || maxWidth <= 0 || maxLines <= 0) return lines;
		while (!rest.isEmpty() && lines.size() < maxLines) {
			boolean last = lines.size() == maxLines - 1;
			if (width.applyAsInt(rest) <= maxWidth) {
				lines.add(rest);
				return lines;
			}
			if (last) {
				lines.add(cut(rest, maxWidth, width));
				return lines;
			}
			int end = longestFit(rest, maxWidth, width);
			int space = rest.lastIndexOf(' ', end);
			int split = space > 0 ? space : Math.max(1, end);
			lines.add(rest.substring(0, split).strip());
			rest = rest.substring(split).strip();
		}
		return lines;
	}

	/** The longest prefix length that fits, at least 0. */
	private static int longestFit(String text, int maxWidth, ToIntFunction<String> width) {
		int end = 0;
		while (end < text.length() && width.applyAsInt(text.substring(0, end + 1)) <= maxWidth) end++;
		return end;
	}

	private static String cut(String text, int maxWidth, ToIntFunction<String> width) {
		int room = maxWidth - width.applyAsInt(ELLIPSIS);
		if (room <= 0) return "";
		int end = longestFit(text, room, width);
		return text.substring(0, end).stripTrailing() + ELLIPSIS;
	}
}
