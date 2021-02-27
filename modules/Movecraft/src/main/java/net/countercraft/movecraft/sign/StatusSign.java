package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;

import net.countercraft.movecraft.utils.BlockLimitManager;
import net.countercraft.movecraft.utils.Counter;
import net.countercraft.movecraft.utils.SignUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Map;

public final class StatusSign implements Listener{

    @EventHandler
    public void onCraftDetect(CraftDetectEvent event){
        World world = event.getCraft().getWorld();
        for(MovecraftLocation location: event.getCraft().getHitBox()){
            Block block = location.toBukkit(world).getBlock();
            if(!SignUtils.isSign(block)){
                return;
            }
            Sign sign = (Sign) block.getState();
            if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Status:")) {
                sign.setLine(1, "");
                sign.setLine(2, "");
                sign.setLine(3, "");
                sign.update();
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
        Counter<Material> foundBlocks = new Counter<>();
        for (MovecraftLocation ml : craft.getHitBox()) {
            Material blockID = craft.getW().getBlockAt(ml.getX(), ml.getY(), ml.getZ()).getType();
            foundBlocks.add(blockID);

            if (blockID == Material.FURNACE) {
                InventoryHolder inventoryHolder = (InventoryHolder) craft.getW().getBlockAt(ml.getX(), ml.getY(), ml.getZ()).getState();
                Map<Material, Double> fuelTypes = craft.getType().getFuelTypes();
                for (ItemStack iStack : inventoryHolder.getInventory()) {
                    if (iStack == null || !fuelTypes.containsKey(iStack.getType())) {
                        continue;
                    }
                    fuel += iStack.getAmount() * fuelTypes.get(iStack.getType());
                }
            }
            if (blockID != Material.AIR && blockID != Material.FIRE) {
                totalBlocks++;
            }
        }
        int signLine = 1;
        int signColumn = 0;
        for (BlockLimitManager.Entry alFlyBlockID : craft.getType().getFlyBlocks().getEntries()) {
            Material flyBlockID = new ArrayList<>(alFlyBlockID.getBlocks()).get(0).getType();
            double minimum = craft.getType().getFlyBlocks().getLowerLimit(flyBlockID);
            if (foundBlocks.get(flyBlockID) != 0 && minimum > 0) { // if it has a minimum, it should be considered for sinking consideration
                int amount = foundBlocks.get(flyBlockID);
                double percentPresent = (amount * 100D / totalBlocks);
                String signText = "";
                if (percentPresent > minimum * 1.04) {
                    signText += ChatColor.GREEN;
                } else if (percentPresent > minimum * 1.02) {
                    signText += ChatColor.YELLOW;
                } else {
                    signText += ChatColor.RED;
                }
                signText += flyBlockID.name().charAt(0);


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

            if (signLine < 3 && signColumn == 1) {
                signLine++;
            }
            String fuelText = "";
            int fuelRange = (int) ((fuel * (1 + (craft.getType().getCruiseSkipBlocks(craft.getWorld()) + 1))) / craft.getType().getFuelBurnRate(craft.getW()));
            if (fuelRange > 1000) {
                fuelText += ChatColor.GREEN;
            } else if (fuelRange > 100) {
                fuelText += ChatColor.YELLOW;
            } else {
                fuelText += ChatColor.RED;
            }
            fuelText += "Fuel range:";
            fuelText += fuelRange;
            event.setLine(signLine, fuelText);
        }
    }
}