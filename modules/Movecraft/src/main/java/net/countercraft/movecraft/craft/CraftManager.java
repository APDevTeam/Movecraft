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

package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

import static net.countercraft.movecraft.utils.ChatUtils.ERROR_PREFIX;

public class CraftManager implements Iterable<Craft>{
    private static CraftManager ourInstance;
    @NotNull private final Set<Craft> craftList = ConcurrentHashMap.newKeySet();
    @NotNull private final ConcurrentMap<Player, Craft> craftPlayerIndex = new ConcurrentHashMap<>();
    @NotNull private final ConcurrentMap<Craft, BukkitTask> releaseEvents = new ConcurrentHashMap<>();
    @NotNull private Set<CraftType> craftTypes;
    @NotNull private final WeakHashMap<Player, Long> overboards = new WeakHashMap<>();

    public static void initialize(){
        ourInstance = new CraftManager();
    }

    private CraftManager() {
        this.craftTypes = loadCraftTypes();
    }

    public static CraftManager getInstance() {
        return ourInstance;
    }

    @NotNull
    public Set<CraftType> getCraftTypes() {
        return Collections.unmodifiableSet(craftTypes);
    }

    @NotNull
    private Set<CraftType> loadCraftTypes(){
        File craftsFile = new File(Movecraft.getInstance().getDataFolder().getAbsolutePath() + "/types");

        if (craftsFile.mkdirs()) {
            if (Settings.IsLegacy) { //save legacy craft files if server version is 1.12.2 or below
                Movecraft.getInstance().saveResource("types/legacy/airship.craft", false);
                Movecraft.getInstance().saveResource("types/legacy/airskiff.craft", false);
                Movecraft.getInstance().saveResource("types/legacy/BigAirship.craft", false);
                Movecraft.getInstance().saveResource("types/legacy/BigSubAirship.craft", false);
                Movecraft.getInstance().saveResource("types/legacy/elevator.craft", false);
                Movecraft.getInstance().saveResource("types/legacy/LaunchTorpedo.craft", false);
                Movecraft.getInstance().saveResource("types/legacy/Ship.craft", false);
                Movecraft.getInstance().saveResource("types/legacy/SubAirship.craft", false);
                Movecraft.getInstance().saveResource("types/legacy/Submarine.craft", false);
                Movecraft.getInstance().saveResource("types/legacy/Turret.craft", false);

            } else if (Settings.is1_14){ //if 1.14, save 1.14 files
                Movecraft.getInstance().saveResource("types/1_14/airship.craft", false);
                Movecraft.getInstance().saveResource("types/1_14/airskiff.craft", false);
                Movecraft.getInstance().saveResource("types/1_14/BigAirship.craft", false);
                Movecraft.getInstance().saveResource("types/1_14/BigSubAirship.craft", false);
                Movecraft.getInstance().saveResource("types/1_14/elevator.craft", false);
                Movecraft.getInstance().saveResource("types/1_14/LaunchTorpedo.craft", false);
                Movecraft.getInstance().saveResource("types/1_14/Ship.craft", false);
                Movecraft.getInstance().saveResource("types/1_14/SubAirship.craft", false);
                Movecraft.getInstance().saveResource("types/1_14/Submarine.craft", false);
                Movecraft.getInstance().saveResource("types/1_14/Turret.craft", false);

            } else { //if 1.13, save 1.13 files
                Movecraft.getInstance().saveResource("types/airship.craft", false);
                Movecraft.getInstance().saveResource("types/airskiff.craft", false);
                Movecraft.getInstance().saveResource("types/BigAirship.craft", false);
                Movecraft.getInstance().saveResource("types/BigSubAirship.craft", false);
                Movecraft.getInstance().saveResource("types/elevator.craft", false);
                Movecraft.getInstance().saveResource("types/LaunchTorpedo.craft", false);
                Movecraft.getInstance().saveResource("types/Ship.craft", false);
                Movecraft.getInstance().saveResource("types/SubAirship.craft", false);
                Movecraft.getInstance().saveResource("types/Submarine.craft", false);
                Movecraft.getInstance().saveResource("types/Turret.craft", false);
            }
        }
        final File legacydir = new File(craftsFile,"legacy");
        if (legacydir.exists()) {
            for (File craftFile : legacydir.listFiles()) {
                final String fileName = craftFile.getName();
                if (!craftFile.renameTo(new File(craftsFile, fileName))) continue;
                craftFile.delete();
            }
            if (legacydir.listFiles().length == 0)
                legacydir.delete();
        }
        final File v1_14dir = new File(craftsFile,"1_14");
        if (v1_14dir.exists()) {
            File[] files = v1_14dir.listFiles();
            for (File craftFile : files) {
                final String fileName = craftFile.getName();
                Bukkit.getLogger().info(fileName);
                File destination = new File(craftsFile, fileName);
                if (!craftFile.renameTo(destination)) continue;
                craftFile.delete();
            }
            if (v1_14dir.listFiles().length == 0)
                v1_14dir.delete();
        }
        Set<CraftType> craftTypes = new HashSet<>();
        File[] files = craftsFile.listFiles();
        if (files == null){
            return craftTypes;
        }

        for (File file : files) {
            if (file.isFile()) {

                if (file.getName().contains(".craft")) {
                    Bukkit.getLogger().info(file.getName());
                    CraftType type = new CraftType(file);
                    craftTypes.add(type);
                }
            }
        }
        if (craftTypes.isEmpty()) {
            Movecraft.getInstance().getLogger().log(Level.SEVERE, ERROR_PREFIX + I18nSupport.getInternationalisedString("Startup - No Crafts Found"));
        }
        Movecraft.getInstance().getLogger().log(Level.INFO, String.format(I18nSupport.getInternationalisedString("Startup - Number of craft files loaded"), craftTypes.size()));
        return craftTypes;
    }

    public void initCraftTypes() {
        this.craftTypes = loadCraftTypes();
    }

    public void addCraft(@NotNull Craft c, @Nullable Player p) {
        this.craftList.add(c);
        if(p!=null)
            this.craftPlayerIndex.put(p, c);
    }

    public void removeCraft(@NotNull Craft c) {
        //TODO move this to callers
        Bukkit.getServer().getPluginManager().callEvent(new CraftReleaseEvent(c, CraftReleaseEvent.Reason.PLAYER));
        removeReleaseTask(c);
        Player player = getPlayerFromCraft(c);
        if (player!=null)
            this.craftPlayerIndex.remove(player);
        // if its sinking, just remove the craft without notifying or checking
        this.craftList.remove(c);
        if(!c.getHitBox().isEmpty()) {
            if (player != null) {
                player.sendMessage(I18nSupport.getInternationalisedString("Release - Craft has been released message"));
                Movecraft.getInstance().getLogger().log(Level.INFO, String.format(I18nSupport.getInternationalisedString("Release - Player has released a craft console"), c.getNotificationPlayer().getName(), c.getType().getCraftName(), c.getHitBox().size(), c.getHitBox().getMinX(), c.getHitBox().getMinZ()));
            } else {
                Movecraft.getInstance().getLogger().log(Level.INFO, String.format(I18nSupport.getInternationalisedString("Release - Null Craft Release Console"), c.getType().getCraftName(), c.getHitBox().size(), c.getHitBox().getMinX(), c.getHitBox().getMinZ()));
            }
        }else{
            Movecraft.getInstance().getLogger().warning(I18nSupport.getInternationalisedString("Release - Empty Craft Release Console"));
        }
        Movecraft.getInstance().getAsyncManager().addWreck(c);
    }

    public void forceRemoveCraft(@NotNull Craft c) {
        this.craftList.remove(c);
        if (getPlayerFromCraft(c) != null)
            this.craftPlayerIndex.remove(getPlayerFromCraft(c));
        Bukkit.getServer().getPluginManager().callEvent(new CraftReleaseEvent(c, CraftReleaseEvent.Reason.FORCE));
    }

    @NotNull
    public Set<Craft> getCraftsInWorld(@NotNull World w) {
        Set<Craft> crafts = new HashSet<>();
        for(Craft c : this.craftList){
            if(c.getW() == w)
                crafts.add(c);
        }
        return crafts;
    }

    public Craft getCraftByPlayer(@NotNull Player p) {
        if(p == null)
            return null;
        return craftPlayerIndex.get(p);
    }


    public Craft getCraftByPlayerName(String name) {
        Set<Player> players = craftPlayerIndex.keySet();
        for (Player player : players) {
            if (player != null && player.getName().equals(name)) {
                return this.craftPlayerIndex.get(player);
            }
        }
        return null;
    }

    public void removeCraftByPlayer(Player player){
        List<Craft> crafts = new ArrayList<>();
        for(Craft c : craftList){
            if(c.getNotificationPlayer() != null && c.getNotificationPlayer().equals(player)){
                releaseEvents.remove(c);
                crafts.add(c);
            }
        }
        craftPlayerIndex.remove(player);
        craftList.removeAll(crafts);
        for(Craft c : crafts){
            Bukkit.getServer().getPluginManager().callEvent(new CraftReleaseEvent(c, CraftReleaseEvent.Reason.DISCONNECT));
        }
    }

    @Nullable
    public Player getPlayerFromCraft(@NotNull Craft c) {
        for (Map.Entry<Player, Craft> playerCraftEntry : craftPlayerIndex.entrySet()) {
            if (playerCraftEntry.getValue() == c) {
                return playerCraftEntry.getKey();
            }
        }
        return null;
    }

    public void removePlayerFromCraft(Craft c) {
        if (getPlayerFromCraft(c) == null) {
            return;
        }
        removeReleaseTask(c);
        Player p = getPlayerFromCraft(c);
        p.sendMessage(I18nSupport.getInternationalisedString("Release - Craft has been released message"));
        Movecraft.getInstance().getLogger().info(String.format(I18nSupport.getInternationalisedString("Release - Player has released a craft console"), p.getName(), c.getType().getCraftName(), c.getHitBox().size(), c.getHitBox().getMinX(), c.getHitBox().getMinZ()));
        c.setNotificationPlayer(null);
        craftPlayerIndex.remove(p);
    }


    @Deprecated
    public final void addReleaseTask(final Craft c) {
        Player p = getPlayerFromCraft(c);
        if (p!=null) {
            p.sendMessage(I18nSupport.getInternationalisedString("Release - Player has left craft"));
        }
        BukkitTask releaseTask = new BukkitRunnable() {
            @Override
            public void run() {
                removeCraft(c);
            }
        }.runTaskLater(Movecraft.getInstance(), (20 * 15));
        releaseEvents.put(c, releaseTask);

    }

    @Deprecated
    public final void removeReleaseTask(final Craft c) {
        Player p = getPlayerFromCraft(c);
        if (p != null) {
            if (releaseEvents.containsKey(c)) {
                if (releaseEvents.get(c) != null)
                    releaseEvents.get(c).cancel();
                releaseEvents.remove(c);
            }
        }
    }

    @Deprecated
    public boolean isReleasing(final Craft craft){
        return releaseEvents.containsKey(craft);
    }

    @NotNull
    @Deprecated
    public Set<Craft> getCraftList(){
        return Collections.unmodifiableSet(craftList);
    }

    public CraftType getCraftTypeFromString(String s) {
        for (CraftType t : craftTypes) {
            if (s.equalsIgnoreCase(t.getCraftName())) {
                return t;
            }
        }
        return null;
    }

    public boolean isEmpty(){
        return this.craftList.isEmpty();
    }

    @NotNull
    @Override
    public Iterator<Craft> iterator() {
        return Collections.unmodifiableSet(this.craftList).iterator();
    }

    public void addOverboard(Player player) {
        overboards.put(player, System.currentTimeMillis());
    }

    @NotNull
    public long getTimeFromOverboard(Player player) {
        return overboards.getOrDefault(player, 0L);
    }
}
