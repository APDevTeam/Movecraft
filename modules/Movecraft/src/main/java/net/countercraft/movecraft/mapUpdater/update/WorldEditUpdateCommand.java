package net.countercraft.movecraft.mapUpdater.update;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.blocks.BaseBlock;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

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
        if (worldEditBaseBlock.getType() == 63 ||worldEditBaseBlock.getType() == 68 ){
            BlockState state = block.getState();
            if (state instanceof Sign) {
                Sign s = (Sign) state;
                CompoundTag nbtData = worldEditBaseBlock.getNbtData();
                //Text NBT tags for first to fourth line are called Text1 - Text4
                for (int i = 1 ; i <= 4 ; i++){
                    String line = nbtData.getString("Text" + i);
                    line = line.replace("\"", "");

                    line = line.substring(1);

                    if (line.substring(0, 5).equalsIgnoreCase("extra")){
                        HashMap<String, String> stringMap = new HashMap<>();
                        line = line.substring(7);
                        line = line.replace("],text:}", "");
                        final String[] parts = line.split("},\\{");
                        line = "";
                        for (int index = 0 ; index < parts.length ; index++){
                            String part = parts[index];
                            if (part.startsWith("{"))
                                part = part.substring(1);
                            if (part.endsWith("}"))
                                part = part.substring(0, part.lastIndexOf("}"));
                            if (part.startsWith("color")){
                                final String[] fragments = part.split(",");
                                final String colorID = fragments[0].replace("color:", "");
                                final String text = fragments[1].replace("text:", "");
                                line += ChatColor.valueOf(colorID.toUpperCase());
                                line += text;
                            } else {
                                line = part.replace("text:", "");
                            }
                        }
                        if (Settings.Debug){
                            Bukkit.broadcastMessage("Text on line " + i + " at " + location.toString() + ": " + line);
                        }
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
        if (type == Material.FURNACE){
            ListTag list = worldEditBaseBlock.getNbtData().getListTag("Items");
            FurnaceInventory fInv = ((Furnace) block.getState()).getInventory();
            if (list != null){
                for (Tag t : list.getValue()){
                    if (!(t instanceof CompoundTag)){
                        continue;
                    }
                    CompoundTag ct = (CompoundTag) t;
                    byte slot = ct.getByte("Slot");
                    if (slot == 2){//Ignore the result slot
                        continue;
                    }
                    String id = ct.getString("id");
                    ImmutablePair<Material, Byte> content;
                    if (id.equals("minecraft:coal")){
                        byte data = (byte) ct.getShort("Damage");
                        byte count = ct.getByte("Count");
                        //Smelting slot

                        if (slot == 0) {
                            if (fInv.getSmelting() != null && fInv.getSmelting().getData().getData() == data){
                                fInv.getSmelting().setAmount(count);
                            } else {
                                fInv.setSmelting(new ItemStack(Material.COAL, count, (short) 0, data));
                            }
                        } else if (slot == 1) {//Fuel slot
                            if (fInv.getFuel() != null && fInv.getFuel().getData().getData() == data){
                                fInv.getFuel().setAmount(count);
                            } else {
                                fInv.setFuel(new ItemStack(Material.COAL, count, (short) 0, data));
                            }
                        }

                    }
                    if (id.equals("minecraft:coal_block")){
                        byte count = ct.getByte("Count");
                        //Smelting slot
                        //Fuel slot
                        if (slot == 0) {
                            if (fInv.getSmelting() != null){
                                fInv.getSmelting().setAmount(count);
                            } else {
                                fInv.setSmelting(new ItemStack(Material.COAL_BLOCK, count));
                            }
                        } else if (slot == 1) {//Fuel slot
                            if (fInv.getFuel() != null){
                                fInv.getFuel().setAmount(count);
                            } else {
                                fInv.setFuel(new ItemStack(Material.COAL_BLOCK, count));
                            }
                        }
                    }
                }
            }
        }
    }
}
