package dev.lucasloepke.gtbsolver.mixin;

import dev.lucasloepke.gtbsolver.GtbSolverClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public final class InGameHudMixin {
	@Inject(method = "setOverlayMessage", at = @At("HEAD"))
	private void gtbsolver$onSetOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
		GtbSolverClient.onActionBarMessage(message);
	}
}

