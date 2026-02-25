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

		// Strip Minecraft-style formatting/color codes (e.g. "§e", "§l", hex codes starting with "§x").
		// This prevents leftover 'e' / 'E' from color codes from ever entering our pattern logic.
		plain = plain
			// legacy codes like §e, §l etc.
			.replaceAll("§.", "")
			// just in case some servers literally use &e-style codes in the action bar
			.replaceAll("&.", "");

		if (!plain.contains("_")) return null;

		// 1) Exact Hypixel GTB format: "The theme is __p__o" or "The theme is ___ ___"
		String lower = plain.toLowerCase(Locale.ROOT);
		int themeIdx = lower.indexOf(THEME_PREFIX);
		if (themeIdx >= 0) {
			int start = themeIdx + THEME_PREFIX.length();
			if (start < plain.length()) {
				String themePart = plain.substring(start);
				String cleaned = normalizeThemeHint(themePart);
				if (cleaned != null) {
					return HintPattern.of(cleaned);
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

	/**
	 * Normalize the part after "The theme is " into a stable pattern string
	 * that HintPattern can accept.
	 */
	private static String normalizeThemeHint(String themePart) {
		if (themePart == null) return null;

		// Strip formatting / punctuation except spaces, underscores, and basic word chars.
		String cleaned = themePart
			.replaceAll("[^A-Za-z0-9_\\s\\-'&]", " ")
			.trim()
			.replaceAll("\\s+", " ");

		if (cleaned.isEmpty() || !cleaned.contains("_")) return null;

		// If there are any stray leading letters etc, re-run our region extractor
		// on just this substring to keep only the core pattern with underscores.
		Matcher m = HINT_REGION.matcher(cleaned);
		String best = null;
		while (m.find()) {
			String candidate = m.group().trim();
			if (!candidate.contains("_")) continue;
			if (best == null || candidate.length() > best.length()) best = candidate;
		}
		if (best != null) cleaned = best;

		// Normalize away any leftover color-code artifacts (e.g. a stray leading 'e'
		// from '§e' that sometimes sneaks in before the real pattern).
		cleaned = stripLeadingColorE(cleaned);

		return cleaned;
	}

	private static boolean isHintChar(char c) {
		return c == ' ' || c == '_' || Character.isLetterOrDigit(c) || c == '\'' || c == '-' || c == '&';
	}

	/**
	 * Some Hypixel themes are color-coded like "§e__g_ H__l_". After we strip non-word
	 * characters, this can leave a bogus leading 'e' / 'E' in front of the real pattern
	 * (e.g. "e__g_ H__l_"), which then shows up as an extra "E" in our pretty-printed
	 * hint. This helper trims that artifact for both the Hypixel-specific and generic
	 * parsing paths.
	 */
	private static String stripLeadingColorE(String raw) {
		if (raw == null) return null;
		String s = raw.trim();
		if (s.length() < 2 || !s.contains("_")) return raw;

		char first = s.charAt(0);
		if (first != 'e' && first != 'E') return raw;

		// Look for the first non-space character after the leading 'e'/'E'.
		int i = 1;
		while (i < s.length() && s.charAt(i) == ' ') {
			i++;
		}

		// If the next significant character is an underscore, this 'e' is almost certainly
		// the tail of a color code like '§e', not part of the actual hint. Drop it.
		if (i < s.length() && s.charAt(i) == '_') {
			return s.substring(1).trim();
		}

		return raw;
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

		String pattern = String.join(" ", wordPatterns);

		// Apply the same color-code cleanup heuristic used for Hypixel theme parsing,
		// in case we hit this generic path on a server that uses '§e' style colors.
		return stripLeadingColorE(pattern);
	}
}

