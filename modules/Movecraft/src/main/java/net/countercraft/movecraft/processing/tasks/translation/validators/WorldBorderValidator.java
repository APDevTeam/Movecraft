package net.countercraft.movecraft.processing.tasks.translation.validators;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.processing.functions.TetradicPredicate;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.WorldBorder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class WorldBorderValidator implements TetradicPredicate<MovecraftLocation, MovecraftWorld, HitBox, CraftType> {
    @Override
    public @NotNull Result validate(@NotNull MovecraftLocation translation, @NotNull MovecraftWorld movecraftWorld, @NotNull HitBox hitBox, @NotNull CraftType type) {
        var border = movecraftWorld.getWorldBorder();
        for(var querry : new MovecraftLocation[]{new MovecraftLocation(hitBox.getMaxX(), hitBox.getMaxY(), hitBox.getMaxZ()), new MovecraftLocation(hitBox.getMinX(), hitBox.getMinY(), hitBox.getMinZ())}){
            if(!withinWorldBorder(border, querry)){
                return Result.failWithMessage(I18nSupport.getInternationalisedString("Translation - Failed Craft cannot pass world border") + String.format(" @ %d,%d,%d", querry.getX(), querry.getY(), querry.getZ()));
            }
        }
        return Result.succeed();
    }


    /**
     * Checks if a <link>MovecraftLocation</link> is within the border of the given <link>World</link>
     * @param border the border to check in
     * @param location the location in the given <link>World</link>
     * @return true if location is within the world border, false otherwise
     */
    @Contract(pure = true)
    private static boolean withinWorldBorder(@NotNull WorldBorder border, @NotNull MovecraftLocation location) {
        int radius = (int) (border.getSize() / 2.0);
        //The visible border will always end at 29,999,984 blocks, despite being larger
        int minX = border.getCenter().getBlockX() - radius;
        int maxX = border.getCenter().getBlockX() + radius;
        int minZ = border.getCenter().getBlockZ() - radius;
        int maxZ = border.getCenter().getBlockZ() + radius;
        return Math.abs(location.getX()) < 29999984 &&
                Math.abs(location.getZ()) < 29999984 &&
                location.getX() >= minX &&
                location.getX() <= maxX &&
                location.getZ() >= minZ &&
                location.getZ() <= maxZ;
    }
}
