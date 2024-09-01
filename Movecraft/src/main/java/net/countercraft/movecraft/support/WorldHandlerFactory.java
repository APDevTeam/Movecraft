package net.countercraft.movecraft.support;

import jakarta.inject.Provider;
import net.countercraft.movecraft.WorldHandler;
import net.countercraft.movecraft.config.Settings;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class WorldHandlerFactory implements Provider<WorldHandler> {
    private final @NotNull Logger logger;
    private final @NotNull VersionInfo versionInfo;

    public WorldHandlerFactory(@NotNull Logger logger, @NotNull VersionInfo versionInfo) {
        this.logger = logger;
        this.versionInfo = versionInfo;
    }

    @Override
    public WorldHandler get() {
        try {
            final Class<?> worldHandlerClazz = Class.forName("net.countercraft.movecraft.compat." + versionInfo.getPackageName() + ".IWorldHandler");
            // Check if we have a NMSHandler class at that location.
            if (WorldHandler.class.isAssignableFrom(worldHandlerClazz)) { // Make sure it actually implements NMS
                return (WorldHandler) worldHandlerClazz.getConstructor().newInstance(); // Set our handler
            }
        } catch (final Exception e) {
            if (!Settings.DisableNMSCompatibilityCheck) {
                throw new IllegalStateException("Could not find support for version %s.".formatted(versionInfo.version()));
            }
        }

        // Server owner claims to know what they are doing, warn them of the possible consequences
        logger.severe("""
                WARNING!
                Running Movecraft on an incompatible version can corrupt your world and break EVERYTHING!
                We provide no support for any issues.""");

        return null;
    }
}
