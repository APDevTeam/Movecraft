package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.MovecraftLocation;
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
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Contacts - Must Be Player"));
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
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Paginator - Invalid Page") +" \"" + args[0] + "\"");
            return true;
        }

        TopicPaginator pageinator = new TopicPaginator(I18nSupport.getInternationalisedString("Contacts"));
        Craft ccraft = CraftManager.getInstance().getCraftByPlayer(player);
        HashHitBox hitBox = ccraft.getHitBox();
        MovecraftLocation center = hitBox.getMidPoint();
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(ccraft.getW())) {
            HashHitBox tHitBox = tcraft.getHitBox();
            if (tHitBox.isEmpty())
                continue;
            MovecraftLocation tCenter = tHitBox.getMidPoint();

            int distsquared = center.distanceSquared(tCenter);
            int detectionRangeSquared = (int) (tCenter.getY() > tcraft.getType().getStaticWaterLevel() ?
                                (Math.sqrt(tcraft.getOrigBlockCount()) * tcraft.getType().getDetectionMultiplier()) :
                                (Math.sqrt(tcraft.getOrigBlockCount()) * tcraft.getType().getUnderwaterDetectionMultiplier()));
            if (distsquared < detectionRangeSquared * detectionRangeSquared && tcraft.getNotificationPlayer() != ccraft.getNotificationPlayer()) {
                String notification = I18nSupport.getInternationalisedString("Contact");
                notification += ": ";
                notification += tcraft.getSinking() ? ChatColor.RED : tcraft.getDisabled() ? ChatColor.BLUE : "";
                notification += tcraft.getName().length() >= 1 ? tcraft.getName() + " (" : "";
                notification += tcraft.getType().getCraftName();
                notification += tcraft.getName().length() >= 1 ? ") " : " ";
                notification += ChatColor.RESET;
                notification += I18nSupport.getInternationalisedString("Contact - Commanded By") + ", ";
                notification += tcraft.getNotificationPlayer() != null ? tcraft.getNotificationPlayer().getDisplayName() : "null";
                notification += " ";
                notification += I18nSupport.getInternationalisedString("Contact - Size")+ " ";
                notification += tcraft.getOrigBlockCount();
                notification += ", "+I18nSupport.getInternationalisedString("Contact - Range")+" ";
                notification += (int) Math.sqrt(distsquared);
                notification += " "+I18nSupport.getInternationalisedString("Contact - To The");
                int diffx = center.getX() - tCenter.getX();
                int diffz = center.getZ() - tCenter.getZ();
                if (Math.abs(diffx) > Math.abs(diffz))
                    if (diffx < 0)
                        notification += " "+I18nSupport.getInternationalisedString("Contact/Subcraft Rotate - East") + ".";
                    else
                        notification += " "+I18nSupport.getInternationalisedString("Contact/Subcraft Rotate - West") + ".";
                else if (diffz < 0)
                    notification += " "+I18nSupport.getInternationalisedString("Contact/Subcraft Rotate - South") + ".";
                else
                    notification += " "+I18nSupport.getInternationalisedString("Contact/Subcraft Rotate - North") + ".";
                pageinator.addLine(notification);
            }
        }
        if (pageinator.isEmpty()) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Contacts - None Found"));
            return true;
        }
        if(!pageinator.isInBounds(page)){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Paginator - Invalid page") + "\"" + args[1] + "\"");
            return true;
        }
        for(String line :pageinator.getPage(page))
            commandSender.sendMessage(line);
        return true;
    }
}
