package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;


public class BlockCreateCommand extends UpdateCommand {

    final private MovecraftLocation newBlockLocation;
    final private BlockData data;
    final private World world;

    public BlockCreateCommand(@NotNull MovecraftLocation newBlockLocation, BlockData data, @NotNull Craft craft) {
        this.newBlockLocation = newBlockLocation;
        this.data = data;
        this.world = craft.getWorld();
    }

    public BlockCreateCommand(@NotNull World world, @NotNull MovecraftLocation newBlockLocation, @NotNull BlockData data) {
        this.newBlockLocation = newBlockLocation;
        this.data = data;
        this.world = world;
    }

    public BlockCreateCommand(@NotNull World world, @NotNull MovecraftLocation newBlockLocation, @NotNull Material type) {
        this.newBlockLocation = newBlockLocation;
        this.data = type.createBlockData();
        this.world = world;
    }



    @Override
    @SuppressWarnings("deprecation")
    public void doUpdate() {
        // now do the block updates, move entities when you set the block they are on
        Movecraft.getInstance().getWorldHandler().setBlockFast(newBlockLocation.toBukkit(world), data);
        //craft.incrementBlockUpdates();
        newBlockLocation.toBukkit(world).getBlock().getState().update(false, false);

        //Do comperator stuff

        if (data.getMaterial() == Material.COMPARATOR) { // for some reason comparators are flakey, have to do it twice sometimes
            Block b = newBlockLocation.toBukkit(world).getBlock();
            if (b.getType() != Material.COMPARATOR) {
                b.setType(data.getMaterial(), false);
            }
        }

        //TODO: Re-add sign updating
    }

    @Override
    public int hashCode() {
        return Objects.hash(newBlockLocation, data, world.getUID());
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof BlockCreateCommand)){
            return false;
        }
        BlockCreateCommand other = (BlockCreateCommand) obj;
        return other.newBlockLocation.equals(this.newBlockLocation)  &&
                other.data.equals(this.data) &&
                other.world.equals(this.world);
    }
}
