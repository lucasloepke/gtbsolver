package dev.lucasloepke.gtbsolver;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;
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
							Minecraft client = Minecraft.getInstance();
							if (client.player == null) return 0;
							String word = getString(ctx, "word").toLowerCase();
							client.player.connection.sendChat(word);
							return 1;
						})
					)
				)
				.then(literal("hints")
					.executes(ctx -> {
						Minecraft client = Minecraft.getInstance();
						if (lastHintKey == null) {
							if (client != null && client.gui != null && client.gui.hud != null) {
								client.gui.hud.getChat().addClientSystemMessage(
									Component.literal("[GTBS] No theme hint detected yet.").withStyle(ChatFormatting.WHITE)
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
						Minecraft client = Minecraft.getInstance();
						if (client != null && client.gui != null && client.gui.hud != null) {
							String raw = lastOverlayRaw;
							if (raw == null || raw.isBlank()) {
								client.gui.hud.getChat().addClientSystemMessage(
									Component.literal("[GTBS] No overlay captured yet.").withStyle(ChatFormatting.WHITE)
								);
							} else {
								client.gui.hud.getChat().addClientSystemMessage(
									Component.literal("[GTBS] overlay: ").withStyle(ChatFormatting.WHITE)
										.append(Component.literal(raw).withStyle(ChatFormatting.WHITE))
								);
							}
						}
						return 1;
					})
				)
				.then(literal("toggle")
					.executes(ctx -> {
						autoOutputEnabled = !autoOutputEnabled;
						sendClientMessage(ctx.getSource(), Component.literal("Auto output: ")
							.append(Component.literal(autoOutputEnabled ? "ON" : "OFF").withStyle(ChatFormatting.WHITE)));
						return 1;
					})
				)
			);
		});
	}

	private static void sendClientMessage(FabricClientCommandSource source, Component msg) {
		source.sendFeedback(Component.literal("[GTBS] ").withStyle(ChatFormatting.WHITE).append(msg));
	}

	/**
	 * Called by mixin when the action bar (overlay) message changes.
	 */
	public static void onActionBarMessage(Component message) {
		Minecraft client = Minecraft.getInstance();

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

	private static void printCandidatesToChat(Minecraft client, String hintKey, boolean allowAutoGuess) {
		if (client == null || client.gui == null || client.gui.hud == null || hintKey == null) return;

		HintPattern pattern = HintPattern.fromKey(hintKey);
		if (pattern == null) return;

		List<String> matches = Wordlist.findMatches(pattern, Integer.MAX_VALUE);

		int wordLength = pattern.toKey().length();
		if (matches.isEmpty()) {
			client.gui.hud.getChat().addClientSystemMessage(
				Component.literal("[GTBS] ").withStyle(ChatFormatting.WHITE)
					.append(Component.literal(pattern.pretty() + "  length: " + wordLength + "  No matches.").withStyle(ChatFormatting.WHITE))
			);
			return;
		}

		// Single match: auto-guess and notify (only when allowAutoGuess)
		if (allowAutoGuess && matches.size() == 1 && client.player != null) {
			String word = matches.get(0).toLowerCase();
			client.player.connection.sendChat(word);
			client.gui.hud.getChat().addClientSystemMessage(
				Component.literal("[GTBS] Auto-guessed: ").withStyle(ChatFormatting.WHITE)
					.append(Component.literal(word).withStyle(ChatFormatting.GREEN))
			);
			return;
		}

		client.gui.hud.getChat().addClientSystemMessage(
			Component.literal("[GTBS] ").withStyle(ChatFormatting.WHITE)
				.append(Component.literal(pattern.pretty() + "  length: " + wordLength).withStyle(ChatFormatting.WHITE))
		);

		MutableComponent line = Component.empty();
		for (String word : matches) {
			MutableComponent clickable = Component.literal(word)
				.setStyle(Style.EMPTY
					.withColor(ChatFormatting.GREEN)
					.withClickEvent(new ClickEvent.RunCommand("/gtbs guess " + word.toLowerCase()))
					.withHoverEvent(new HoverEvent.ShowText(
						Component.literal("[GTBS] Click to guess: ").withStyle(ChatFormatting.WHITE)
							.append(Component.literal(word).withStyle(ChatFormatting.GREEN))))
				);

			if (!line.getString().isEmpty()) {
				line = line.append(Component.literal("   ").withStyle(ChatFormatting.WHITE));
			}
			line = line.append(clickable);
		}

		client.gui.hud.getChat().addClientSystemMessage(line);
	}
}
