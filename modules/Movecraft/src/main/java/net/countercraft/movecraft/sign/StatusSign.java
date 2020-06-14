package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StatusSign implements Listener{

    @EventHandler
    public void onCraftDetect(CraftDetectEvent event){
        World world = event.getCraft().getW();
        for(MovecraftLocation location: event.getCraft().getHitBox()){
            Block block = location.toBukkit(world).getBlock();
            if(block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST){
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
        int fuel=0;
        int totalBlocks=0;
        Map<Integer, Integer> foundBlocks = new HashMap<>();
        for (MovecraftLocation ml : craft.getHitBox()) {
            Integer blockID = craft.getW().getBlockAt(ml.getX(), ml.getY(), ml.getZ()).getTypeId();

            if (foundBlocks.containsKey(blockID)) {
                Integer count = foundBlocks.get(blockID);
                if (count == null) {
                    foundBlocks.put(blockID, 1);
                } else {
                    foundBlocks.put(blockID, count + 1);
                }
            } else {
                foundBlocks.put(blockID, 1);
            }

            if (blockID == 61) {
                InventoryHolder inventoryHolder = (InventoryHolder) craft.getW().getBlockAt(ml.getX(), ml.getY(), ml.getZ()).getState();
                if (inventoryHolder.getInventory().contains(263)
                        || inventoryHolder.getInventory().contains(173)) {
                    ItemStack[] istack=inventoryHolder.getInventory().getContents();
                    for(ItemStack i : istack) {
                        if(i!=null) {
                            if(i.getTypeId()==263) {
                                fuel+=i.getAmount()*8;
                            }
                            if(i.getTypeId()==173) {
                                fuel+=i.getAmount()*80;
                            }
                        }
                    }
                }
            }
            if (blockID != 0) {
                totalBlocks++;
            }
        }
        int signLine=1;
        int signColumn=0;
        for(List<Integer> alFlyBlockID : craft.getType().getFlyBlocks().keySet()) {
            int flyBlockID=alFlyBlockID.get(0);
            Double minimum=craft.getType().getFlyBlocks().get(alFlyBlockID).get(0);
            if(foundBlocks.containsKey(flyBlockID) && minimum>0) { // if it has a minimum, it should be considered for sinking consideration
                int amount=foundBlocks.get(flyBlockID);
                Double percentPresent=(double) (amount*100/totalBlocks);
                int deshiftedID=flyBlockID;
                if(deshiftedID>10000) {
                    deshiftedID=(deshiftedID-10000)>>4;
                }
                String signText="";
                if(percentPresent>minimum*1.04) {
                    signText+= ChatColor.GREEN;
                } else if(percentPresent>minimum*1.02) {
                    signText+=ChatColor.YELLOW;
                } else {
                    signText+=ChatColor.RED;
                }
                if(deshiftedID==152) {
                    signText+="R";
                } else if(deshiftedID==42) {
                    signText+="I";
                } else {
                    signText+= Material.getMaterial(deshiftedID).toString().charAt(0);
                }

                signText+=" ";
                signText+=percentPresent.intValue();
                signText+="/";
                signText+=minimum.intValue();
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
        String fuelText="";
        int fuelRange=(int) ((fuel*(1+(craft.getType().getCruiseSkipBlocks(craft.getW())+1)))/craft.getType().getFuelBurnRate(craft.getW()));
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