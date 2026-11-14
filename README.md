# FPS Stabilizer — Fabric 26.1.2

Client-side FPS stabilizer for Minecraft Java 26.1.2. It runs automatically: no chat commands, keybinds, or manual profile selection.

## What it does

- Detects when a world is entered and captures the current video options.
- Applies a conservative performance profile: shorter view/simulation distance, Fast graphics, reduced particles, clouds off, entity shadows off, lower entity render distance, reduced biome blending, and ambient occlusion off.
- Measures actual rendered frame times (average FPS and 1% low) through a lightweight render hook.
- Checks performance at spaced intervals. If slow frame times persist, it reduces view distance one step at a time; only after reaching the view-distance floor can it reduce simulation distance.
- When performance stays comfortably high for a longer period, it gradually restores view/simulation distance, without exceeding the user's original values (or the mod's safety cap).
- Restores the user's original video options when returning to the title screen or closing Minecraft.

## Compatibility

- Minecraft Java Edition 26.1.2
- Fabric Loader 0.19.2+
- Fabric API compatible with 26.1.2 (project currently targets 0.155.3+26.1.2)
- Java 25+

## Build

Use JDK 25 and Gradle 9.4.1 or a compatible Gradle 9.x release with Fabric Loom 1.16-SNAPSHOT. From this directory, run:

```bash
gradle build
```

The mod jar will be created at:

```text
build/libs/fps-stabilizer-1.1.0.jar
```

Install the jar in `.minecraft/mods` alongside Fabric API for Minecraft 26.1.2.

## Notes and limitations

- This mod is a client-side settings controller, not a renderer replacement. It cannot create extra GPU/CPU capacity; the result depends on hardware, shaders, resource packs, world complexity, and the server.
- The mod does not forcibly change resolution, FPS cap, shaders, texture packs, or other mods' settings.
- Adaptive view distance is bounded. It will not raise the user's original view distance and will restore the original settings after leaving the world.
- This source project has not been compiled or play-tested in Minecraft in the current environment. A successful Gradle build and an in-game smoke test are still required before treating a jar as verified.
