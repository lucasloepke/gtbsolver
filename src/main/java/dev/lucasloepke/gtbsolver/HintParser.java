package dev.lucasloepke.gtbsolver;

import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HintParser {
	private HintParser() {}

	// Hypixel-specific prefix used in Guess The Build
	private static final String THEME_PREFIX = "the theme is ";

	// Tries to find a "letter-by-letter" hint region containing underscores/letters/spaces
	private static final Pattern HINT_REGION = Pattern.compile("([A-Za-z0-9_\\-\\'&]\\s+){2,}[A-Za-z0-9_\\-\\'&]|[_A-Za-z0-9\\-\\'& ]{3,}");

	public static HintPattern tryParse(Text actionBarText) {
		if (actionBarText == null) return null;
		String plain = actionBarText.getString();
		if (plain == null || plain.isBlank()) return null;
		if (!plain.contains("_")) return null;

		// 1) Exact Hypixel GTB format: "The theme is __p__o" or "The theme is ___ ___"
		String lower = plain.toLowerCase(Locale.ROOT);
		int themeIdx = lower.indexOf(THEME_PREFIX);
		if (themeIdx >= 0) {
			int start = themeIdx + THEME_PREFIX.length();
			if (start < plain.length()) {
				String themePart = plain.substring(start).trim();
				if (!themePart.isEmpty() && themePart.contains("_")) {
					return HintPattern.of(themePart);
				}
			}
		}

		// 2) Fallback: generic region extraction (for other servers / formats)
		String region = extractBestRegion(plain);
		if (region == null) return null;

		String pattern = parseRegionToPattern(region);
		return HintPattern.of(pattern);
	}

	private static String extractBestRegion(String plain) {
		Matcher m = HINT_REGION.matcher(plain);
		String best = null;
		while (m.find()) {
			String candidate = m.group().trim();
			if (!candidate.contains("_")) continue;
			if (best == null || candidate.length() > best.length()) best = candidate;
		}
		if (best != null) return best;

		// Fallback: expand around first underscore
		int idx = plain.indexOf('_');
		if (idx < 0) return null;
		int left = idx;
		while (left > 0) {
			char c = plain.charAt(left - 1);
			if (isHintChar(c)) left--;
			else break;
		}
		int right = idx;
		while (right < plain.length() - 1) {
			char c = plain.charAt(right + 1);
			if (isHintChar(c)) right++;
			else break;
		}
		String s = plain.substring(left, right + 1).trim();
		return s.contains("_") ? s : null;
	}

	private static boolean isHintChar(char c) {
		return c == ' ' || c == '_' || Character.isLetterOrDigit(c) || c == '\'' || c == '-' || c == '&';
	}

	/**
	 * Turns an actionbar region into a compact pattern string like "___ __".
	 * - 2+ spaces indicate word breaks
	 * - 1+ spaces indicate letter separators
	 */
	private static String parseRegionToPattern(String region) {
		String trimmed = region.trim();

		// Split words on 2+ spaces
		String[] words = trimmed.split(" {2,}");
		List<String> wordPatterns = new ArrayList<>();

		for (String w : words) {
			String word = w.trim();
			if (word.isEmpty()) continue;
			String[] tokens = word.split("\\s+");

			StringBuilder wp = new StringBuilder();
			for (String t : tokens) {
				if (t.isEmpty()) continue;
				char ch = t.charAt(0);
				if (ch == '_') {
					wp.append('_');
				} else if (Character.isLetterOrDigit(ch) || ch == '\'' || ch == '-' || ch == '&') {
					wp.append(ch);
				} else {
					// ignore unknown token chars
				}
			}

			if (wp.length() > 0) wordPatterns.add(wp.toString());
		}

		return String.join(" ", wordPatterns);
	}
}

