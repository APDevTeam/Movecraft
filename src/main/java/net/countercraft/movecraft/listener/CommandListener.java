/*
 * This file is part of Movecraft.
 *
 *     Movecraft is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Movecraft is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Movecraft.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.countercraft.movecraft.listener;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.MathUtils;
import net.countercraft.movecraft.utils.MovecraftLocation;
import net.countercraft.movecraft.utils.Rotation;
import net.countercraft.movecraft.warfare.assault.Assault;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;

//public class CommandListener implements Listener {
public class CommandListener implements CommandExecutor {

    private CraftType getCraftTypeFromString(String s) {
        for (CraftType t : CraftManager.getInstance().getCraftTypes()) {
            if (s.equalsIgnoreCase(t.getCraftName())) {
                return t;
            }
        }

        return null;
    }

    private Location getCraftTeleportPoint(Craft craft, World w) {
        int maxDX = 0;
        int maxDZ = 0;
        int maxY = 0;
        int minY = 32767;
        for (int[][] i1 : craft.getHitBox()) {
            maxDX++;
            if (i1 != null) {
                int indexZ = 0;
                for (int[] i2 : i1) {
                    indexZ++;
                    if (i2 != null) {
                        if (i2[0] < minY) {
                            minY = i2[0];
                        }
                    }
                    if (i2 != null) {
                        if (i2[1] > maxY) {
                            maxY = i2[1];
                        }
                    }
                }
                if (indexZ > maxDZ) {
                    maxDZ = indexZ;
                }

            }
        }
        double telX = craft.getMinX() + (maxDX / 2.0);
        double telZ = craft.getMinZ() + (maxDZ / 2.0);
        double telY = maxY + 1.0;
        Location telPoint = new Location(w, telX, telY, telZ);
        return telPoint;
    }

    private MovecraftLocation getCraftMidPoint(Craft craft) {
        int maxDX = 0;
        int maxDZ = 0;
        int maxY = 0;
        int minY = 32767;
        for (int[][] i1 : craft.getHitBox()) {
            maxDX++;
            if (i1 != null) {
                int indexZ = 0;
                for (int[] i2 : i1) {
                    indexZ++;
                    if (i2 != null) {
                        if (i2[0] < minY) {
                            minY = i2[0];
                        }
                    }
                    if (i2 != null) {
                        if (i2[1] < maxY) {
                            maxY = i2[1];
                        }
                    }
                }
                if (indexZ > maxDZ) {
                    maxDZ = indexZ;
                }

            }
        }
        int midX = craft.getMinX() + (maxDX / 2);
        int midY = (minY + maxY) / 2;
        int midZ = craft.getMinZ() + (maxDZ / 2);
        MovecraftLocation midPoint = new MovecraftLocation(midX, midY, midZ);
        return midPoint;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
//	public void onCommand( PlayerCommandPreprocessEvent e ) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return false;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("release")) {
            if (!player.hasPermission("movecraft.commands") && !player.hasPermission("movecraft.commands.release")) {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return true;
            }
            if (args.length > 0) {
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target == null && args[0] != "-a") {
                    sender.sendMessage("That player could not be found");
                } else {
                    if (!player.hasPermission("movecraft.commands.release.others")) {
                        player.sendMessage("You do not have permission to make others release");
                    } else {
                        if (args[0] == "-a") {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                String name = p.getName();
                                final Craft pCraft = CraftManager.getInstance().getCraftByPlayerName(name);
                                if (pCraft != null) {
                                    CraftManager.getInstance().removeCraft(pCraft);

                                }
                            }
                            player.sendMessage("You forced release every player's ship");
                        } else {
                            final Craft pCraft = CraftManager.getInstance().getCraftByPlayerName(args[0]);
                            if (pCraft != null) {
                                CraftManager.getInstance().removeCraft(pCraft);
                                player.sendMessage("You have successfully force released a ship");
                            } else {
                                player.sendMessage("That player is not piloting a craft");
                            }
                        }
                    }
                }
            } else {
                final Craft pCraft = CraftManager.getInstance().getCraftByPlayerName(player.getName());

                if (pCraft != null) {
                    CraftManager.getInstance().removeCraft(pCraft);
                    //e.getPlayer().sendMessage( String.format( I18nSupport.getInternationalisedString( "Player- Craft has been released" ) ) );
                } else {
                    player.sendMessage(I18nSupport.getInternationalisedString("Player- Error - You do not have a craft to release!"));
                }

                return true;
            }
        }

        if (cmd.getName().equalsIgnoreCase("pilot")) {
            if (!player.hasPermission("movecraft.commands") && !player.hasPermission("movecraft.commands.pilot")) {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return true;
            }

            if (args.length > 0) {
                if (player.hasPermission("movecraft." + args[0] + ".pilot")) {
                    CraftType craftType = getCraftTypeFromString(args[0]);
                    if (craftType != null) {
                        Craft oldCraft = CraftManager.getInstance().getCraftByPlayerName(player.getName());
                        if (oldCraft != null) {
                            CraftManager.getInstance().removeCraft(oldCraft);
                        }
                        Craft newCraft = new Craft(craftType, player.getWorld());
                        MovecraftLocation startPoint = MathUtils.bukkit2MovecraftLoc(player.getLocation());
                        newCraft.detect(player, player, startPoint);
                    } else {
                        player.sendMessage(I18nSupport.getInternationalisedString("Unknown craft type"));
                    }
                } else {
                    player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                }
                return true;
            }
        }

        if (cmd.getName().equalsIgnoreCase("rotateleft")) {
            if (!player.hasPermission("movecraft.commands") && !player.hasPermission("movecraft.commands.rotateleft")) {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return true;
            }

            final Craft craft = CraftManager.getInstance().getCraftByPlayerName(player.getName());

            if (player.hasPermission("movecraft." + craft.getType().getCraftName() + ".rotate")) {
                MovecraftLocation midPoint = getCraftMidPoint(craft);
                CraftManager.getInstance().getCraftByPlayerName(player.getName()).rotate(Rotation.ANTICLOCKWISE, midPoint);
            } else {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
            }

            return true;
        }

        if (cmd.getName().equalsIgnoreCase("rotateright")) {
            if (!player.hasPermission("movecraft.commands") && !player.hasPermission("movecraft.commands.rotateright")) {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return true;
            }

            final Craft craft = CraftManager.getInstance().getCraftByPlayerName(player.getName());

            if (player.hasPermission("movecraft." + craft.getType().getCraftName() + ".rotate")) {
                MovecraftLocation midPoint = getCraftMidPoint(craft);
                CraftManager.getInstance().getCraftByPlayerName(player.getName()).rotate(Rotation.CLOCKWISE, midPoint);
            } else {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
            }

            return true;
        }

        if (cmd.getName().equalsIgnoreCase("cruise")) {
            if (!player.hasPermission("movecraft.commands") && !player.hasPermission("movecraft.commands.cruise")) {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return true;
            }

            final Craft craft = CraftManager.getInstance().getCraftByPlayerName(player.getName());
            if (craft == null) {
                player.sendMessage(I18nSupport.getInternationalisedString("You must be piloting a craft"));
                return true;
            }

            if (player.hasPermission("movecraft." + craft.getType().getCraftName() + ".move")) {
                if (craft.getType().getCanCruise()) {
                    if (args.length == 0) {
                        float yaw = player.getLocation().getYaw();
                        if (yaw >= 135 || yaw < -135) {
                            // north
                            craft.setCruiseDirection((byte) 0x3);
                            craft.setCruising(true);
                        } else if (yaw >= 45) {
                            // west
                            craft.setCruiseDirection((byte) 0x5);
                            craft.setCruising(true);
                        } else if (yaw < -45) {
                            // south
                            craft.setCruiseDirection((byte) 0x2);
                            craft.setCruising(true);
                        } else {
                            // east
                            craft.setCruiseDirection((byte) 0x4);
                            craft.setCruising(true);
                        }
                        return true;
                    }
                    if (args[0].equalsIgnoreCase("north") || args[0].equalsIgnoreCase("n")) {
                        craft.setCruiseDirection((byte) 0x3);
                        craft.setCruising(true);
                    } else if (args[0].equalsIgnoreCase("south") || args[0].equalsIgnoreCase("s")) {
                        craft.setCruiseDirection((byte) 0x2);
                        craft.setCruising(true);
                    } else if (args[0].equalsIgnoreCase("east") || args[0].equalsIgnoreCase("e")) {
                        craft.setCruiseDirection((byte) 0x4);
                        craft.setCruising(true);
                    } else if (args[0].equalsIgnoreCase("west") || args[0].equalsIgnoreCase("w")) {
                        craft.setCruiseDirection((byte) 0x5);
                        craft.setCruising(true);
                    } else if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("o")) {
                        craft.setCruising(false);
                    }
                }
            } else {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
            }

            return true;
        }

        if (cmd.getName().equalsIgnoreCase("cruiseoff")) {
            final Craft craft = CraftManager.getInstance().getCraftByPlayerName(player.getName());
            if (craft != null) {
                craft.setCruising(false);
            } else {
                player.sendMessage(I18nSupport.getInternationalisedString("You must be piloting a craft"));
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("craftreport")) {
            if (!player.hasPermission("movecraft.commands") && !player.hasPermission("movecraft.commands.craftreport")) {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return true;
            }

            boolean noCraftsFound = true;
            if (CraftManager.getInstance().getCraftsInWorld(player.getWorld()) != null)
                for (Craft craft : CraftManager.getInstance().getCraftsInWorld(player.getWorld())) {
                    if (craft != null) {
                        String output = "";
                        if (craft.getNotificationPlayer() != null) {
                            output = craft.getType().getCraftName() + " " + craft.getNotificationPlayer().getName() + " " + craft.getBlockList().length + " @ " + craft.getMinX() + "," + craft.getMinY() + "," + craft.getMinZ();
                        } else {
                            output = craft.getType().getCraftName() + " NULL " + craft.getBlockList().length + " @ " + craft.getMinX() + "," + craft.getMinY() + "," + craft.getMinZ();

                        }
                        player.sendMessage(output);
                        noCraftsFound = false;
                    }
                }
            if (noCraftsFound) {
                player.sendMessage("No crafts found");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("contacts")) {
            if (CraftManager.getInstance().getCraftByPlayer(player) != null) {
                Craft ccraft = CraftManager.getInstance().getCraftByPlayer(player);
                boolean foundContact = false;
                for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(ccraft.getW())) {
                    long cposx = ccraft.getMaxX() + ccraft.getMinX();
                    long cposy = ccraft.getMaxY() + ccraft.getMinY();
                    long cposz = ccraft.getMaxZ() + ccraft.getMinZ();
                    cposx = cposx >> 1;
                    cposy = cposy >> 1;
                    cposz = cposz >> 1;
                    long tposx = tcraft.getMaxX() + tcraft.getMinX();
                    long tposy = tcraft.getMaxY() + tcraft.getMinY();
                    long tposz = tcraft.getMaxZ() + tcraft.getMinZ();
                    tposx = tposx >> 1;
                    tposy = tposy >> 1;
                    tposz = tposz >> 1;
                    long diffx = cposx - tposx;
                    long diffy = cposy - tposy;
                    long diffz = cposz - tposz;
                    long distsquared = Math.abs(diffx) * Math.abs(diffx);
                    distsquared += Math.abs(diffy) * Math.abs(diffy);
                    distsquared += Math.abs(diffz) * Math.abs(diffz);
                    long detectionRange = 0;
                    if (tposy > tcraft.getW().getSeaLevel()) {
                        detectionRange = (long) (Math.sqrt(tcraft.getOrigBlockCount()) * tcraft.getType().getDetectionMultiplier());
                    } else {
                        detectionRange = (long) (Math.sqrt(tcraft.getOrigBlockCount()) * tcraft.getType().getUnderwaterDetectionMultiplier());
                    }
                    if (distsquared < detectionRange * detectionRange && tcraft.getNotificationPlayer() != ccraft.getNotificationPlayer()) {
                        // craft has been detected
                        foundContact = true;
                        String notification = "Contact: ";
                        notification += tcraft.getType().getCraftName();
                        notification += " commanded by ";
                        notification += tcraft.getNotificationPlayer().getDisplayName();
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

                        ccraft.getNotificationPlayer().sendMessage(notification);
                    }
                }
                if (!foundContact)
                    player.sendMessage(I18nSupport.getInternationalisedString("No contacts within range"));
                return true;
            } else {
                player.sendMessage(I18nSupport.getInternationalisedString("You must be piloting a craft"));
                return true;
            }

        }

        if (cmd.getName().equalsIgnoreCase("manOverBoard")) {
            if (CraftManager.getInstance().getCraftByPlayerName(player.getName()) != null) {
                Location telPoint = getCraftTeleportPoint(CraftManager.getInstance().getCraftByPlayerName(player.getName()), CraftManager.getInstance().getCraftByPlayerName(player.getName()).getW());
                if (!CraftManager.getInstance().getCraftByPlayerName(player.getName()).getDisabled())
                    player.teleport(telPoint);
            } else {
                for (World w : Bukkit.getWorlds()) {
                    if (CraftManager.getInstance().getCraftsInWorld(w) != null)
                        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(w)) {
                            if (tcraft.getMovedPlayers().containsKey(player)) {
                                if (tcraft.getW() != player.getWorld()) {
                                    player.sendMessage(I18nSupport.getInternationalisedString("Distance to craft is too far"));
                                }
                                if ((System.currentTimeMillis() - tcraft.getMovedPlayers().get(player)) / 1000 < Settings.ManOverBoardTimeout) {
                                    Location telPoint = getCraftTeleportPoint(tcraft, w);
                                    if (telPoint.distance(player.getLocation()) > 1000) {
                                        player.sendMessage(I18nSupport.getInternationalisedString("Distance to craft is too far"));
                                    } else {
                                        if (!CraftManager.getInstance().getCraftByPlayerName(player.getName()).getDisabled())
                                            player.teleport(telPoint);
                                    }
                                }
                            }
                        }
                }
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("assaultinfo")) {
            if (!Settings.AssaultEnable) {
                player.sendMessage(I18nSupport.getInternationalisedString("Assault is not enabled"));
                return true;
            }
            if (!player.hasPermission("movecraft.assaultinfo")) {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return true;
            }
            ApplicableRegionSet regions = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getApplicableRegions(player.getLocation());
            if (regions.size() != 0) {
                LocalPlayer lp = Movecraft.getInstance().getWorldGuardPlugin().wrapPlayer(player);
                Map<String, ProtectedRegion> allRegions = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getRegions();
                boolean foundOwnedRegion = false;
                for (ProtectedRegion iRegion : allRegions.values()) {
                    if (iRegion.isOwner(lp) && iRegion.getFlag(DefaultFlag.TNT) == State.DENY) {
                        foundOwnedRegion = true;
                    }
                }
                if (!foundOwnedRegion) {
                    player.sendMessage(I18nSupport.getInternationalisedString("You are not the owner of any assaultable region and can not assault others"));
                    return true;
                }

                boolean foundAssaultableRegion = false;
                for (ProtectedRegion tRegion : regions.getRegions()) {
                    boolean canBeAssaulted = true;
                    if (Settings.SiegeName != null) {
                        for (String tSiegeName : Settings.SiegeName) {
                            // siegable regions can not be assaulted
                            if (tRegion.getId().equalsIgnoreCase(Settings.SiegeRegion.get(tSiegeName)))
                                canBeAssaulted = false;
                            if (tRegion.getId().equalsIgnoreCase(Settings.SiegeControlRegion.get(tSiegeName)))
                                canBeAssaulted = false;
                        }
                    }
                    // a region can only be assaulted if it disables TNT, this is to prevent child regions or sub regions from being assaulted
                    if (tRegion.getFlag(DefaultFlag.TNT) != State.DENY)
                        canBeAssaulted = false;
                    // regions with no owners can not be assaulted
                    if (tRegion.getOwners().size() == 0)
                        canBeAssaulted = false;
                    if (canBeAssaulted) {
                        String output = "REGION NAME: ";
                        output += tRegion.getId();
                        output += ", OWNED BY: ";
                        output += getRegionOwnerList(tRegion);
                        output += ", MAX DAMAGES: ";
                        double maxDamage = getMaxDamages(tRegion);
                        output += String.format("%.2f", maxDamage);
                        output += ", COST TO ASSAULT: ";
                        double cost = getCostToAssault(tRegion);
                        output += String.format("%.2f", cost);
                        for (Assault assault : Movecraft.getInstance().getAssaultManager().getAssaults()) {
                            if (assault.getRegionName().equals(tRegion.getId())) {
                                if (System.currentTimeMillis() - assault.getStartTime() < Settings.AssaultCooldownHours * (60 * 60 * 1000)) {
                                    canBeAssaulted = false;
                                    output += ", NOT ASSAULTABLE DUE TO RECENT ASSAULT ACTIVITY";
                                }
                            }
                        }
                        if (!areDefendersOnline(tRegion)) {
                            canBeAssaulted = false;
                            output += ", NOT ASSAULTABLE DUE TO INSUFFICIENT ONLINE DEFENDERS";
                        }
                        if (tRegion.isMember(lp)) {
                            output += ", NOT ASSAULTABLE BECAUSE YOU ARE A MEMBER";
                            canBeAssaulted = false;
                        }
                        if (canBeAssaulted) {
                            output += ", AVAILABLE FOR ASSAULT";
                        }
                        player.sendMessage(output);
                        foundAssaultableRegion = true;
                    }

                }
                if (!foundAssaultableRegion) {
                    player.sendMessage(I18nSupport.getInternationalisedString("No Assault eligible regions found"));
                    return true;
                }
            } else {
                player.sendMessage(I18nSupport.getInternationalisedString("No Assault eligible regions found"));
                return true;
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("assault")) {
            if (!Settings.AssaultEnable) {
                player.sendMessage(I18nSupport.getInternationalisedString("Assault is not enabled"));
                return true;
            }
            if (!player.hasPermission("movecraft.assault")) {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return true;
            }
            if (args.length == 0) {
                player.sendMessage(I18nSupport.getInternationalisedString("No region specified"));
                return true;
            }
            ProtectedRegion aRegion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getRegion(args[0]);
            if (aRegion == null) {
                player.sendMessage(I18nSupport.getInternationalisedString("Region not found"));
                return true;
            }
            LocalPlayer lp = Movecraft.getInstance().getWorldGuardPlugin().wrapPlayer(player);
            Map<String, ProtectedRegion> allRegions = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getRegions();
            boolean foundOwnedRegion = false;
            for (ProtectedRegion iRegion : allRegions.values()) {
                if (iRegion.isOwner(lp) && iRegion.getFlag(DefaultFlag.TNT) == State.DENY) {
                    foundOwnedRegion = true;
                }
            }
            if (!foundOwnedRegion) {
                player.sendMessage(I18nSupport.getInternationalisedString("You are not the owner of any assaultable region and can not assault others"));
                return true;
            }
            boolean canBeAssaulted = true;
            if (Settings.SiegeName != null) {
                for (String tSiegeName : Settings.SiegeName) {
                    // siegable regions can not be assaulted
                    if (aRegion.getId().equals(Settings.SiegeRegion.get(tSiegeName)))
                        canBeAssaulted = false;
                    if (aRegion.getId().equals(Settings.SiegeControlRegion.get(tSiegeName)))
                        canBeAssaulted = false;
                }
            }
            // a region can only be assaulted if it disables TNT, this is to prevent child regions or sub regions from being assaulted
            if (aRegion.getFlag(DefaultFlag.TNT) != State.DENY)
                canBeAssaulted = false;
            // regions with no owners can not be assaulted
            if (aRegion.getOwners().size() == 0)
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
            if (!areDefendersOnline(aRegion)) {
                canBeAssaulted = false;
            }
            if (aRegion.isMember(lp)) {
                canBeAssaulted = false;
            }
            if (!canBeAssaulted) {
                player.sendMessage(I18nSupport.getInternationalisedString("You can not assault this region, check AssaultInfo"));
                return true;
            }
            OfflinePlayer offP = Bukkit.getOfflinePlayer(player.getUniqueId());
            if (Movecraft.getInstance().getEconomy().getBalance(offP) < getCostToAssault(aRegion)) {
                player.sendMessage(I18nSupport.getInternationalisedString("You can not afford to assault this region"));
                return true;
            }
//			if(aRegion.getType() instanceof ProtectedCuboidRegion) { // Originally I wasn't going to do non-cubes, but we'll try it and see how it goes. In theory it may repair more than it should but... meh...
            ProtectedRegion cubRegion = aRegion;
            String repairStateName = Movecraft.getInstance().getDataFolder().getAbsolutePath() + "/RegionRepairStates";
            File file = new File(repairStateName);
            if (!file.exists()) {
                file.mkdirs();
            }
            repairStateName += "/";
            repairStateName += aRegion.getId().replaceAll("\\s+", "_");
            repairStateName += ".schematic";
            file = new File(repairStateName);

            Vector min = new Vector(cubRegion.getMinimumPoint().getBlockX(), cubRegion.getMinimumPoint().getBlockY(), cubRegion.getMinimumPoint().getBlockZ());
            Vector max = new Vector(cubRegion.getMaximumPoint().getBlockX(), cubRegion.getMaximumPoint().getBlockY(), cubRegion.getMaximumPoint().getBlockZ());

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
                player.sendMessage(I18nSupport.getInternationalisedString("Could not save file"));
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
            final String taskAssaultName = args[0];
            final Player taskPlayer = player;
            final World taskWorld = player.getWorld();
            final Long taskMaxDamages = (long) getMaxDamages(aRegion);
            final Vector taskMin = min;
            final Vector taskMax = max;

            new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.getServer().broadcastMessage(String.format("The Assault of %s has commenced! The assault leader is %s. Destroy the enemy region!"
                            , taskAssaultName, taskPlayer.getDisplayName()));
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0.25F);
                    }
                    Movecraft.getInstance().getAssaultManager().getAssaults().add(new Assault(taskAssaultName, taskPlayer, taskWorld, System.currentTimeMillis(), taskMaxDamages, taskMin, taskMax));
                    ProtectedRegion tRegion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(taskWorld).getRegion(taskAssaultName);
                    tRegion.setFlag(DefaultFlag.TNT, State.ALLOW);
                }
            }.runTaskLater(Movecraft.getInstance(), (20 * Settings.AssaultDelay));
            return true;

        }

        if (cmd.getName().equalsIgnoreCase("siege")) {
            if (!player.hasPermission("movecraft.siege")) {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return true;
            }
            if (Settings.SiegeName == null) {
                player.sendMessage(I18nSupport.getInternationalisedString("Siege is not configured on this server"));
                return true;
            }
            if (Movecraft.getInstance().siegeInProgress) {
                player.sendMessage(I18nSupport.getInternationalisedString("A Siege is already taking place"));
                return true;
            }
            String foundSiegeName = null;
            LocalPlayer lp = Movecraft.getInstance().getWorldGuardPlugin().wrapPlayer(player);
            ApplicableRegionSet regions = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getApplicableRegions(player.getLocation());
            if (regions.size() != 0) {
                for (ProtectedRegion tRegion : regions.getRegions()) {
                    for (String tSiegeName : Settings.SiegeName) {
                        if (tRegion.getId().equalsIgnoreCase(Settings.SiegeRegion.get(tSiegeName)))
                            foundSiegeName = tSiegeName;
                    }
                }
            }
            if (foundSiegeName != null) {
                long cost = Settings.SiegeCost.get(foundSiegeName);
                int numControlledSieges = 0;
                for (String tSiegeName : Settings.SiegeName) {
                    ProtectedRegion tregion = Movecraft.getInstance().getWorldGuardPlugin().getRegionManager(player.getWorld()).getRegion(Settings.SiegeControlRegion.get(tSiegeName));
                    if (tregion.getOwners().contains(player.getName())) {
                        numControlledSieges++;
                        cost = cost * 2;
                    }
                }

                if (Movecraft.getInstance().getEconomy().has(player, cost)) {
                    Calendar rightNow = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    int hour = rightNow.get(Calendar.HOUR_OF_DAY);
                    int minute = rightNow.get(Calendar.MINUTE);
                    int currMilitaryTime = hour * 100 + minute;
                    int dayOfWeek = rightNow.get(Calendar.DAY_OF_WEEK);
                    boolean isInSchedule = false;

                    if (Settings.SiegeDayOfTheWeek.get(foundSiegeName) != null) {
                        if ((currMilitaryTime > Settings.SiegeScheduleStart.get(foundSiegeName)) && (currMilitaryTime < Settings.SiegeScheduleEnd.get(foundSiegeName)) && dayOfWeek == Settings.SiegeDayOfTheWeek.get(foundSiegeName))
                            isInSchedule = true;
                    } else {
                        if ((currMilitaryTime > Settings.SiegeScheduleStart.get(foundSiegeName)) && (currMilitaryTime < Settings.SiegeScheduleEnd.get(foundSiegeName)))
                            isInSchedule = true;
                    }

                    if (isInSchedule) {
                        if (Settings.SiegeCommandsOnStart.get(foundSiegeName) != null)
                            for (String command : Settings.SiegeCommandsOnStart.get(foundSiegeName)) {
                                command = command.replaceAll("%r", Settings.SiegeRegion.get(foundSiegeName))
                                        .replaceAll("%c", Settings.SiegeCost.get(foundSiegeName).toString());
                                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
                            }

                        Bukkit.getServer().broadcastMessage(String.format("%s is preparing to siege %s! All players wishing to participate in the defense should head there immediately! Siege will begin in %d minutes"
                                , player.getDisplayName(), foundSiegeName, Settings.SiegeDelay.get(foundSiegeName) / 60));
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, (float) 0.25);
                        }
                        final String taskPlayerDisplayName = player.getDisplayName();
                        final String taskPlayerName = player.getName();
                        final String taskSiegeName = foundSiegeName;
                        BukkitTask warningtask1 = new BukkitRunnable() {
                            @Override
                            public void run() {
                                Bukkit.getServer().broadcastMessage(String.format("%s is preparing to siege %s! All players wishing to participate in the defense should head there immediately! Siege will begin in %d minutes"
                                        , taskPlayerDisplayName, taskSiegeName, (Settings.SiegeDelay.get(taskSiegeName) / 60) / 4 * 3));
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0);
                                }
                            }
                        }.runTaskLater(Movecraft.getInstance(), (20 * Settings.SiegeDelay.get(taskSiegeName) / 4 * 1));
                        BukkitTask warningtask2 = new BukkitRunnable() {
                            @Override
                            public void run() {
                                Bukkit.getServer().broadcastMessage(String.format("%s is preparing to siege %s! All players wishing to participate in the defense should head there immediately! Siege will begin in %d minutes"
                                        , taskPlayerDisplayName, taskSiegeName, (Settings.SiegeDelay.get(taskSiegeName) / 60) / 4 * 2));
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0);
                                }
                            }
                        }.runTaskLater(Movecraft.getInstance(), (20 * Settings.SiegeDelay.get(taskSiegeName) / 4 * 2));
                        BukkitTask warningtask3 = new BukkitRunnable() {
                            @Override
                            public void run() {
                                Bukkit.getServer().broadcastMessage(String.format("%s is preparing to siege %s! All players wishing to participate in the defense should head there immediately! Siege will begin in %d minutes"
                                        , taskPlayerDisplayName, taskSiegeName, (Settings.SiegeDelay.get(taskSiegeName) / 60) / 4 * 1));
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, 0);
                                }
                            }
                        }.runTaskLater(Movecraft.getInstance(), (20 * Settings.SiegeDelay.get(taskSiegeName) / 4 * 3));
                        BukkitTask commencetask = new BukkitRunnable() {
                            @Override
                            public void run() {
                                Bukkit.getServer().broadcastMessage(String.format("The Siege of %s has commenced! The siege leader is %s. Destroy the enemy vessels!"
                                        , taskSiegeName, taskPlayerDisplayName, (Settings.SiegeDuration.get(taskSiegeName) / 60)));
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1, (float) 0.25);
                                }
                                Movecraft.getInstance().currentSiegeName = taskSiegeName;
                                Movecraft.getInstance().currentSiegePlayer = taskPlayerName;
                                Movecraft.getInstance().currentSiegeStartTime = System.currentTimeMillis();
                            }
                        }.runTaskLater(Movecraft.getInstance(), (20 * Settings.SiegeDelay.get(taskSiegeName)));
                        Movecraft.getInstance().getLogger().log(Level.INFO, String.format("Siege: %s commenced by %s for a cost of %d", foundSiegeName, player.getName(), cost));
                        Movecraft.getInstance().getEconomy().withdrawPlayer(player, cost);
                        Movecraft.getInstance().siegeInProgress = true;
                        return true;
                    } else {
                        player.sendMessage(I18nSupport.getInternationalisedString("The time is not during the Siege schedule"));
                        return true;
                    }
                } else {
                    player.sendMessage(String.format("You do not have enough money. You need %d", cost));
                    return true;
                }
            } else {
                player.sendMessage(I18nSupport.getInternationalisedString("Could not find a siege configuration for the region you are in"));
                return true;
            }
        }

        return false;
    }

    private boolean areDefendersOnline(ProtectedRegion tRegion) {
        HashSet<UUID> players = new HashSet<>();
        players.addAll(tRegion.getMembers().getUniqueIds());
        players.addAll(tRegion.getOwners().getUniqueIds());
        int numOnline = 0;
        for (UUID playerName : players) {
            if (Bukkit.getPlayer(playerName) != null) {
                numOnline++;
            }
        }
        return numOnline >= Settings.AssaultRequiredDefendersOnline;
    }

    public String getRegionOwnerList(ProtectedRegion tRegion) {
        StringBuilder output = new StringBuilder();
        if (tRegion == null)
            return output.toString();
        boolean first = true;
        if (tRegion.getOwners().getUniqueIds().size() > 0) {
            for (UUID uid : tRegion.getOwners().getUniqueIds()) {
                if (!first)
                    output.append(",");
                else
                    first = false;
                OfflinePlayer offP = Bukkit.getOfflinePlayer(uid);
                if (offP.getName() == null)
                    output.append(uid.toString());
                else
                    output.append(offP.getName());
            }
        }
        if (tRegion.getOwners().getPlayers().size() > 0) {
            for (String player : tRegion.getOwners().getPlayers()) {
                if (!first)
                    output.append(",");
                else
                    first = false;
                output.append(player);
            }
        }
        return output.toString();
    }

    private double getCostToAssault(ProtectedRegion tRegion) {
        HashSet<UUID> players = new HashSet<>();
        players.addAll(tRegion.getMembers().getUniqueIds());
        players.addAll(tRegion.getOwners().getUniqueIds());
        double total = 0.0;
        for (UUID playerName : players) {
            OfflinePlayer offP = Bukkit.getOfflinePlayer(playerName);
            if (offP.getName() != null)
                if (Movecraft.getInstance().getEconomy().getBalance(offP) > 5000000)
                    total += 5000000;
                else
                    total += Movecraft.getInstance().getEconomy().getBalance(offP);
        }
        return total * Settings.AssaultCostPercent / 100.0;
    }

    private double getMaxDamages(ProtectedRegion tRegion) {
        HashSet<UUID> players = new HashSet<>();
        players.addAll(tRegion.getMembers().getUniqueIds());
        players.addAll(tRegion.getOwners().getUniqueIds());
        double total = 0.0;
        for (UUID playerName : players) {
            OfflinePlayer offP = Bukkit.getOfflinePlayer(playerName);
            if (offP.getName() != null)
                if (Movecraft.getInstance().getEconomy().getBalance(offP) > 5000000)
                    total += 5000000;
                else
                    total += Movecraft.getInstance().getEconomy().getBalance(offP);
        }
        return total * Settings.AssaultDamagesCapPercent / 100.0;
    }

}
