package net.countercraft.movecraft.mapUpdater.update;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.registry.state.EnumProperty;
import com.sk89q.worldedit.registry.state.Property;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.utils.LegacyUtils;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.inventory.ItemStack;

public class WorldEditUpdateCommand extends UpdateCommand {
    private final BaseBlock worldEditBaseBlock;
    private final com.sk89q.worldedit.world.block.BaseBlock worldEdit7BaseBlock;
    private World world;
    private MovecraftLocation location;
    private Material type;
    private byte data;

    public WorldEditUpdateCommand(BaseBlock worldEditBaseBlock, World world, MovecraftLocation location, Material type, byte data) {
        this.worldEditBaseBlock = worldEditBaseBlock;
        worldEdit7BaseBlock = null;
        this.world = world;
        this.location = location;
        this.type = type;
        this.data = data;
    }
    //1.13 constructor
    public WorldEditUpdateCommand(com.sk89q.worldedit.world.block.BaseBlock worldEditBlockState, World world, MovecraftLocation location, Material type){
        worldEditBaseBlock = null;
        this.worldEdit7BaseBlock = worldEditBlockState;
        this.world = world;
        this.location = location;
        this.type = type;
    }

    @Override
    public void doUpdate() {
        Block block = world.getBlockAt(location.getX(), location.getY(), location.getZ());
        block.setType(type);
        if (Settings.IsLegacy) {
            LegacyUtils.setData(block, data);

            // put inventory into dispensers if its a repair
            if (type == Material.DISPENSER) {
                //DispenserBlock dispBlock = new DispenserBlock(worldEditBaseBlock.getData());
                //dispBlock.setNbtData(worldEditBaseBlock.getNbtData());
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
                Dispenser disp = (Dispenser) world.getBlockAt(location.getX(), location.getY(), location.getZ()).getState();
                if (numFireCharges > 0) {
                    ItemStack fireItems = new ItemStack(LegacyUtils.FIREBALL, numFireCharges);
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
            if (worldEditBaseBlock.getType() == 63 || worldEditBaseBlock.getType() == 68) {
                BlockState state = world.getBlockAt(location.getX(), location.getY(), location.getZ()).getState();
                if (state instanceof Sign) {
                    Sign s = (Sign) state;
                    CompoundTag nbtData = worldEditBaseBlock.getNbtData();
                    //first line
                    String firstLine = nbtData.getString("Text1");
                    firstLine = firstLine.substring(2);
                    if (firstLine.substring(0, 5).equalsIgnoreCase("extra")){
                        firstLine = firstLine.substring(17);
                        String[] parts = firstLine.split("\"");
                        firstLine = parts[0];
                    } else {
                        firstLine = "";
                    }
                    //second line
                    String secondLine = nbtData.getString("Text2");
                    secondLine = secondLine.substring(2);
                    if (secondLine.substring(0, 5).equalsIgnoreCase("extra")){
                        secondLine = secondLine.substring(17);
                        String[] parts = secondLine.split("\"");
                        secondLine = parts[0];
                    } else {
                        secondLine = "";
                    }
                    //third line
                    String thirdLine = nbtData.getString("Text3");
                    thirdLine = thirdLine.substring(2);
                    if (thirdLine.substring(0, 5).equalsIgnoreCase("extra")){
                        thirdLine = thirdLine.substring(17);
                        String[] parts = thirdLine.split("\"");
                        thirdLine = parts[0];
                    } else {
                        thirdLine = "";
                    }
                    //fourth line
                    String fourthLine = nbtData.getString("Text4");
                    fourthLine = fourthLine.substring(2);
                    if (fourthLine.substring(0, 5).equalsIgnoreCase("extra")){
                        fourthLine = fourthLine.substring(17);
                        String[] parts = fourthLine.split("\"");
                        fourthLine = parts[0];
                    } else {
                        fourthLine = "";
                    }
                    String[] lines = new String[4];
                    if (firstLine.equalsIgnoreCase("\\\\  ||  /")){
                        firstLine = "\\  ||  /";
                        secondLine = "==      ==";
                        thirdLine = "/  ||  \\";
                    }
                    lines[0] = firstLine;
                    lines[1] = secondLine;
                    lines[2] = thirdLine;
                    lines[3] = fourthLine;
                    for (int line = 0; line < lines.length; line++) {
                        s.setLine(line, lines[line]);
                    }
                    s.update(false, false);
                }
            }
        } else {
            BlockData bData = BukkitAdapter.adapt(worldEdit7BaseBlock);
            block.setBlockData(bData);
            if (type == Material.DISPENSER){
                Tag t = worldEdit7BaseBlock.getNbtData().getValue().get("Items");
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
                if (numFireCharges > 0) {
                    ItemStack fireItems = new ItemStack(Material.FIRE_CHARGE, numFireCharges);
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
            if (BukkitAdapter.adapt(worldEdit7BaseBlock.getBlockType()).equals(Material.SIGN)||BukkitAdapter.adapt(worldEdit7BaseBlock.getBlockType()).equals(Material.WALL_SIGN)){
                BlockState state = block.getState();
                if (state instanceof Sign) {
                    Sign s = (Sign) state;
                    CompoundTag nbtData = worldEdit7BaseBlock.getNbtData();
                    //first line
                    String firstLine = nbtData.getString("Text1");
                    firstLine = firstLine.substring(2);
                    if (firstLine.substring(0, 5).equalsIgnoreCase("extra")){
                        firstLine = firstLine.substring(17);
                        String[] parts = firstLine.split("\"");
                        firstLine = parts[0];
                    } else {
                        firstLine = "";
                    }
                    //second line
                    String secondLine = nbtData.getString("Text2");
                    secondLine = secondLine.substring(2);
                    if (secondLine.substring(0, 5).equalsIgnoreCase("extra")){
                        secondLine = secondLine.substring(17);
                        String[] parts = secondLine.split("\"");
                        secondLine = parts[0];
                    } else {
                        secondLine = "";
                    }
                    //third line
                    String thirdLine = nbtData.getString("Text3");
                    thirdLine = thirdLine.substring(2);
                    if (thirdLine.substring(0, 5).equalsIgnoreCase("extra")){
                        thirdLine = thirdLine.substring(17);
                        String[] parts = thirdLine.split("\"");
                        thirdLine = parts[0];
                    } else {
                        thirdLine = "";
                    }
                    //fourth line
                    String fourthLine = nbtData.getString("Text4");
                    fourthLine = fourthLine.substring(2);
                    if (fourthLine.substring(0, 5).equalsIgnoreCase("extra")){
                        fourthLine = fourthLine.substring(17);
                        String[] parts = fourthLine.split("\"");
                        fourthLine = parts[0];
                    } else {
                        fourthLine = "";
                    }
                    String[] lines = new String[4];
                    if (firstLine.equalsIgnoreCase("\\\\  ||  /")){
                        firstLine = "\\  ||  /";
                        secondLine = "==      ==";
                        thirdLine = "/  ||  \\";
                    }
                    lines[0] = firstLine;
                    lines[1] = secondLine;
                    lines[2] = thirdLine;
                    lines[3] = fourthLine;
                    for (int line = 0; line < lines.length; line++) {
                        s.setLine(line, lines[line]);
                    }
                    s.update(false, false);
                }
            }
        }
    }
}
