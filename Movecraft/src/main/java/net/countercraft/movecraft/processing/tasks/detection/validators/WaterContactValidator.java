package net.countercraft.movecraft.processing.tasks.detection.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.type.PropertyKeys;
import net.countercraft.movecraft.craft.type.TypeSafeCraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.DetectionPredicate;
import net.countercraft.movecraft.processing.functions.Result;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.Map;

public class WaterContactValidator implements DetectionPredicate<Map<NamespacedKey, Deque<MovecraftLocation>>> {

    static final NamespacedKey WATER_ID = BlockType.WATER.getKey();

    @Override
    @Contract(pure = true)
    public @NotNull Result validate(@NotNull Map<NamespacedKey, Deque<MovecraftLocation>> materialDequeMap, @NotNull TypeSafeCraftType type, @NotNull MovecraftWorld world, @Nullable Player player) {
        return type.get(PropertyKeys.REQUIRE_WATER_CONTACT) && !materialDequeMap.containsKey(WATER_ID) ? Result.failWithMessage(I18nSupport.getInternationalisedString("Detection - Failed - Water contact required but not found")) : Result.succeed();
    }
}
