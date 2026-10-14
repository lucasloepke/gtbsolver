package dev.lucasloepke.gtbsolver.mixin;

import dev.lucasloepke.gtbsolver.GtbSolverClient;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public final class HudMixin {
	@Inject(method = "setOverlayMessage", at = @At("HEAD"))
	private void gtbsolver$onSetOverlayMessage(Component message, boolean animate, CallbackInfo ci) {
		GtbSolverClient.onActionBarMessage(message);
	}
}
