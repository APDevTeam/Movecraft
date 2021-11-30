package net.countercraft.movecraft.craft;

import org.jetbrains.annotations.NotNull;

public interface SubCraft extends Craft {

    @NotNull
    Craft getParent();

    void setParent(@NotNull Craft parent);
}
