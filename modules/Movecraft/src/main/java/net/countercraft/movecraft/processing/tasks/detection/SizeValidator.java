package net.countercraft.movecraft.processing.tasks.detection;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.processing.MovecraftWorld;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.Map;

public class SizeValidator implements DetectionValidator<Map<Material, Deque<MovecraftLocation>>>{
    @Override
    public Modifier validate(@NotNull Map<Material, Deque<MovecraftLocation>> materialDequeMap, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable Player player) {
        int size = materialDequeMap.values().parallelStream().map(Deque::size).reduce(Integer::sum).orElse(0);
        return size > type.getMaxSize() || size < type.getMinSize() ? Modifier.FAIL : Modifier.NONE;
    }
}
