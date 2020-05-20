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

public class ContactsSign implements Listener{

    @EventHandler
    public void onCraftDetect(CraftDetectEvent event){
        World world = event.getCraft().getW();
        for(MovecraftLocation location: event.getCraft().getHitBox()){
            Block block = location.toBukkit(world).getBlock();
            if(block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST){
                Sign sign = (Sign) block.getState();
                if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Contacts:")) {
                    sign.setLine(1, "");
                    sign.setLine(2, "");
                    sign.setLine(3, "");
                    sign.update();
                }
            }
        }
    }

    @EventHandler
    public final void onSignTranslateEvent(SignTranslateEvent event){
        String[] lines = event.getLines();
        Craft craft = event.getCraft();
        if (!ChatColor.stripColor(lines[0]).equalsIgnoreCase("Contacts:")) {
            return;
        }
        int signLine=1;
        for(Craft tcraft : craft.getContacts()) {
            MovecraftLocation center = craft.getHitBox().getMidPoint();
            MovecraftLocation tcenter = tcraft.getHitBox().getMidPoint();
            int distsquared= center.distanceSquared(tcenter);
            // craft has been detected
            String notification = ChatColor.BLUE + tcraft.getType().getCraftName();
            if(notification.length()>9) {
                notification = notification.substring(0, 7);
            }
            notification += " " + (int)Math.sqrt(distsquared);
            int diffx=center.getX() - tcenter.getX();
            int diffz=center.getZ() - tcenter.getZ();
            if(Math.abs(diffx) > Math.abs(diffz)) {
                if(diffx<0) {
                    notification+=" E";
                } else {
                    notification+=" W";
                }
            } else {
                if(diffz<0) {
                    notification+=" S";
                } else {
                    notification+=" N";
                }
            }
            lines[signLine++] = notification;
            if (signLine >= 4) {
                break;
            }

        }
        if(signLine<4) {
            for(int i=signLine; i<4; i++) {
                lines[signLine]="";
            }
        }
    }


}
