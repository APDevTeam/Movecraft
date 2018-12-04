package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.HashHitBox;
import net.countercraft.movecraft.utils.TopicPaginator;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static net.countercraft.movecraft.utils.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class ContactsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("contacts")) {
            return false;
        }
        if(!(commandSender instanceof Player)){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "you need to be a player to get contacts");
            return true;
        }
        Player player = (Player) commandSender;

        if (CraftManager.getInstance().getCraftByPlayer(player) == null) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("You must be piloting a craft"));
            return true;
        }

        int page;
        try {
            if (args.length == 0)
                page = 1;
            else
                page = Integer.parseInt(args[0]);
        }catch(NumberFormatException e){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + " Invalid page \"" + args[0] + "\"");
            return true;
        }

        TopicPaginator pageinator = new TopicPaginator("Contacts");
        Craft ccraft = CraftManager.getInstance().getCraftByPlayer(player);
        HashHitBox hitBox = ccraft.getHitBox();
        long cposx = hitBox.getMaxX() + hitBox.getMinX();
        long cposy = hitBox.getMaxY() + hitBox.getMinY();
        long cposz = hitBox.getMaxZ() + hitBox.getMinZ();
        cposx = cposx >> 1;
        cposy = cposy >> 1;
        cposz = cposz >> 1;
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(ccraft.getW())) {
            HashHitBox tHitBox = tcraft.getHitBox();
            long tposx = tHitBox.getMaxX() + tHitBox.getMinX();
            long tposy = tHitBox.getMaxY() + tHitBox.getMinY();
            long tposz = tHitBox.getMaxZ() + tHitBox.getMinZ();
            tposx = tposx >> 1;
            tposy = tposy >> 1;
            tposz = tposz >> 1;
            long diffx = cposx - tposx;
            long diffy = cposy - tposy;
            long diffz = cposz - tposz;
            long distsquared = diffx * diffx + diffy * diffy + diffz * diffz;
            long detectionRangeSquared = tposy > tcraft.getW().getSeaLevel() ?
                    (long) (tcraft.getOrigBlockCount() * tcraft.getType().getDetectionMultiplier()) :
                    (long) (tcraft.getOrigBlockCount() * tcraft.getType().getUnderwaterDetectionMultiplier());
            if (distsquared < detectionRangeSquared && tcraft.getNotificationPlayer() != ccraft.getNotificationPlayer()) {
                String notification = "Contact: ";
                notification += tcraft.getSinking() ? ChatColor.RED : tcraft.getDisabled() ? ChatColor.BLUE : "";
                notification += tcraft.getUniqueName() != null && tcraft.getUniqueName().length() >= 1 ? tcraft.getUniqueName() + " (" : "";
                notification += tcraft.getType().getCraftName();
                notification += tcraft.getUniqueName() != null && tcraft.getUniqueName().length() >= 1 ? ")" : "";
                notification += ChatColor.RESET;
                notification += " commanded by ";
                notification += tcraft.getNotificationPlayer() != null ? tcraft.getNotificationPlayer().getDisplayName() : "null";
                notification += ", size: ";
                notification += tcraft.getOrigBlockCount();
                notification += ", range: ";
                notification += (int) Math.sqrt(distsquared);
                notification += " to the";
                if (Math.abs(diffx) > Math.abs(diffz))
                    if (diffx < 0)
                        notification += " east.";
                    else
                        notification += " west.";
                else if (diffz < 0)
                    notification += " south.";
                else
                    notification += " north.";
                pageinator.addLine(notification);
            }
        }
        if (pageinator.isEmpty()) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("No contacts within range"));
            return true;
        }
        if(!pageinator.isInBounds(page)){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + " Invalid page \"" + args[1] + "\"");
            return true;
        }
        for(String line :pageinator.getPage(page))
            commandSender.sendMessage(line);
        return true;
    }
}
