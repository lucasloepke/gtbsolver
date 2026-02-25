package dev.lucasloepke.gtbsolver;

import java.util.Objects;

/**
 * Represents a hint pattern like "___ __" where '_' are unknown chars and spaces are significant.
 */
public final class HintPattern {
	private final String pattern;

	private HintPattern(String pattern) {
		this.pattern = pattern;
	}

	public static HintPattern of(String pattern) {
		String normalized = normalizePattern(pattern);
		if (normalized == null) return null;
		return new HintPattern(normalized);
	}

	public static HintPattern fromKey(String key) {
		if (key == null || key.isBlank()) return null;
		return HintPattern.of(key);
	}

	public String toKey() {
		return pattern;
	}

	public String pretty() {
		// render as spaced letters to look like Hypixel hint format
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < pattern.length(); i++) {
			char c = pattern.charAt(i);
			if (c == ' ') {
				out.append("   ");
			} else {
				if (out.length() > 0) out.append(' ');
				out.append(c == '_' ? '_' : Character.toUpperCase(c));
			}
		}
		return out.toString();
	}

	public boolean matchesCandidate(String candidate) {
		if (candidate == null) return false;
		String c = normalizeCandidate(candidate);
		if (c.length() != pattern.length()) return false;

		for (int i = 0; i < pattern.length(); i++) {
			char p = pattern.charAt(i);
			char ch = c.charAt(i);

			if (p == ' ') {
				if (ch != ' ') return false;
				continue;
			}

			if (p == '_') continue;
			if (Character.toLowerCase(p) != Character.toLowerCase(ch)) return false;
		}
		return true;
	}

	public static String normalizeCandidate(String candidate) {
		String s = candidate.trim().replaceAll("\\s+", " ");
		return s;
	}

	private static String normalizePattern(String raw) {
		if (raw == null) return null;
		String s = raw.trim().replaceAll("\\s+", " ");
		if (s.isEmpty()) return null;

		// Must contain at least one unknown underscore, otherwise we might be matching random actionbar text
		if (!s.contains("_")) return null;

		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == ' ') continue;
			if (c == '_' || Character.isLetterOrDigit(c) || c == '\'' || c == '-' || c == '&') continue;
			return null;
		}

		// Heuristic: occasionally a stray lowercase 'e' from Minecraft color code "§e"
		// ends up as a leading character before the real pattern, e.g. "e__g_ h__l_".
		// If we see exactly this shape (leading 'e' followed by an underscore) we
		// strip that 'e' so the pattern matches the visual hint "__g_ h__l_".
		if (s.length() >= 2 && s.charAt(0) == 'e' && s.charAt(1) == '_' && s.contains("_")) {
			s = s.substring(1);
		}

		return s;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof HintPattern that)) return false;
		return Objects.equals(pattern, that.pattern);
	}

	@Override
	public int hashCode() {
		return Objects.hash(pattern);
	}
}
