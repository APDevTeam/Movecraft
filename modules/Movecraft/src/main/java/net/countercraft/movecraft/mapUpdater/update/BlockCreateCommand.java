package net.countercraft.movecraft.mapUpdater.update;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.api.MovecraftLocation;
import net.countercraft.movecraft.api.craft.Craft;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;



public class BlockCreateCommand extends UpdateCommand {

    @SuppressWarnings("deprecation")
    private final int[] tileEntityBlocksToPreserve = {
            Material.DISPENSER.getId(), Material.NOTE_BLOCK.getId(), Material.CHEST.getId(),
            Material.FURNACE.getId(), Material.BURNING_FURNACE.getId(), Material.SIGN_POST.getId(),
            Material.WALL_SIGN.getId(), Material.COMMAND.getId(), Material.TRAPPED_CHEST.getId(),
            Material.DAYLIGHT_DETECTOR.getId(), Material.HOPPER.getId(), Material.DROPPER.getId(),
            Material.DAYLIGHT_DETECTOR_INVERTED.getId(), Material.COMMAND_REPEATING.getId(), Material.COMMAND_CHAIN.getId()};

    private MovecraftLocation newBlockLocation;
    private Material type;
    private byte dataID;
    private Craft craft;

    public BlockCreateCommand(@NotNull MovecraftLocation newBlockLocation, @NotNull Material type, byte dataID, Craft craft) {
        this.newBlockLocation = newBlockLocation;
        this.type = type;
        this.dataID = dataID;
        this.craft = craft;
    }



    @Override
    @SuppressWarnings("deprecation")
    public void doUpdate() {
        // now do the block updates, move entities when you set the block they are on




        if (shouldMakeChanges()) {
            Movecraft.getInstance().getWorldHandler().setBlockFast(newBlockLocation.toBukkit(craft.getW()),type,dataID);
            //craft.incrementBlockUpdates();
            newBlockLocation.toBukkit(craft.getW()).getBlock().getState().update(false, false);
        }


        //Do comperator stuff

        if (type == Material.REDSTONE_COMPARATOR_OFF) { // for some reason comparators are flakey, have to do it twice sometimes
            //Block b = updateWorld.getBlockAt(newBlockLocation.getX(), newBlockLocation.getY(), newBlockLocation.getZ());
            Block b = newBlockLocation.toBukkit(craft.getW()).getBlock();
            if (b.getType() != Material.REDSTONE_COMPARATOR_OFF) {
                b.setTypeIdAndData(type.getId(), dataID, false);
            }
        }
        if (type == Material.REDSTONE_COMPARATOR) { // for some reason comparators are flakey, have to do it twice sometimes
            Block b = newBlockLocation.toBukkit(craft.getW()).getBlock();
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
