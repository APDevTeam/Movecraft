package net.countercraft.movecraft.features.fading;

import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.util.MathUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class WreckManager {
    private final @NotNull WorldManager worldManager;

    public WreckManager(@NotNull WorldManager worldManager){
        this.worldManager = Objects.requireNonNull(worldManager);
    }

    public void queueWreck(Craft craft){
        if(craft.getCollapsedHitBox().isEmpty() || Settings.FadeWrecksAfter == 0){
            return;
        }

        worldManager.submit(new WreckTask(
            craft.getCollapsedHitBox(),
            craft.getMovecraftWorld(),
            craft
                .getPhaseBlocks()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                    entry -> MathUtils.bukkit2MovecraftLoc(entry.getKey()),
                    Map.Entry::getValue
                ))));
    }
}
