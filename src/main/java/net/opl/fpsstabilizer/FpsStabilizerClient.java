package net.opl.fpsstabilizer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Starts the adaptive profile automatically; no commands or keybinds are required. */
public final class FpsStabilizerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(AdaptiveController::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(AdaptiveController::stop);
    }
}
