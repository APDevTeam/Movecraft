package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.async.detection.DetectionTask;
import net.countercraft.movecraft.async.rotation.RotationTask;
import net.countercraft.movecraft.async.translation.TranslationTask;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static net.countercraft.movecraft.utils.SignUtils.getFacing;

public class PilotedCraft extends BaseCraft implements PlayerCraft{
    private final UUID id = UUID.randomUUID();
    private final Player pilot;
    public PilotedCraft(@NotNull CraftType type, @NotNull World world, @NotNull Player player) {
        super(type, world);
        this.pilot = player;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof PilotedCraft)){
            return false;
        }
        return this.id.equals(((PilotedCraft)obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @NotNull
    @Override
    public Player getPlayer() {
        return pilot;
    }
}
