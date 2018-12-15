package net.countercraft.movecraft.commands;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.warfare.assault.Assault;
import net.countercraft.movecraft.warfare.assault.AssaultBarTask;
import net.countercraft.movecraft.warfare.assault.AssaultUtils;
import net.countercraft.movecraft.warfare.siege.Siege;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Map;

import static net.countercraft.movecraft.utils.ChatUtils.MOVECRAFT_COMMAND_PREFIX;
import static net.countercraft.movecraft.warfare.assault.AssaultUtils.getCostToAssault;

public class AssaultCommand implements CommandExecutor{
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (!command.getName().equalsIgnoreCase("assault")) {
            return false;
        }
        if (!Settings.AssaultEnable) {
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Assault is not enabled, you shouldn't even see this"));
            return true;
        }
        if(!(commandSender instanceof Player)){
            commandSender.sendMessage(MOVECRAFT_COMMAND_PREFIX + "you need to be a player to get assault info");
            return true;
        }
        Player player = (Player) commandSender;

        if (!player.hasPermission("movecraft.assault")) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("No region specified"));
            return true;
        }
        ProtectedRegion aRegion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getRegion(args[0]);
        if (aRegion == null) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Region not found"));
            return true;
        }
        LocalPlayer lp = Movecraft.getInstance().getWorldGuardPlugin().wrapPlayer(player);
        Map<String, ProtectedRegion> allRegions = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getRegions();
        boolean foundOwnedRegion = false;
        for (ProtectedRegion iRegion : allRegions.values()) {
            if (iRegion.isOwner(lp) && iRegion.getFlag(DefaultFlag.TNT) == StateFlag.State.DENY) {
                foundOwnedRegion = true;
                break;
            }
        }
        if (!foundOwnedRegion) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("You are not the owner of any assaultable region and can not assault others"));
            return true;
        }
        boolean canBeAssaulted = true;
        for (Siege siege : Movecraft.getInstance().getSiegeManager().getSieges()) {
            // siegable regions can not be assaulted
            if (aRegion.getId().equalsIgnoreCase(siege.getAttackRegion()) || aRegion.getId().equalsIgnoreCase(siege.getCaptureRegion())) {
                canBeAssaulted = false;
                break;
            }
        }
        // a region can only be assaulted if it disables TNT, this is to prevent child regions or sub regions from being assaulted
        // regions with no owners can not be assaulted
        if (aRegion.getFlag(DefaultFlag.TNT) != StateFlag.State.DENY || aRegion.getOwners().size() == 0)
            canBeAssaulted = false;

        {
            Assault assault = null;
            for (Assault tempAssault : Movecraft.getInstance().getAssaultManager().getAssaults()) {
                if (tempAssault.getRegionName().equals(aRegion.getId())) {
                    assault = tempAssault;
                    break;
                }
            }
            if (assault != null) {
                long startTime = assault.getStartTime();
                long curtime = System.currentTimeMillis();
                if (curtime - startTime < Settings.AssaultCooldownHours * (60 * 60 * 1000)) {
                    canBeAssaulted = false;
                }
            }
        }
        if (!AssaultUtils.areDefendersOnline(aRegion)) {
            canBeAssaulted = false;
        }
        if (aRegion.isMember(lp)) {
            canBeAssaulted = false;
        }
        if (!canBeAssaulted) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("You can not assault this region, check AssaultInfo"));
            return true;
        }
        OfflinePlayer offP = Bukkit.getOfflinePlayer(player.getUniqueId());
        if (Movecraft.getInstance().getEconomy().getBalance(offP) < getCostToAssault(aRegion)) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("You can not afford to assault this region"));
            return true;
        }
//			if(aRegion.getType() instanceof ProtectedCuboidRegion) { // Originally I wasn't going to do non-cubes, but we'll try it and see how it goes. In theory it may repair more than it should but... meh...
        String repairStateName = Movecraft.getInstance().getDataFolder().getAbsolutePath() + "/RegionRepairStates";
        File file = new File(repairStateName);
        if (!file.exists()) {
            file.mkdirs();
        }
        repairStateName += "/";
        repairStateName += aRegion.getId().replaceAll("\\s+", "_");
        repairStateName += ".schematic";
        file = new File(repairStateName);

        Vector min = new Vector(aRegion.getMinimumPoint().getBlockX(), aRegion.getMinimumPoint().getBlockY(), aRegion.getMinimumPoint().getBlockZ());
        Vector max = new Vector(aRegion.getMaximumPoint().getBlockX(), aRegion.getMaximumPoint().getBlockY(), aRegion.getMaximumPoint().getBlockZ());

        if (max.subtract(min).getBlockX() > 256) {
            if (min.getBlockX() < player.getLocation().getBlockX() - 128) {
                min = min.setX(player.getLocation().getBlockX() - 128);
            }
            if (max.getBlockX() > player.getLocation().getBlockX() + 128) {
                max = max.setX(player.getLocation().getBlockX() + 128);
            }
        }
        if (max.subtract(min).getBlockZ() > 256) {
            if (min.getBlockZ() < player.getLocation().getBlockZ() - 128) {
                min = min.setZ(player.getLocation().getBlockZ() - 128);
            }
            if (max.getBlockZ() > player.getLocation().getBlockZ() + 128) {
                max = max.setZ(player.getLocation().getBlockZ() + 128);
            }
        }

        CuboidClipboard clipboard = new CuboidClipboard(max.subtract(min).add(Vector.ONE), min);
        CuboidSelection selection = new CuboidSelection(player.getWorld(), min, max);

        for (int x = 0; x < selection.getWidth(); ++x) {
            for (int y = 0; y < selection.getHeight(); ++y) {
                for (int z = 0; z < selection.getLength(); ++z) {
                    Vector vector = new Vector(x, y, z);
                    int bx = selection.getMinimumPoint().getBlockX() + x;
                    int by = selection.getMinimumPoint().getBlockY() + y;
                    int bz = selection.getMinimumPoint().getBlockZ() + z;
                    Block block = player.getWorld().getBlockAt(bx, by, bz);
                    if (!player.getWorld().isChunkLoaded(bx >> 4, bz >> 4))
                        player.getWorld().loadChunk(bx >> 4, bz >> 4);
                    BaseBlock baseBlock = new BaseBlock(block.getTypeId(), block.getData());

                    clipboard.setBlock(vector, baseBlock);
                }
            }
        }
        try {
            clipboard.saveSchematic(file);
        } catch (Exception e) {
            player.sendMessage(MOVECRAFT_COMMAND_PREFIX + I18nSupport.getInternationalisedString("Could not save file"));
            e.printStackTrace();
            return true;
        }
//			} else {
//				player.sendMessage( String.format( I18nSupport.getInternationalisedString( "This region is not a cuboid - see an admin" ) ) );
//				return true;
//			}
        Movecraft.getInstance().getEconomy().withdrawPlayer(offP, getCostToAssault(aRegion));
        Bukkit.getServer().broadcastMessage(String.format("%s is preparing to assault %s! All players wishing to participate in the defense should head there immediately! Assault will begin in %d minutes"
                , player.getDisplayName(), args[0], Settings.AssaultDelay / 60));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, (float) 0.25);
        }
        new AssaultBarTask(Movecraft.getInstance() ,args[0] ,System.currentTimeMillis()).runTaskTimer(Movecraft.getInstance(), 0, 20);
        final String taskAssaultName = args[0];
        final Player taskPlayer = player;
        final World taskWorld = player.getWorld();
        final Long taskMaxDamages = (long) AssaultUtils.getMaxDamages(aRegion);
        final Vector taskMin = min;
        final Vector taskMax = max;
        //TODO: Make async
        new BukkitRunnable() {
            @Override
            public void run() {
                //progressBar.setVisible(false);
                Bukkit.getServer().broadcastMessage(String.format("The Assault of %s has commenced! The assault leader is %s. Destroy the enemy region!"
                        , taskAssaultName, taskPlayer.getDisplayName()));
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0.25F);
                }
                Movecraft.getInstance().getAssaultManager().getAssaults().add(new Assault(taskAssaultName, taskPlayer, taskWorld, System.currentTimeMillis(), taskMaxDamages, taskMin, taskMax));
                ProtectedRegion tRegion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(taskWorld).getRegion(taskAssaultName);
                tRegion.setFlag(DefaultFlag.TNT, StateFlag.State.ALLOW);
            }
        }.runTaskLater(Movecraft.getInstance(), (20 * Settings.AssaultDelay));

        return true;


    }
}
