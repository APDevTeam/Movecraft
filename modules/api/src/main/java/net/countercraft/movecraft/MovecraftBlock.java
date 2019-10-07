package net.countercraft.movecraft;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class MovecraftBlock {
    private final Material type;
    private final Byte data;
    public MovecraftBlock(Material type, Byte data){
        this.type = type;
        this.data = data;
    }

    public MovecraftBlock(Material type){
        this.type = type;
        this.data = null;
    }

    @NotNull
    public Material getType() {
        return type;
    }

    @Nullable
    public Byte getData() {
        return data;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MovecraftBlock)){
            return false;
        }
        MovecraftBlock mb = (MovecraftBlock) obj;
        return getType() == mb.getType() && getData() == mb.getData();
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, data);
    }
}

