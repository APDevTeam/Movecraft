package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.Tags;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public final class StatusSign implements Listener{

    @EventHandler
    public void onCraftDetect(CraftDetectEvent event){
        World world = event.getCraft().getWorld();
        for(MovecraftLocation location: event.getCraft().getHitBox()){
            var block = location.toBukkit(world).getBlock();
            if(!Tag.SIGNS.isTagged(block.getType())){
                continue;
            }
            BlockState state = block.getState();
            if(state instanceof Sign){
                Sign sign = (Sign) state;
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
        int fuel=0;
        int totalBlocks=0;
        Counter<Material> foundBlocks = new Counter<>();

        var v = craft.getType().getObjectProperty(CraftType.FUEL_TYPES);
        if(!(v instanceof Map<?, ?>))
            throw new IllegalStateException("FUEL_TYPES must be of type Map");
        var fuelTypes = (Map<?, ?>) v;
        for(var e : fuelTypes.entrySet()) {
            if(!(e.getKey() instanceof Material))
                throw new IllegalStateException("Keys in FUEL_TYPES must be of type Material");
            if(!(e.getValue() instanceof Double))
                throw new IllegalStateException("Values in FUEL_TYPES must be of type Double");
        }

        for (MovecraftLocation ml : craft.getHitBox()) {
            Material material = craft.getWorld().getBlockAt(ml.getX(), ml.getY(), ml.getZ()).getType();
            foundBlocks.add(material);

            if(Tags.FURNACES.contains(material)) {
                InventoryHolder inventoryHolder = (InventoryHolder) craft.getWorld().getBlockAt(ml.getX(), ml.getY(), ml.getZ()).getState();
                for (ItemStack iStack : inventoryHolder.getInventory()) {
                    if (iStack == null || !fuelTypes.containsKey(iStack.getType()))
                        continue;
                    fuel += iStack.getAmount() * (double) fuelTypes.get(iStack.getType());
                }
            }
            if(!material.isAir() && material != Material.FIRE) {
                totalBlocks++;
            }
        }
        int signLine=1;
        int signColumn=0;
        /*  TODO: Implement new system for flyblocks on status signs
        for(EnumSet<Material> alFlyBlockID : craft.getType().getFlyBlocks().keySet()) {
            Material flyBlockID= alFlyBlockID.get(0);
            double minimum=craft.getType().getFlyBlocks().get(alFlyBlockID).get(0);
            if(foundBlocks.get(flyBlockID) != 0 && minimum>0) { // if it has a minimum, it should be considered for sinking consideration
                int amount=foundBlocks.get((flyBlockID));
                double percentPresent= (amount*100D/totalBlocks);
                String signText="";
                if(percentPresent>minimum*1.04) {
                    signText+= ChatColor.GREEN;
                } else if(percentPresent>minimum*1.02) {
                    signText+=ChatColor.YELLOW;
                } else {
                    signText+=ChatColor.RED;
                }
                if(flyBlockID == Material.REDSTONE_BLOCK) {
                    signText+="R";
                } else if(flyBlockID == Material.IRON_BLOCK) {
                    signText+="I";
                } else {
                    signText+= flyBlockID.toString().charAt(0);
                }

                signText+=" ";
                signText+=  (int) percentPresent;
                signText+="/";
                signText+= (int) minimum;
                signText+="  ";
                if(signColumn==0) {
                    event.setLine(signLine,signText);
                    signColumn++;
                } else if(signLine<3) {
                    String existingLine = event.getLine(signLine);
                    existingLine += signText;
                    event.setLine(signLine, existingLine);
                    signLine++;
                    signColumn=0;
                }
            }
        }
        if (signLine < 3 && signColumn == 1){
            signLine++;
        }
        */
        String fuelText="";
        int cruiseSkipBlocks = (int) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_CRUISE_SKIP_BLOCKS, craft.getWorld());
        cruiseSkipBlocks++;
        double fuelBurnRate = (double) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_FUEL_BURN_RATE, craft.getWorld());
        int fuelRange= (int) Math.round((fuel * (1 + cruiseSkipBlocks)) / fuelBurnRate);
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