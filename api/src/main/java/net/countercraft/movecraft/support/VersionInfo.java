package net.countercraft.movecraft.support;

import org.jetbrains.annotations.NotNull;

public record VersionInfo(String version) {
    public @NotNull String getPackageName() {
        String[] parts = version.split("\\.");
        if (parts.length < 2)
            throw new IllegalArgumentException();

        return "v1_" + parts[1];
    }
}
