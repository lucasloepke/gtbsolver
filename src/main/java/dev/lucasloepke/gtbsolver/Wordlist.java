package dev.lucasloepke.gtbsolver;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Wordlist {
	private Wordlist() {}

	private static final String WORDLIST_RESOURCE = "/assets/gtbsolver/wordlist.js";
	private static final Pattern QUOTED = Pattern.compile("\"([^\"]+)\"");

	private static volatile List<String> WORDS = List.of();

	public static void ensureLoaded() {
		if (!WORDS.isEmpty()) return;
		WORDS = Collections.unmodifiableList(loadWords());
	}

	public static List<String> findMatches(HintPattern pattern, int limit) {
		ensureLoaded();
		if (pattern == null) return List.of();

		List<String> out = new ArrayList<>(Math.min(limit, 64));
		for (String w : WORDS) {
			if (pattern.matchesCandidate(w)) {
				out.add(w);
				if (out.size() >= limit) break;
			}
		}
		return out;
	}

	private static List<String> loadWords() {
		InputStream is = Wordlist.class.getResourceAsStream(WORDLIST_RESOURCE);
		if (is == null) {
			return List.of();
		}

		Set<String> unique = new LinkedHashSet<>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				Matcher m = QUOTED.matcher(line);
				while (m.find()) {
					String word = HintPattern.normalizeCandidate(m.group(1));
					if (!word.isEmpty()) unique.add(word);
				}
			}
		} catch (Exception e) {
			return List.of();
		}

		return new ArrayList<>(unique);
	}
}

