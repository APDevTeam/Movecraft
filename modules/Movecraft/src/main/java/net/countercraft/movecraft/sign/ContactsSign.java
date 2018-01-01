package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.api.craft.Craft;
import net.countercraft.movecraft.api.events.SignTranslateEvent;
import net.countercraft.movecraft.craft.CraftManager;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ContactsSign implements Listener{

    @EventHandler
    public final void onSignTranslateEvent(SignTranslateEvent event){
        String[] lines = event.getLines();
        Craft craft = event.getCraft();
        if (!lines[0].equalsIgnoreCase("Contacts:")) {
            return;
        }
        boolean foundContact=false;
        int signLine=1;
        for(Craft tcraft : CraftManager.getInstance().getCraftsInWorld(craft.getW())) {
            long cposx=craft.getMaxX()+craft.getMinX();
            long cposy=craft.getMaxY()+craft.getMinY();
            long cposz=craft.getMaxZ()+craft.getMinZ();
            cposx=cposx>>1;
            cposy=cposy>>1;
            cposz=cposz>>1;
            long tposx=tcraft.getMaxX()+tcraft.getMinX();
            long tposy=tcraft.getMaxY()+tcraft.getMinY();
            long tposz=tcraft.getMaxZ()+tcraft.getMinZ();
            tposx=tposx>>1;
            tposy=tposy>>1;
            tposz=tposz>>1;
            long diffx=cposx-tposx;
            long diffy=cposy-tposy;
            long diffz=cposz-tposz;
            long distsquared= diffx * diffx;
            distsquared+= diffy * diffy;
            distsquared+= diffz * diffz;
            long detectionRange=0;
            if(tposy>tcraft.getW().getSeaLevel()) {
                detectionRange=(long) (Math.sqrt(tcraft.getOrigBlockCount())*tcraft.getType().getDetectionMultiplier());
            } else {
                detectionRange=(long) (Math.sqrt(tcraft.getOrigBlockCount())*tcraft.getType().getUnderwaterDetectionMultiplier());
            }
            if(distsquared<detectionRange*detectionRange && tcraft.getNotificationPlayer()!=craft.getNotificationPlayer()) {
                // craft has been detected
                foundContact=true;
                String notification = ChatColor.BLUE + tcraft.getType().getCraftName();
                if(notification.length()>9) {
                    notification = notification.substring(0, 7);
                }
                notification += " " + (int)Math.sqrt(distsquared);
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
                if (signLine > 4) {
                    break;
                }
            }
        }
        if(signLine<4) {
            for(int i=signLine; i<4; i++) {
                lines[signLine]="";
            }
        }
    }


}
