package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.utils.ChatUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.NotNull;

public final class NameSign implements Listener {

    private static final String NAME_SIGN_IDENTIFIER = "Name:"; //String on first line which will identify the block as a name sign

    /**
     * When a craft is piloted, check the craft for a name sign
     */
    @EventHandler
    public void onCraftDetect(@NotNull CraftDetectEvent event) {
        Craft craft = event.getCraft();

        //Ensure nonnull player and sufficient permissions
        if (craft.getNotificationPlayer() == null || (Settings.RequireNamePerm && !craft.getNotificationPlayer().hasPermission("movecraft.name.use"))) {
            return;
        }

        World craftWorld = craft.getW();
        for (MovecraftLocation location : craft.getHitBox()) { //Iterate through every craft block to search for name sign
            Block b = location.toBukkit(craftWorld).getBlock();
            if (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN) { //If block is a sign
                Sign sign = (Sign) b.getState();
                String craftNameFromSign = "";

                if (sign.getLine(0).equalsIgnoreCase(NAME_SIGN_IDENTIFIER)) { //If name sign is found
                    for (int i = 1; i <= 3; i++) { //Go through each line below the NAME_SIGN_IDENTIFIER string
                        if (!sign.getLine(i).isEmpty()) { //If the line is not empty
                            craftNameFromSign += sign.getLine(i) + " "; //Add the line to the name, with a space at the end
                        }
                    }
                    craft.setName(craftNameFromSign.isEmpty() ? "" : craftNameFromSign.substring(0, craftNameFromSign.length() - 1)); //Set craft name if the sign wasn't blank, while removing the last space
                    return;
                }
            }
        }
    }

    /**
     * When player is creating a new Name Sign
     */
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.getLine(0).equalsIgnoreCase(NAME_SIGN_IDENTIFIER)) { //If player is placing a Name Sign
            if (Settings.RequireNamePerm && !event.getPlayer().hasPermission("movecraft.name.place")) { //If they don't have permission
                event.getPlayer().sendMessage(ChatUtils.MOVECRAFT_COMMAND_PREFIX + "Insufficient permissions"); //Send error message
                event.setCancelled(true); //Disallow placing of sign
            }
        }
    }
}
