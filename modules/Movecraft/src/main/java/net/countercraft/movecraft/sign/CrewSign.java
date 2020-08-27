package net.countercraft.movecraft.sign;

import com.earth2me.essentials.User;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.LegacyUtils;
import net.countercraft.movecraft.utils.SignUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Arrays;

public class CrewSign implements Listener {
    //As of 1.13, each bed color have their own values
    final Material[] beds = !Settings.IsLegacy ? new Material[]{Material.CYAN_BED, Material.BLACK_BED, Material.BLUE_BED,
    Material.BROWN_BED, Material.GRAY_BED, Material.GREEN_BED, Material.LIGHT_BLUE_BED, Material.LIGHT_GRAY_BED, Material.LIME_BED, Material.MAGENTA_BED,
    Material.ORANGE_BED, Material.PINK_BED, Material.PURPLE_BED, Material.RED_BED, Material.WHITE_BED, Material.YELLOW_BED} : new Material[]{LegacyUtils.BED_BLOCK};
    @EventHandler
    public final void onSignChange(SignChangeEvent event) {
        if (!event.getLine(0).equalsIgnoreCase("Crew:")) {
            return;
        }
        Player player = event.getPlayer();
        event.setLine(1, player.getName());
    }

    @EventHandler
    public final void onSignTranslate(SignTranslateEvent event) {
        Craft craft = event.getCraft();
        if (!Settings.AllowCrewSigns || !ChatColor.stripColor(event.getLine(0)).equalsIgnoreCase("Crew:")) {
            return;
        }
        String crewName = ChatColor.stripColor(event.getLine(1));
        Player crewPlayer = Movecraft.getInstance().getServer().getPlayer(crewName);
        if (crewPlayer == null) {
            return;
        }
        Location valid = null;
        for(MovecraftLocation location : event.getLocations()){
            Location bedLoc = location.toBukkit(craft.getWorld()).subtract(0,1,0);
            Material bedType = craft.getWorld().getBlockAt(bedLoc).getType();
            if (Settings.IsLegacy ? bedType.equals(LegacyUtils.BED_BLOCK) : bedType.name().endsWith("_BED")) {
                valid = bedLoc;
                break;
            }
        }
        if(valid == null){
            return;
        }
        craft.getCrewSigns().put(crewPlayer.getUniqueId(), valid);
    }

    @EventHandler
    public final void onSignRightClick(PlayerInteractEvent event) {
        if (!Settings.AllowCrewSigns || !event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        Player player = event.getPlayer();
        final Block block = event.getClickedBlock();
        if (!player.isSneaking() || !SignUtils.isSign(block)) {
            return;
        }
        Sign sign = (Sign) block.getState();
        if (!sign.getLine(0).equalsIgnoreCase("Crew:")) {
            return;
        }
        if (Settings.IsLegacy ? !sign.getBlock().getRelative(0,-1,0).getType().equals(LegacyUtils.BED_BLOCK) : Arrays.binarySearch(beds, sign.getBlock().getRelative(0,-1,0).getType()) < 0) {
            player.sendMessage(I18nSupport.getInternationalisedString("CrewSign - Need Bed Below"));
            return;
        }
        if (!sign.getLine(1).equalsIgnoreCase(player.getName())) {
            player.sendMessage(I18nSupport.getInternationalisedString("CrewSign - Sign Not Owned"));
            return;
        }
        if(CraftManager.getInstance().getCraftByPlayer(player)!=null){
            player.sendMessage(I18nSupport.getInternationalisedString("CrewSign - Craft Currently Piloted"));
            return;
        }
        Location location = sign.getLocation();
        player.sendMessage(I18nSupport.getInternationalisedString("CrewSign - Spawn Set"));
        player.setBedSpawnLocation(location, true);
        if (!Settings.SetHomeToCrewSign || Movecraft.getInstance().getEssentialsPlugin() == null) {
            return;
        }
        User u = Movecraft.getInstance().getEssentialsPlugin().getUser(player);
        u.setHome("home", location);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public final void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Craft craft = CraftManager.getInstance().getCraftByPlayer(player);
        if (craft == null) {
            return;
        }
        if(craft.getSinking() || craft.getDisabled() || !craft.getCrewSigns().containsKey(player.getUniqueId())) {
            return;
        }
        player.sendMessage(I18nSupport.getInternationalisedString("CrewSign - Respawn"));
        Location respawnLoc = craft.getCrewSigns().get(player.getUniqueId());
        Material test = respawnLoc.getBlock().getType();
        final boolean hasBed = Settings.IsLegacy ? test == LegacyUtils.BED_BLOCK : test.name().endsWith("_BED");
        if (Settings.Debug){
            Bukkit.broadcastMessage("Has bed: " + hasBed + ". Found type at " + respawnLoc.toVector() + ": " + test.name());
        }

        if (!hasBed){
            return;
        }
        event.setRespawnLocation(respawnLoc);
    }

    @EventHandler
    public void onCraftDetect(CraftDetectEvent event){
        World world = event.getCraft().getWorld();
        for(MovecraftLocation location: event.getCraft().getHitBox()){
            Block block = location.toBukkit(world).getBlock();
            if (!(block.getState() instanceof Sign)) {
                continue;
            }
            Sign sign = (Sign) block.getState();
            if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Crew:") && Arrays.binarySearch(beds, sign.getLocation().subtract(0,1,0).getBlock().getType()) >= 0) {
                final Player crew = Bukkit.getPlayer(sign.getLine(1));
                if (crew == null){
                    return;
                }
               event.getCraft().getCrewSigns().put(crew.getUniqueId(),block.getLocation().subtract(0,1,0));
            }
        }
    }
}
