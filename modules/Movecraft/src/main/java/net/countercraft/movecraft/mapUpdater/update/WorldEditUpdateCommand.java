package net.countercraft.movecraft.mapUpdater.update;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.blocks.BaseBlock;
import net.countercraft.movecraft.MovecraftLocation;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;

public class WorldEditUpdateCommand extends UpdateCommand {
    private final BaseBlock worldEditBaseBlock;
    private World world;
    private MovecraftLocation location;
    private Material type;
    private byte data;

    public WorldEditUpdateCommand(BaseBlock worldEditBaseBlock, World world, MovecraftLocation location, Material type, byte data) {
        this.worldEditBaseBlock = worldEditBaseBlock;
        this.world = world;
        this.location = location;
        this.type = type;
        this.data = data;
    }

    @Override
    public void doUpdate() {
        world.getBlockAt(location.getX(), location.getY(), location.getZ()).setType(type);
        world.getBlockAt(location.getX(), location.getY(), location.getZ()).setData(data);
        Block block = location.toBukkit(world).getBlock();
        // put inventory into dispensers if its a repair
        if (type == Material.DISPENSER){
            Tag t = worldEditBaseBlock.getNbtData().getValue().get("Items");
            ListTag lt = null;
            if (t instanceof ListTag) {
                lt = (ListTag) t;
            }
            int numFireCharges = 0;
            int numTNT = 0;
            int numWater = 0;
            if (lt != null) {
                for (Tag entryTag : lt.getValue()) {
                    if (entryTag instanceof CompoundTag) {
                        CompoundTag cTag = (CompoundTag) entryTag;
                        if (cTag.toString().contains("minecraft:tnt")) {
                            numTNT += cTag.getByte("Count");
                        }
                        if (cTag.toString().contains("minecraft:fire_charge")) {
                            numFireCharges += cTag.getByte("Count");
                        }
                        if (cTag.toString().contains("minecraft:water_bucket")) {
                            numWater += cTag.getByte("Count");
                        }
                    }
                }
            }
            Dispenser disp = (Dispenser) block.getState();
            ItemStack[] contents = disp.getInventory().getContents();
            for (ItemStack iStack : contents){
                if (iStack == null){
                    continue;
                }
                if (iStack.getType().equals(Material.TNT)){
                    numTNT -= iStack.getAmount();
                }
                if (iStack.getType().equals(Material.WATER_BUCKET)){
                    numWater -= iStack.getAmount();
                }
                if (iStack.getType().equals(Material.FIREBALL)){
                    numFireCharges -= iStack.getAmount();
                }
            }
            if (numFireCharges > 0) {
                ItemStack fireItems = new ItemStack(Material.FIREBALL, numFireCharges);
                disp.getInventory().addItem(fireItems);
            }
            if (numTNT > 0) {
                ItemStack TNTItems = new ItemStack(Material.TNT, numTNT);
                disp.getInventory().addItem(TNTItems);
            }
            if (numWater > 0) {
                ItemStack WaterItems = new ItemStack(Material.WATER_BUCKET, numWater);
                disp.getInventory().addItem(WaterItems);
            }
        }
        if (type == Material.FURNACE){
            Furnace furnace = (Furnace) block.getState();
            FurnaceInventory fInv = furnace.getInventory();
            CompoundTag nbtData = worldEditBaseBlock.getNbtData();
            ListTag lt = nbtData.getListTag("Items");
            if (lt != null){
                for (Tag t : lt.getValue()){
                    if (!(t instanceof CompoundTag)){
                        continue;
                    }
                    CompoundTag ct = (CompoundTag) t;
                    if (ct.getString("id").equals("minecraft:coal")){
                        double count = (double) ct.getByte("Count");
                        switch (ct.getByte("Slot")) {
                            case 0:
                                if (fInv.getSmelting() != null && fInv.getSmelting().getType().equals(Material.COAL)) {
                                    if (fInv.getSmelting().getData().getData() != ct.getShort("Damage")){
                                        ItemStack istack = new ItemStack(Material.COAL, (int) count, (short) 0, (byte) ct.getShort("Damage"));
                                        fInv.setFuel(istack);
                                        break;
                                    }
                                    fInv.getSmelting().setAmount((int) count);
                                } else {
                                    ItemStack istack = new ItemStack(Material.COAL, (int) count, (short) 0, (byte) ct.getShort("Damage"));
                                    fInv.setSmelting(istack);
                                }
                                break;
                            case 1:
                                if (fInv.getFuel() != null && fInv.getFuel().getType().equals(Material.COAL)) {
                                    if (fInv.getFuel().getData().getData() != ct.getShort("Damage")){
                                        ItemStack istack = new ItemStack(Material.COAL, (int) count, (short) 0, (byte) ct.getShort("Damage"));
                                        fInv.setFuel(istack);
                                        break;
                                    }
                                    fInv.getFuel().setAmount((int) count);
                                } else {
                                    ItemStack istack = new ItemStack(Material.COAL, (int) count, (short) 0, (byte) ct.getShort("Damage"));
                                    fInv.setFuel(istack);
                                }
                                break;
                        }
                    }
                    if (ct.getString("id").equals("minecraft:coal_block")){
                        double count = (double) ct.getByte("Count");
                        switch (ct.getByte("Slot")){
                            case 0:
                                if (fInv.getSmelting() != null && fInv.getSmelting().getType().equals(Material.COAL_BLOCK)) {
                                    fInv.getSmelting().setAmount((int) count);
                                } else {
                                    fInv.setSmelting(new ItemStack(Material.COAL_BLOCK, (int) count));
                                }
                                break;
                            case 1:
                                if (fInv.getFuel() != null && fInv.getFuel().getType().equals(Material.COAL_BLOCK)) {
                                    fInv.getFuel().setAmount((int) count);
                                } else {
                                    fInv.setFuel(new ItemStack(Material.COAL_BLOCK, (int) count));
                                }
                                break;
                        }
                    }
                }
            }
        }
        if (worldEditBaseBlock.getType() == 63 ||worldEditBaseBlock.getType() == 68 ){
            BlockState state = block.getState();
            if (state instanceof Sign) {
                Sign s = (Sign) state;
                CompoundTag nbtData = worldEditBaseBlock.getNbtData();
                //Text NBT tags for first to fourth line are called Text1 - Text4
                for (int i = 1 ; i <= 4 ; i++){
                    String line = nbtData.getString("Text" + i);
                    line = line.substring(2);
                    if (line.substring(0, 5).equalsIgnoreCase("extra")){
                        line = line.substring(17);
                        String[] parts = line.split("\"");
                        line = parts[0];
                    } else {
                        line = "";
                    }
                    if (i == 1 && line.equalsIgnoreCase("\\\\  ||  /")){
                        s.setLine(0,"\\  ||  /");
                        s.setLine(1,"==      ==");
                        s.setLine(2,"/  ||  \\");
                        break;
                    }
                    s.setLine(i - 1, line);
                }
                s.update(false, false);
            }
        }
    }
}
