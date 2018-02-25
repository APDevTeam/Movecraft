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
import net.countercraft.movecraft.localisation.I18nSupport;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class CraftManager implements Iterable<Craft>{
    private static CraftManager ourInstance;
    @NotNull private final Set<Craft> craftList = new HashSet<>();
    @NotNull private final HashMap<Player, Craft> craftPlayerIndex = new HashMap<>();
    @NotNull private final HashMap<Craft, BukkitTask> releaseEvents = new HashMap<>();
    @NotNull private Set<CraftType> craftTypes;

    public static void initialize(){
        ourInstance = new CraftManager();
    }

    private CraftManager() {
        this.craftTypes = loadCraftTypes();
    }

    public static CraftManager getInstance() {
        return ourInstance;
    }

    public Set<CraftType> getCraftTypes() {
        return Collections.unmodifiableSet(craftTypes);
    }

    private Set<CraftType> loadCraftTypes(){
        File craftsFile = new File(Movecraft.getInstance().getDataFolder().getAbsolutePath() + "/types");

        if (craftsFile.mkdirs()) {
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

        Set<CraftType> craftTypes = new HashSet<>();
        File[] files = craftsFile.listFiles();
        if (files == null){
            return craftTypes;
        }

        for (File file : files) {
            if (file.isFile()) {

                if (file.getName().contains(".craft")) {
                    CraftType type = new CraftType(file);
                    craftTypes.add(type);
                }
            }
        }
        if (craftTypes.isEmpty()) {
            Movecraft.getInstance().getLogger().log(Level.SEVERE, "ERROR: NO CRAFTS FOUND!");
        }
        Movecraft.getInstance().getLogger().log(Level.INFO, String.format(I18nSupport.getInternationalisedString("Startup - Number of craft files loaded"), craftTypes.size()));
        return craftTypes;
    }

    public void initCraftTypes() {
        this.craftTypes = loadCraftTypes();
    }

    public void addCraft(Craft c, Player p) {
        this.craftList.add(c);
        this.craftPlayerIndex.put(p, c);
    }

    public void removeCraft(Craft c) {
        removeReleaseTask(c);
        this.craftPlayerIndex.remove(getPlayerFromCraft(c));
        // if its sinking, just remove the craft without notifying or checking
        if (c.getSinking()) {
            this.craftList.remove(c);
        }
        // don't just release torpedoes, make them sink so they don't clutter up the place
        if (c.getType().getCruiseOnPilot()) {
            c.setCruising(false);
            c.setSinking(true);
            c.setNotificationPlayer(null);
            new BukkitRunnable() {
                @Override
                public void run() {
                    CraftManager.getInstance().removeCraft(c);
                }
            }.runTaskLater(Movecraft.getInstance(), (20 * 600));
            //c.setScheduledBlockChanges(null);
            return;
        }
       /* if (c.getScheduledBlockChanges() != null) {
            ArrayList<BlockTranslateCommand> updateCommands = new ArrayList<>();
            updateCommands.addAll(c.getScheduledBlockChanges().keySet());
            if (updateCommands.size() > 0) {
                MapUpdateManager.getInstance().scheduleUpdates(updateCommands.toArray(new BlockTranslateCommand[1]));
            }
        }
        c.setScheduledBlockChanges(null);*/
        this.craftList.remove(c);
        if (getPlayerFromCraft(c) != null) {
            getPlayerFromCraft(c).sendMessage(I18nSupport.getInternationalisedString("Release - Craft has been released message"));
            Movecraft.getInstance().getLogger().log(Level.INFO, String.format(I18nSupport.getInternationalisedString("Release - Player has released a craft console"), c.getNotificationPlayer().getName(), c.getType().getCraftName(), c.getHitBox().size(), c.getHitBox().getMinX(), c.getHitBox().getMinZ()));
        } else {
            Movecraft.getInstance().getLogger().log(Level.INFO, String.format(I18nSupport.getInternationalisedString("NULL Player has released a craft of type %s with size %d at coordinates : %d x , %d z"), c.getType().getCraftName(), c.getHitBox().size(), c.getHitBox().getMinX(), c.getHitBox().getMinZ()));
        }
        this.craftPlayerIndex.remove(getPlayerFromCraft(c));
    }

    public void forceRemoveCraft(Craft c) {
        this.craftList.remove(c);
        if (getPlayerFromCraft(c) != null)
            this.craftPlayerIndex.remove(getPlayerFromCraft(c));
    }

    public Craft[] getCraftsInWorld(World w) {
        Set<Craft> crafts = new HashSet<>();
        for(Craft c : this.craftList){
            if(c.getW() == w)
                crafts.add(c);
        }
        if(crafts.isEmpty())
            return null;
        return crafts.toArray(new Craft[1]);
    }

    public Craft getCraftByPlayer(Player p) {
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
    }

    public Player getPlayerFromCraft(Craft c) {
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
        craftPlayerIndex.put(null, c);
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
}
