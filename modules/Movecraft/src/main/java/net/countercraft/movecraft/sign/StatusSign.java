package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.countercraft.movecraft.utils.BlockLimitManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
        int totalBlocks = craft.getHitBox().size();
        int signLine = 1;
        int signColumn = 0;
        final BlockLimitManager flyBlocks = craft.getType().getFlyBlocks();
        Map<BlockLimitManager.Entry, Integer> foundFlyBlocks = new HashMap<>();
        for (MovecraftLocation ml : craft.getHitBox()){
            Location loc = ml.toBukkit(craft.getW());
            Material testType = loc.getBlock().getType();
            byte data = 0;
            if (Settings.IsLegacy){
                data = loc.getBlock().getData();
            }

            if (flyBlocks.contains(testType)){
                if (foundFlyBlocks.containsKey(flyBlocks.get(testType))){
                    int count = foundFlyBlocks.get(flyBlocks.get(testType));
                    count++;
                    foundFlyBlocks.put(flyBlocks.get(testType), count);
                } else {
                    foundFlyBlocks.put(flyBlocks.get(testType), 1);
                }
            } else if (flyBlocks.contains(testType, data)){
                if (foundFlyBlocks.containsKey(flyBlocks.get(testType, data))){
                    int count = foundFlyBlocks.get(flyBlocks.get(testType, data));
                    count++;
                    foundFlyBlocks.put(flyBlocks.get(testType, data), count);
                } else {
                    foundFlyBlocks.put(flyBlocks.get(testType, data), 1);
                }
            }

            if (testType == Material.FURNACE) {
                Furnace furnace = (Furnace) loc.getBlock().getState();
                for (ItemStack fuelStack : furnace.getInventory().getContents()) {
                    if (fuelStack == null || !Settings.FuelTypes.containsKey(fuelStack.getType())) {
                        continue;
                    }
                    fuel += fuelStack.getAmount() * Settings.FuelTypes.get(fuelStack.getType());
                }
            }
        }
        for (BlockLimitManager.Entry entry : foundFlyBlocks.keySet()) {
            int amount = foundFlyBlocks.get(entry);
            double minimum = entry.getLowerLimit();
            if (minimum == 0)
                continue;
            double percentPresent = ((double) amount * 100 / (double) totalBlocks);
                String signText = "";
                if (percentPresent > minimum * 1.04) {
                    signText += ChatColor.GREEN;
                } else if (percentPresent > minimum * 1.02) {
                    signText += ChatColor.YELLOW;
                } else {
                    signText += ChatColor.RED;
                }
                String[] parts = new ArrayList<>(entry.getBlocks()).get(0).getType().name().split("_");
                signText += parts[ entry.getBlocks().size() > 1 ? parts.length - 1 : 0 ].charAt(0);
                signText += " ";
                signText += (int) percentPresent;
                signText += "/";
                signText += (int) minimum;
                signText += "  ";
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

        if (signLine < 3 && signColumn == 1){
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