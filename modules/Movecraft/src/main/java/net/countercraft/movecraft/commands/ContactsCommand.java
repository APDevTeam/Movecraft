package net.countercraft.movecraft.commands;

import net.countercraft.movecraft.api.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.update.BlockTranslateCommand;
import net.countercraft.movecraft.utils.TopicPaginator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("deprecation")
public class ContactsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("contacts")) {
            return false;
        }
        if(!(commandSender instanceof Player)){
            commandSender.sendMessage("you need to be a player to get contacts");
            return true;
        }
        Player player = (Player) commandSender;

        if (CraftManager.getInstance().getCraftByPlayer(player) == null) {
            player.sendMessage(I18nSupport.getInternationalisedString("You must be piloting a craft"));
            return true;
        }

        int page;
        try {
            if (args.length == 0)
                page = 1;
            else
                page = Integer.parseInt(args[1]);
        }catch(NumberFormatException e){
            commandSender.sendMessage(" Invalid page \"" + args[1] + "\"");
            return true;
        }

        TopicPaginator pageinator = new TopicPaginator("Contacts");
        Craft ccraft = CraftManager.getInstance().getCraftByPlayer(player);
        long cposx = ccraft.getMaxX() + ccraft.getMinX();
        long cposy = ccraft.getMaxY() + ccraft.getMinY();
        long cposz = ccraft.getMaxZ() + ccraft.getMinZ();
        cposx = cposx >> 1;
        cposy = cposy >> 1;
        cposz = cposz >> 1;
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(ccraft.getW())) {
            long tposx = tcraft.getMaxX() + tcraft.getMinX();
            long tposy = tcraft.getMaxY() + tcraft.getMinY();
            long tposz = tcraft.getMaxZ() + tcraft.getMinZ();
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
                notification += tcraft.getType().getCraftName();
                notification += " commanded by ";
                notification += tcraft.getNotificationPlayer().getDisplayName();
                notification += ", name:";
                notification += 
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
            player.sendMessage(I18nSupport.getInternationalisedString("No contacts within range"));
            return true;
        }

        for(String line :pageinator.getPage(page))
            commandSender.sendMessage(line);
        return true;
    }
}
