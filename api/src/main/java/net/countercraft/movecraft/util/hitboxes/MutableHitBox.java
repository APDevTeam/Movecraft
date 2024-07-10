package net.countercraft.movecraft.util.hitboxes;

import net.countercraft.movecraft.MovecraftLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface MutableHitBox extends HitBox{
    public boolean add(@NotNull MovecraftLocation location);
    public boolean addAll(@NotNull Collection<? extends MovecraftLocation> collection);
    public boolean addAll(@NotNull HitBox hitBox);
    public boolean remove(@NotNull MovecraftLocation location);
    public boolean removeAll(@NotNull Collection<? extends MovecraftLocation> collection);
    public boolean removeAll(@NotNull HitBox hitBox);
    public void clear();
}
