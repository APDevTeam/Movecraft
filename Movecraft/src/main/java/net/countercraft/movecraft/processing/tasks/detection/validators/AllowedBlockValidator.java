package net.countercraft.movecraft.processing.tasks.detection.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.type.PropertyKeys;
import net.countercraft.movecraft.craft.type.TypeSafeCraftType;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.DetectionPredicate;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.util.NamespacedIDUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AllowedBlockValidator implements DetectionPredicate<MovecraftLocation> {
    @Override
    @Contract(pure = true)
    public @NotNull Result validate(@NotNull MovecraftLocation movecraftLocation, @NotNull TypeSafeCraftType type, @NotNull MovecraftWorld world, @Nullable Player player) {
        return type.get(PropertyKeys.ALLOWED_BLOCKS).contains(NamespacedIDUtil.getBlockID(world.getData(movecraftLocation))) ? Result.succeed() : Result.fail();
    }
}
