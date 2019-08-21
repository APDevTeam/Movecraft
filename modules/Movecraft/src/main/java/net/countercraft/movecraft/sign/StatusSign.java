package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class StatusSign implements Listener{

    @EventHandler
    public void onCraftDetect(CraftDetectEvent event){
        World world = event.getCraft().getW();
        for(MovecraftLocation location: event.getCraft().getHitBox()){
            Block block = location.toBukkit(world).getBlock();
            if(block.getState() instanceof Sign){
                Sign sign = (Sign) block.getState();
                if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Status:")) {
                    sign.setLine(1, "");
                    sign.setLine(2, "");
                    sign.setLine(3, "");
                    sign.update();
                }
            }
        }
    }

    @EventHandler
    public final void onSignTranslate(SignTranslateEvent event) {
        Craft craft = event.getCraft();
        if (!ChatColor.stripColor(event.getLine(0)).equalsIgnoreCase("Status:")) {
            return;
        }
        int fuel = 0;
        int totalBlocks = 0;
        Map<Material, Integer> foundBlocks = new HashMap<>();
        for (MovecraftLocation ml : craft.getHitBox()) {
            Material blockType = craft.getW().getBlockAt(ml.getX(), ml.getY(), ml.getZ()).getType();
            Integer count = foundBlocks.get(blockType);
            if (foundBlocks.containsKey(blockType)) {
                foundBlocks.put(blockType, count + 1);
            } else {
                foundBlocks.put(blockType, 1);
            }

            if (blockType == Material.FURNACE) {
                InventoryHolder inventoryHolder = (InventoryHolder) craft.getW().getBlockAt(ml.getX(), ml.getY(), ml.getZ()).getState();
                for (Material fuelType : Settings.FuelTypes.keySet()) {
                    if (!inventoryHolder.getInventory().contains(fuelType)) {
                        continue;
                    }
                    ItemStack[] content = inventoryHolder.getInventory().getContents();
                    for (ItemStack item : content) {
                        if (item == null)
                            continue;
                        fuel += item.getAmount() * Settings.FuelTypes.get(fuelType);
                    }
                }

            }
            if (blockType != Material.AIR || blockType != Material.CAVE_AIR || blockType != Material.VOID_AIR) {
                totalBlocks++;
            }
        }
        int signLine = 1;
        int signColumn = 0;
        for (Map<Material, List<Integer>> alFlyBlock : craft.getType().getFlyBlocks().keySet()) {
            ArrayList<Material> flyBlocks = new ArrayList<>(alFlyBlock.keySet());
            Collections.sort(flyBlocks);
            Double minimum = craft.getType().getFlyBlocks().get(alFlyBlock).get(0);
            int amount = 0;
            boolean addToStatus = false;
            for (Material flyBlock : flyBlocks) {
                if (foundBlocks.containsKey(flyBlock) && minimum > 0) { // if it has a minimum, it should be considered for sinking consideration
                    amount += foundBlocks.get(flyBlock);
                    addToStatus = true;
                }
            }
            Material flyBlock = flyBlocks.get(0);
            if (addToStatus) {
                Double percentPresent = (double) (amount * 100 / totalBlocks);
                Bukkit.broadcastMessage("test");
                String signText = "";
                if (percentPresent > minimum * 1.04) {
                    signText += ChatColor.GREEN;
                } else if (percentPresent > minimum * 1.02) {
                    signText += ChatColor.YELLOW;
                } else {
                    signText += ChatColor.RED;
                }
                Bukkit.broadcastMessage(Settings.StatusSignMarkers.get(flyBlocks));
                signText += Settings.StatusSignMarkers.get(flyBlocks);
                signText += " ";
                signText += percentPresent.intValue();
                signText += "/";
                signText += minimum.intValue();
                signText += "  ";
                Bukkit.broadcastMessage(signLine + " " + signColumn + signText);
                if (signColumn == 0) {
                    event.setLine(signLine, signText);
                    signColumn++;
                } else if (signLine < 3) {
                    String existingLine = event.getLine(signLine);
                    existingLine += signText;
                    event.setLine(signLine, existingLine);
                    signLine++;
                    signColumn = 0;
                }
            }
        }
        if (signLine < 3 && signColumn == 0){
            signLine++;
        }
        String fuelText="";
        int fuelRange=(int) ((fuel*(1+craft.getType().getCruiseSkipBlocks()))/craft.getType().getFuelBurnRate());
        if(fuelRange>1000) {
            fuelText+=ChatColor.GREEN;
        } else if(fuelRange>100) {
            fuelText+=ChatColor.YELLOW;
        } else {
            fuelText+=ChatColor.RED;
        }
        fuelText+="Fuel range:";
        fuelText+=fuelRange;
        event.setLine(signLine,fuelText);
    }
}