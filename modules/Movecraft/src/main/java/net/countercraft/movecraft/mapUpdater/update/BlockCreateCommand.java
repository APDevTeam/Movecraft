package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.api.MovecraftLocation;
import net.countercraft.movecraft.api.craft.Craft;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;



public class BlockCreateCommand extends UpdateCommand {

    private MovecraftLocation newBlockLocation;
    private Material type;
    private byte dataID;
    private World world;

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



    @Override
    @SuppressWarnings("deprecation")
    public void doUpdate() {
        // now do the block updates, move entities when you set the block they are on

        if (shouldMakeChanges()) {
            Movecraft.getInstance().getWorldHandler().setBlockFast(newBlockLocation.toBukkit(world),type,dataID);
            //craft.incrementBlockUpdates();
            newBlockLocation.toBukkit(world).getBlock().getState().update(false, false);
        }


        //Do comperator stuff

        if (type == Material.REDSTONE_COMPARATOR_OFF) { // for some reason comparators are flakey, have to do it twice sometimes
            //Block b = updateWorld.getBlockAt(newBlockLocation.getX(), newBlockLocation.getY(), newBlockLocation.getZ());
            Block b = newBlockLocation.toBukkit(world).getBlock();
            if (b.getType() != Material.REDSTONE_COMPARATOR_OFF) {
                b.setTypeIdAndData(type.getId(), dataID, false);
            }
        }
        if (type == Material.REDSTONE_COMPARATOR) { // for some reason comparators are flakey, have to do it twice sometimes
            Block b = newBlockLocation.toBukkit(world).getBlock();
            if (b.getType() != Material.REDSTONE_COMPARATOR) {
                b.setTypeIdAndData(type.getId(), dataID, false);
            }
        }

        //TODO: Re-add sign updating
    }

    private boolean shouldMakeChanges(){
        return true;
    }


}
