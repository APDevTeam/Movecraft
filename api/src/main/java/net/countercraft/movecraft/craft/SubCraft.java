package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.block.TileState;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public interface SubCraft extends Craft {

    @NotNull
    Craft getParent();

    void setParent(@NotNull Craft parent);

    @Override
    default void removeUUIDMarkFromTile(TileState tile) {
        Craft parent = this.getParent();
        if (parent != null) {
            tile.getPersistentDataContainer().set(
                    MathUtils.KEY_CRAFT_UUID,
                    PersistentDataType.STRING,
                    parent.getUUID().toString()
            );
        } else {
            Craft.super.removeUUIDMarkFromTile(tile);
        }
    }
}
