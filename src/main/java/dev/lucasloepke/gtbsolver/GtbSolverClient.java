package dev.lucasloepke.gtbsolver;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GtbSolverClient implements ClientModInitializer {
	private static final AtomicBoolean WORDLIST_LOADED = new AtomicBoolean(false);
	private static volatile String lastHintKey = null;
	private static volatile boolean autoOutputEnabled = true;
	private static volatile String lastOverlayRaw = null;

	@Override
	public void onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(literal("gtbs")
				.then(literal("guess")
					.then(argument("word", greedyString())
						.executes(ctx -> {
							MinecraftClient client = MinecraftClient.getInstance();
							if (client.player == null) return 0;
							String word = getString(ctx, "word").toLowerCase();
							client.player.networkHandler.sendChatMessage(word);
							return 1;
						})
					)
				)
				.then(literal("hints")
					.executes(ctx -> {
						MinecraftClient client = MinecraftClient.getInstance();
						if (lastHintKey == null) {
							if (client != null && client.inGameHud != null) {
								client.inGameHud.getChatHud().addMessage(
									Text.literal("[GTBS] No theme hint detected yet.").formatted(Formatting.WHITE)
								);
							}
							return 1;
						}
						printCandidatesToChat(client, lastHintKey, false);
						return 1;
					})
				)
				.then(literal("debug")
					.executes(ctx -> {
						MinecraftClient client = MinecraftClient.getInstance();
						if (client != null && client.inGameHud != null) {
							String raw = lastOverlayRaw;
							if (raw == null || raw.isBlank()) {
								client.inGameHud.getChatHud().addMessage(
									Text.literal("[GTBS] No overlay captured yet.").formatted(Formatting.WHITE)
								);
							} else {
								client.inGameHud.getChatHud().addMessage(
									Text.literal("[GTBS] overlay: ").formatted(Formatting.WHITE)
										.append(Text.literal(raw).formatted(Formatting.WHITE))
								);
							}
						}
						return 1;
					})
				)
				.then(literal("toggle")
					.executes(ctx -> {
						autoOutputEnabled = !autoOutputEnabled;
						sendClientMessage(ctx.getSource(), Text.literal("Auto output: ")
							.append(Text.literal(autoOutputEnabled ? "ON" : "OFF").formatted(Formatting.WHITE)));
						return 1;
					})
				)
			);
		});
	}

	private static void sendClientMessage(FabricClientCommandSource source, Text msg) {
		source.sendFeedback(Text.literal("[GTBS] ").formatted(Formatting.WHITE).append(msg));
	}

	/**
	 * Called by mixin when the action bar (overlay) message changes.
	 */
	public static void onActionBarMessage(Text message) {
		MinecraftClient client = MinecraftClient.getInstance();

		if (!WORDLIST_LOADED.get()) {
			Wordlist.ensureLoaded();
			WORDLIST_LOADED.set(true);
		}

		// Cache latest overlay text for on-demand debugging.
		if (message != null) {
			lastOverlayRaw = message.getString();
		}

		HintPattern pattern = HintParser.tryParse(message);
		if (pattern == null) return;

		String key = pattern.toKey();
		if (key.equals(lastHintKey)) return;
		lastHintKey = key;

		if (!autoOutputEnabled) return;
		printCandidatesToChat(client, key, true);
	}

	private static void printCandidatesToChat(MinecraftClient client, String hintKey, boolean allowAutoGuess) {
		if (client == null || client.inGameHud == null || hintKey == null) return;

		HintPattern pattern = HintPattern.fromKey(hintKey);
		if (pattern == null) return;

		List<String> matches = Wordlist.findMatches(pattern, Integer.MAX_VALUE);

		int wordLength = pattern.toKey().length();
		if (matches.isEmpty()) {
			client.inGameHud.getChatHud().addMessage(
				Text.literal("[GTBS] ").formatted(Formatting.WHITE)
					.append(Text.literal(pattern.pretty() + "  length: " + wordLength + "  No matches.").formatted(Formatting.WHITE))
			);
			return;
		}

		// Single match: auto-guess and notify (only when allowAutoGuess)
		if (allowAutoGuess && matches.size() == 1 && client.player != null) {
			String word = matches.get(0).toLowerCase();
			client.player.networkHandler.sendChatMessage(word);
			client.inGameHud.getChatHud().addMessage(
				Text.literal("[GTBS] Auto-guessed: ").formatted(Formatting.WHITE)
					.append(Text.literal(word).formatted(Formatting.GREEN))
			);
			return;
		}

		client.inGameHud.getChatHud().addMessage(
			Text.literal("[GTBS] ").formatted(Formatting.WHITE)
				.append(Text.literal(pattern.pretty() + "  length: " + wordLength).formatted(Formatting.WHITE))
		);

		MutableText line = Text.empty();
		for (String word : matches) {
			MutableText clickable = Text.literal(word)
				.setStyle(Style.EMPTY
					.withColor(Formatting.GREEN)
					.withClickEvent(new ClickEvent.RunCommand("/gtbs guess " + word.toLowerCase()))
					.withHoverEvent(new HoverEvent.ShowText(
						Text.literal("[GTBS] Click to guess: ").formatted(Formatting.WHITE)
							.append(Text.literal(word).formatted(Formatting.GREEN))))
				);

			if (!line.getString().isEmpty()) {
				line = line.append(Text.literal("   ").formatted(Formatting.WHITE));
			}
			line = line.append(clickable);
		}

		client.inGameHud.getChatHud().addMessage(line);
	}
}
