package net.countercraft.movecraft.support;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import net.countercraft.movecraft.SmoothTeleport;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.util.BukkitTeleport;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class SmoothTeleportFactory implements Provider<SmoothTeleport> {
    private final @NotNull Logger logger;
    private final @NotNull VersionInfo versionInfo;

    @Inject
    public SmoothTeleportFactory(@NotNull Logger logger, @NotNull VersionInfo versionInfo) {
        this.logger = logger;
        this.versionInfo = versionInfo;
    }

    public SmoothTeleport get(){
        try {
            // Try to set up the smooth teleport handler
            final Class<?> smoothTeleportClazz = Class.forName("net.countercraft.movecraft.support." + versionInfo.getPackageName() + ".ISmoothTeleport");
            if (SmoothTeleport.class.isAssignableFrom(smoothTeleportClazz)) {
                return (SmoothTeleport) smoothTeleportClazz.getConstructor().newInstance();
            }

            // Fall back to bukkit teleportation
            logger.warning("Did not find smooth teleport, falling back to bukkit teleportation provider.");

            return new BukkitTeleport();
        } catch (final ReflectiveOperationException e) {
            // Fall back to bukkit teleportation
            logger.warning("Falling back to bukkit teleportation provider.");
            if (Settings.Debug) {
                e.printStackTrace();
            }

            return new BukkitTeleport();
        }
    }
}
