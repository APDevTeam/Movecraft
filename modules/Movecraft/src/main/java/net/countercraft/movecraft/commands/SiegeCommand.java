package net.countercraft.movecraft.commands;

import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
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

import javax.annotation.Nullable;
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
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }
        if (args.length == 0) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Siege - No Argument"));
            return true;
        }
        SiegeManager siegeManager = Movecraft.getInstance().getSiegeManager();
        if (siegeManager.getSieges().size() == 0) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Siege - Siege Not Configured"));
            return true;
        }

        if(args[0].equalsIgnoreCase("list")){
            return listCommand(commandSender, args);
        } else if (args[0].equalsIgnoreCase("begin")) {
            return beginCommand(commandSender);
        }else if(args[0].equalsIgnoreCase("info")){
            return infoCommand(commandSender,args);
        }
        commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Siege - Invalid Argument"));
        return true;

    }

    private boolean infoCommand(CommandSender commandSender, String[] args){
        if(args.length <=1){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Siege - Specify Region"));
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
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Siege - Siege Region Not Found"));
            return true;
        }
        commandSender.sendMessage("" + ChatColor.YELLOW + ChatColor.BOLD  + "----- " + ChatColor.RESET + ChatColor.GOLD + siege.getName() + ChatColor.YELLOW + ChatColor.BOLD +" -----");
        commandSender.sendMessage(I18nSupport.getInternationalisedString("Siege - Siege Cost") + ChatColor.RED + currencyFormat.format(siege.getCost()));
        commandSender.sendMessage(I18nSupport.getInternationalisedString("Siege - Daily Income") + ChatColor.RED + currencyFormat.format(siege.getDailyIncome()));
        commandSender.sendMessage(I18nSupport.getInternationalisedString("Siege - Day of Week") + ChatColor.RED + dayToString(siege.getDayOfWeek()));
        commandSender.sendMessage(I18nSupport.getInternationalisedString("Siege - Start Time") + ChatColor.RED + militaryTimeIntToString(siege.getScheduleStart()) + " UTC");
        commandSender.sendMessage(I18nSupport.getInternationalisedString("Siege - End Time") + ChatColor.RED + militaryTimeIntToString(siege.getScheduleEnd()) + " UTC");
        commandSender.sendMessage(I18nSupport.getInternationalisedString("Siege - Duration") + ChatColor.RED + secondsIntToString(siege.getDuration()));
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
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Invalid Page") +" \"" + args[1] + "\"");
            return true;
        }
        TopicPaginator paginator = new TopicPaginator("Sieges");
        for (Siege siege : siegeManager.getSieges()) {
            paginator.addLine("- " + siege.getName());
        }
        if(!paginator.isInBounds(page)){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Invalid Page") +" \"" + args[1] + "\"");
            return true;
        }
        for(String line : paginator.getPage(page))
            commandSender.sendMessage(line);
        return true;
    }

    private boolean beginCommand(CommandSender commandSender){

        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Siege - Must Be Player"));
            return true;
        }
        SiegeManager siegeManager = Movecraft.getInstance().getSiegeManager();
        Player player = (Player) commandSender;

        for (Siege siege : siegeManager.getSieges()) {
            if (siege.getStage().get() != SiegeStage.INACTIVE) {
                player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Siege - Siege Already Underway"));
                return true;
            }
        }
        Siege siege = getSiege(player, siegeManager);

        if (siege == null) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Siege - No Configuration Found"));
            return true;
        }
        long cost = calcSiegeCost(siege, siegeManager, player);

        if (!Movecraft.getInstance().getEconomy().has(player, cost)) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + String.format(I18nSupport.getInternationalisedString("Siege - Insufficient Funds"),cost));
            return true;
        }

        int currMilitaryTime = getMilitaryTime();
        int dayOfWeek = getDayOfWeek();
        if (currMilitaryTime <= siege.getScheduleStart() || currMilitaryTime >= siege.getScheduleEnd() || dayOfWeek != siege.getDayOfWeek()) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Siege - Time Not During Schedule"));
            return true;
        }

        //check if piloting craft in siege region
        Craft siegeCraft = CraftManager.getInstance().getCraftByPlayer(player);
        if(siegeCraft == null) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("You must be piloting a craft!"));
            return true;
        }
        if(!siege.getCraftsToWin().contains(siegeCraft.getType().getCraftName())) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("You must be piloting a craft that can siege!"));
            return true;
        }
        MovecraftLocation mid = siegeCraft.getHitBox().getMidPoint();
        if(!Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getRegion(siege.getAttackRegion()).contains(mid.getX(), mid.getY(), mid.getZ())) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("You must be piloting a craft in the siege region!"));
            return true;
        }


        startSiege(siege, player, cost);
        return true;
    }

    private void startSiege(Siege siege, Player player, long cost) {
        for (String startCommand : siege.getCommandsOnStart()) {
            Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), startCommand.replaceAll("%r", siege.getAttackRegion()).replaceAll("%c", "" + siege.getCost()));
        }
        Bukkit.getServer().broadcastMessage(String.format(I18nSupport.getInternationalisedString("Siege - Siege About To Begin")
                , player.getDisplayName(), siege.getName(), siege.getDelayBeforeStart() / 60));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0.25F);
        }
        Movecraft.getInstance().getLogger().log(Level.INFO, String.format(I18nSupport.getInternationalisedString("Siege - Log Siege Start"), siege.getName(), player.getName(), cost));
        Movecraft.getInstance().getEconomy().withdrawPlayer(player, cost);
        siege.setPlayerUUID(player.getUniqueId());
        siege.setStartTime((int)System.currentTimeMillis());
        siege.setStage(SiegeStage.PREPERATION);
    }

    private int getMilitaryTime() {
        Calendar rightNow = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        int hour = rightNow.get(Calendar.HOUR_OF_DAY);
        int minute = rightNow.get(Calendar.MINUTE);
        return hour * 100 + minute;
    }

    private int getDayOfWeek() {
        Calendar rightNow = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        return rightNow.get(Calendar.DAY_OF_WEEK);
    }

    @Nullable
    private Siege getSiege(Player player, SiegeManager siegeManager) {
        ApplicableRegionSet regions = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getApplicableRegions(player.getLocation());
        for (ProtectedRegion tRegion : regions.getRegions()) {
            for (Siege tempSiege : siegeManager.getSieges()) {
                if (tRegion.getId().equalsIgnoreCase(tempSiege.getAttackRegion())) {
                    return tempSiege;
                }
            }
        }
        return null;
    }

    private long calcSiegeCost(Siege siege, SiegeManager siegeManager, Player player) {
        long cost = siege.getCost();
        for (Siege tempSiege : siegeManager.getSieges()) {
            ProtectedRegion tRegion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getRegion(tempSiege.getCaptureRegion());
            assert tRegion != null;
            if (tempSiege.isDoubleCostPerOwnedSiegeRegion() && tRegion.getOwners().contains(player.getUniqueId()))
                cost *= 2;
        }
        return cost;
    }

    private String militaryTimeIntToString(int militaryTime) {
        return String.format("%02d", militaryTime / 100) + ":" + String.format("%02d",militaryTime % 100);

    }

    private String secondsIntToString(int seconds) {
        return String.format("%02d", seconds / 60) + ":" + String.format("%02d",seconds % 60);
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
        output = I18nSupport.getInternationalisedString(output);
        return output;
    }
}
