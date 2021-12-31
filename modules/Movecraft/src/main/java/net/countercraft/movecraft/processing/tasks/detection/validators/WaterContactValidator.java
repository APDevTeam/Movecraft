package net.countercraft.movecraft.processing.tasks.detection.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.DetectionPredicate;
import net.countercraft.movecraft.processing.functions.Result;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.Map;

public class WaterContactValidator implements DetectionPredicate<Map<Material, Deque<MovecraftLocation>>> {
    @Override
    @Contract(pure = true)
    public @NotNull Result validate(@NotNull Map<Material, Deque<MovecraftLocation>> materialDequeMap, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable Player player) {
        return type.getBoolProperty(CraftType.REQUIRE_WATER_CONTACT) && !materialDequeMap.containsKey(Material.WATER) ? Result.failWithMessage(I18nSupport.getInternationalisedString("Detection - Failed - Water contact required but not found")) : Result.succeed();
    }
}
