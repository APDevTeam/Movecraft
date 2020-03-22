package net.countercraft.movecraft.mapUpdater.update;

import com.google.gson.Gson;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.block.BaseBlock;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.utils.Pair;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WorldEdit7UpdateCommand extends UpdateCommand {
    private final BaseBlock baseBlock;
    private World world;
    private MovecraftLocation location;
    private Material type;
    public WorldEdit7UpdateCommand(BaseBlock baseBlock, World world, MovecraftLocation location, Material type){
        this.baseBlock = baseBlock;
        this.world = world;
        this.location = location;
        this.type = type;
    }
    @Override
    public void doUpdate() {
        Block block = world.getBlockAt(location.getX(), location.getY(), location.getZ());
        block.setType(type);
        assert baseBlock != null;
        BlockData bData = BukkitAdapter.adapt(baseBlock);
        block.setBlockData(bData);
        if (Settings.Debug){
            Bukkit.broadcastMessage(String.format("Material: %s, Location: %s",type.name().toLowerCase().replace("_", " "),location.toString()));
        }
        Material weType = BukkitAdapter.adapt(baseBlock.getBlockType());
        if (type == Material.DISPENSER){
            Tag t = baseBlock.getNbtData().getValue().get("Items");
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
                if (iStack.getType().equals(Material.FIRE_CHARGE)){
                    numFireCharges -= iStack.getAmount();
                }
            }
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
        if (type.name().endsWith("SIGN")){
            BlockState state = block.getState();
            if (state instanceof Sign) {
                Sign s = (Sign) state;
                CompoundTag nbtData = baseBlock.getNbtData();
                //Text NBT tags for first to fourth line are called Text1 - Text4
                final Gson gson = new Gson();
                final String[] STYLES = {"bold", "italic", "underline", "strikethrough"};
                for (int i = 1 ; i <= 4 ; i++){
                    Map lineData = gson.fromJson(nbtData.getString("Text" + i), Map.class);
                    if (!lineData.containsKey("extra"))
                        continue;
                    List<Map> extras = (List<Map>) lineData.get("extra");
                    String line = "";
                    for (Map textComponent : extras) {
                        if (textComponent.containsKey("color")) {
                            line += ChatColor.valueOf(((String) textComponent.get("color")).toUpperCase());
                        }
                        for (String style : STYLES) {
                            if (!(boolean) textComponent.getOrDefault(style, false)) {
                                continue;
                            }
                            line += ChatColor.valueOf(style.toUpperCase());
                        }
                        line += textComponent.get("text");
                    }
                    if (i == 1 && line.equalsIgnoreCase("\\\\  ||  /")) {
                        s.setLine(0, "\\  ||  /");
                        s.setLine(1, "==      ==");
                        s.setLine(2, "/  ||  \\");
                        break;
                    }
                    s.setLine(i - 1, line);
                }
                s.update(false, false);
            }
        }
        if (type == Material.FURNACE){
            ListTag list = baseBlock.getNbtData().getListTag("Items");
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
                    Pair<Material, Byte> content;
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

    public BaseBlock getBaseBlock() {
        return baseBlock;
    }

    public MovecraftLocation getLocation() {
        return location;
    }

    public World getWorld() {
        return world;
    }

    public Material getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WorldEdit7UpdateCommand)){
            return false;
        }
        WorldEdit7UpdateCommand weUp = (WorldEdit7UpdateCommand) obj;

        return weUp.getBaseBlock() == getBaseBlock() && weUp.getLocation() == getLocation() && weUp.getWorld() == getWorld() && weUp.getType() == getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseBlock, location, world, type);
    }
}
