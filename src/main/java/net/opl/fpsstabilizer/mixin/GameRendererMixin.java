package net.opl.fpsstabilizer.mixin;

import net.opl.fpsstabilizer.FrameTimeTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.GameRenderer")
abstract class GameRendererMixin {
    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void fpsstabilizer$afterFrame(CallbackInfo callbackInfo) {
        FrameTimeTracker.onFrame();
    }
}
