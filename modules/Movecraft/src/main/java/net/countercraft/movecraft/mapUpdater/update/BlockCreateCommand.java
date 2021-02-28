package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;


public class BlockCreateCommand extends UpdateCommand {

    final private MovecraftLocation newBlockLocation;
    final private Material type;
    final private byte dataID;
    final private World world;

    public BlockCreateCommand(@NotNull MovecraftLocation newBlockLocation, @NotNull Material type, byte dataID, @NotNull Craft craft) {
        this.newBlockLocation = newBlockLocation;
        this.type = type;
        this.dataID = dataID;
        this.world = craft.getW();
    }

    public BlockCreateCommand(@NotNull World world, @NotNull MovecraftLocation newBlockLocation, @NotNull Material type, byte dataID) {
        this.newBlockLocation = newBlockLocation;
        this.type = type;
        this.dataID = dataID;
        this.world = world;
    }

    public BlockCreateCommand(@NotNull World world, @NotNull MovecraftLocation newBlockLocation, @NotNull Material type) {
        this.newBlockLocation = newBlockLocation;
        this.type = type;
        this.dataID = (byte)0;
        this.world = world;
    }



    @Override
    @SuppressWarnings("deprecation")
    public void doUpdate() {
        // now do the block updates, move entities when you set the block they are on
        Movecraft.getInstance().getWorldHandler().setBlockFast(newBlockLocation.toBukkit(world),type,dataID);
        //craft.incrementBlockUpdates();
        newBlockLocation.toBukkit(world).getBlock().getState().update(false, false);

        //Do comperator stuff

        if (type == Material.COMPARATOR) { // for some reason comparators are flakey, have to do it twice sometimes
            Block b = newBlockLocation.toBukkit(world).getBlock();
            if (b.getType() != Material.COMPARATOR) {
                b.setType(type, false);
            }
        }

        //TODO: Re-add sign updating
    }

    @Override
    public int hashCode() {
        return Objects.hash(newBlockLocation, type, dataID, world.getUID());
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof BlockCreateCommand)){
            return false;
        }
        BlockCreateCommand other = (BlockCreateCommand) obj;
        return other.newBlockLocation.equals(this.newBlockLocation) &&
                other.type.equals(this.type) &&
                other.dataID == this.dataID &&
                other.world.equals(this.world);
    }
}
