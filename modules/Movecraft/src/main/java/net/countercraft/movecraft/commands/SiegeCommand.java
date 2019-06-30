package net.countercraft.movecraft.commands;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.HashHitBox;
import net.countercraft.movecraft.utils.TopicPaginator;
import net.countercraft.movecraft.warfare.siege.Siege;
import net.countercraft.movecraft.warfare.siege.SiegeManager;
import net.countercraft.movecraft.warfare.siege.SiegeStage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import static net.countercraft.movecraft.utils.ChatUtils.MOVECRAFT_COMMAND_PREFIX;

public class SiegeCommand implements CommandExecutor {
    //TODO: Add tab complete
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("siege")) {
            return false;
        }
        if (!commandSender.hasPermission("movecraft.siege")) {
            commandSender.sendMessage(I18nSupport.getInternationalisedString(MOVECRAFT_COMMAND_PREFIX + "Insufficient Permissions"));
            return true;
        }
        if (args.length == 0) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "No argument specified, valid arguments include begin and list.");
            return true;
        }
        SiegeManager siegeManager = Movecraft.getInstance().getSiegeManager();
        if (siegeManager.getSieges().size() == 0) {
            commandSender.sendMessage(I18nSupport.getInternationalisedString(MOVECRAFT_COMMAND_PREFIX + "Siege is not configured on this server"));
            return true;
        }

        if(args[0].equalsIgnoreCase("list")){
            return listCommand(commandSender, args);
        } else if (args[0].equalsIgnoreCase("begin")) {
            return beginCommand(commandSender);
        }else if(args[0].equalsIgnoreCase("info")){
            return infoCommand(commandSender,args);
        }
        commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "Invalid argument specified, valid arguments include begin, info, and list.");
        return true;

    }

    private boolean infoCommand(CommandSender commandSender, String[] args){
        if(args.length <=1){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "You need to supply a siege.");
            return true;
        }
        String siegeName = String.join(" ", Arrays.copyOfRange(args, 1,args.length));
        Siege siege = null;
        for(Siege searchSiege : Movecraft.getInstance().getSiegeManager().getSieges()){
            if(searchSiege.getName().equalsIgnoreCase(siegeName)){
                siege = searchSiege;
                break;
            }
        }
        if(siege == null){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "Siege not found. Use \"/siege list\" to find valid sieges");
            return true;
        }
        commandSender.sendMessage("" + ChatColor.YELLOW + ChatColor.BOLD  + "----- " + ChatColor.RESET + ChatColor.GOLD + siege.getName() + ChatColor.YELLOW + ChatColor.BOLD +" -----");
        commandSender.sendMessage("Cost to siege: " + ChatColor.RED + currencyFormat.format(siege.getCost()));
        commandSender.sendMessage("Daily income: " + ChatColor.RED + currencyFormat.format(siege.getDailyIncome()));
        commandSender.sendMessage("Day of week: " + ChatColor.RED + dayToString(siege.getDayOfWeek()));
        commandSender.sendMessage("Start time: " + ChatColor.RED + String.format("%02d", siege.getStartTime()/100) + ":" + String.format("%02d", siege.getStartTime()%100) + " UTC");
        commandSender.sendMessage("End time: " + ChatColor.RED + String.format("%02d", siege.getScheduleEnd()/100) + ":" + String.format("%02d",siege.getScheduleEnd()%100) + " UTC");
        commandSender.sendMessage("Start delay: " + ChatColor.RED + String.format("%02d", siege.getDelayBeforeStart()/60) + ":" + String.format("%02d", siege.getDelayBeforeStart()%60));
        commandSender.sendMessage("Duration: " + ChatColor.RED + String.format("%02d", siege.getDuration()/60) + ":" + String.format("%02d", siege.getDuration()%60));
        return true;

    }

    private boolean listCommand(CommandSender commandSender, String[] args){
        SiegeManager siegeManager = Movecraft.getInstance().getSiegeManager();
        int page;
        try {
            if (args.length <= 1)
                page = 1;
            else
                page = Integer.parseInt(args[1]);
        }catch(NumberFormatException e){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + " Invalid page \"" + args[1] + "\"");
            return true;
        }
        TopicPaginator paginator = new TopicPaginator("Sieges");
        for (Siege siege : siegeManager.getSieges()) {
            paginator.addLine("- " + siege.getName());
        }
        if(!paginator.isInBounds(page)){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + " Invalid page \"" + args[1] + "\"");
            return true;
        }
        for(String line : paginator.getPage(page))
            commandSender.sendMessage(line);
        return true;
    }

    private boolean beginCommand(CommandSender commandSender){

        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "you need to be a player to siege");
            return true;
        }
        SiegeManager siegeManager = Movecraft.getInstance().getSiegeManager();
        Player player = (Player) commandSender;

        for (Siege siege : siegeManager.getSieges()) {
            if (siege.getStage().get() != SiegeStage.INACTIVE) {
                player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("A Siege is already taking place"));
                return true;
            }
        }
        Siege siege = null;
        ApplicableRegionSet regions = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getApplicableRegions(player.getLocation());
        search:
        for (ProtectedRegion tRegion : regions.getRegions()) {
            for (Siege tempSiege : siegeManager.getSieges()) {
                if (tRegion.getId().equalsIgnoreCase(tempSiege.getAttackRegion())) {
                    siege = tempSiege;
                    break search;
                }
            }
        }
        if (siege == null) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Could not find a siege configuration for the region you are in"));
            return true;
        }
        long cost = siege.getCost();
        for (Siege tempSiege : siegeManager.getSieges()) {
            ProtectedRegion tRegion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getRegion(tempSiege.getCaptureRegion());
            assert tRegion != null;
            if (tempSiege.isDoubleCostPerOwnedSiegeRegion() && tRegion.getOwners().contains(player.getUniqueId()))
                cost *= 2;
        }

        if (!Movecraft.getInstance().getEconomy().has(player, cost)) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + String.format("You do not have enough money. You need %d", cost));
            return true;
        }

        Calendar rightNow = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        int hour = rightNow.get(Calendar.HOUR_OF_DAY);
        int minute = rightNow.get(Calendar.MINUTE);
        int currMilitaryTime = hour * 100 + minute;
        int dayOfWeek = rightNow.get(Calendar.DAY_OF_WEEK);
        if (currMilitaryTime <= siege.getScheduleStart() || currMilitaryTime >= siege.getScheduleEnd() || dayOfWeek != siege.getDayOfWeek()) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("The time is not during the Siege schedule"));
            return true;
        }

        for (String startCommand : siege.getCommandsOnStart()) {
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), startCommand.replaceAll("%r", siege.getAttackRegion()).replaceAll("%c", "" + siege.getCost()));
        }
        Bukkit.getServer().broadcastMessage(String.format("%s is preparing to siege %s! All players wishing to participate in the defense should head there immediately! Siege will begin in %d minutes"
                , player.getDisplayName(), siege.getName(), siege.getDelayBeforeStart() / 60));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0.25F);
        }
        Movecraft.getInstance().getLogger().log(Level.INFO, String.format("Siege: %s commenced by %s for a cost of %d", siege.getName(), player.getName(), cost));
        Movecraft.getInstance().getEconomy().withdrawPlayer(player, cost);
        siege.setPlayerUUID(player.getUniqueId());
        siege.setStartTime((int)System.currentTimeMillis());
        siege.setStage(SiegeStage.PREPERATION);
        return true;

    }

    private String dayToString(int day){
        String output = "Error";
        switch (day){
            case 1:
                output = "Sunday";
                break;
            case 2:
                output = "Monday";
                break;
            case 3:
                output = "Tuesday";
                break;
            case 4:
                output = "Wednesday";
                break;
            case 5:
                output = "Thursday";
                break;
            case 6:
                output = "Friday";
                break;
            case 7:
                output = "Saturday";
                break;
        }
        return output;
    }
}