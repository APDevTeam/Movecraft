package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.BaseCraft;
import net.countercraft.movecraft.craft.SubCraft;
import net.countercraft.movecraft.craft.SubcraftRotateCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.SubCraftImpl;
import net.countercraft.movecraft.events.CraftPilotEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.effects.Effect;
import net.countercraft.movecraft.processing.functions.CraftSupplier;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.util.Pair;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.countercraft.movecraft.util.hitboxes.MutableHitBox;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class SubcraftRotateSign implements Listener {
    private static final String HEADER = "Subcraft Rotate";
    private final Set<MovecraftLocation> rotatingCrafts = new HashSet<>();

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        MovecraftRotation rotation;
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK)
            rotation = MovecraftRotation.CLOCKWISE;
        else if(event.getAction() == Action.LEFT_CLICK_BLOCK)
            rotation = MovecraftRotation.ANTICLOCKWISE;
        else
            return;

        BlockState state = event.getClickedBlock().getState();
        if (!(state instanceof Sign))
            return;
        Sign sign = (Sign) state;
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(HEADER))
            return;

        final Location loc = event.getClickedBlock().getLocation();
        final MovecraftLocation startPoint = new MovecraftLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if(rotatingCrafts.contains(startPoint)) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Rotation - Already Rotating"));
            event.setCancelled(true);
            return;
        }

        // rotate subcraft
        String craftTypeStr = ChatColor.stripColor(sign.getLine(1));
        CraftType craftType = CraftManager.getInstance().getCraftTypeFromString(craftTypeStr);
        if(craftType == null)
            return;
        if(ChatColor.stripColor(sign.getLine(2)).equals("")
                && ChatColor.stripColor(sign.getLine(3)).equals("")) {
            sign.setLine(2, "_\\ /_");
            sign.setLine(3, "/ \\");
            sign.update(false, false);
        }

        if(!event.getPlayer().hasPermission("movecraft." + craftTypeStr + ".pilot") || !event.getPlayer().hasPermission("movecraft." + craftTypeStr + ".rotate")) {
            event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return;
        }

        final Craft playerCraft = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
        if(playerCraft != null) {
            if (!playerCraft.isNotProcessing()) {
                event.getPlayer().sendMessage(I18nSupport.getInternationalisedString("Detection - Parent Craft is busy"));
                return;
            }
            playerCraft.setProcessing(true); // prevent the parent craft from moving or updating until the subcraft is done
            new BukkitRunnable() {
                @Override
                public void run() {
                    playerCraft.setProcessing(false);
                }
            }.runTaskLater(Movecraft.getInstance(), (10));
        }

        rotatingCrafts.add(startPoint);

        final Player player = event.getPlayer();
        final World world = event.getClickedBlock().getWorld();
        CraftManager.getInstance().detect(
                startPoint,
                craftType, (type, w, p, parents) -> {
                    if(parents.size() > 1)
                        return new Pair<>(Result.failWithMessage(I18nSupport.getInternationalisedString(
                                "Detection - Failed - Already commanding a craft")), null);
                    if(parents.size() < 1)
                        return new Pair<>(Result.succeed(), new SubcraftRotateCraft(type, w, p));

                    Craft parent = parents.iterator().next();
                    return new Pair<>(Result.succeed(), new SubCraftImpl(type, w, parent));
                },
                world, player,
                Movecraft.getAdventure().player(player),
                craft -> () -> {
                    Bukkit.getServer().getPluginManager().callEvent(new CraftPilotEvent(craft, CraftPilotEvent.Reason.SUB_CRAFT));

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            craft.rotate(rotation, startPoint, true);
                            rotatingCrafts.remove(startPoint);
                            if (craft instanceof SubCraft) {
                                SubCraft subcraft = (SubCraft) craft;
                                var newHitbox = subcraft.getParent().getHitBox().union(craft.getHitBox());
                                subcraft.getParent().setHitBox(newHitbox);
                            }
                            CraftManager.getInstance().removeCraft(craft, CraftReleaseEvent.Reason.SUB_CRAFT);
                        }
                    }.runTaskLater(Movecraft.getInstance(), 3);
                }
        );
        event.setCancelled(true);
    }
}
