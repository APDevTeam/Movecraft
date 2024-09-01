package net.countercraft.movecraft.support;

import jakarta.inject.Provider;
import net.countercraft.movecraft.WorldHandler;
import net.countercraft.movecraft.config.Settings;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class VersionProvider implements Provider<VersionInfo> {
    private final @NotNull Plugin plugin;

    public VersionProvider(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public VersionInfo get() {
        String minecraftVersion = plugin.getServer().getMinecraftVersion();

        return new VersionInfo(minecraftVersion);
    }
}
