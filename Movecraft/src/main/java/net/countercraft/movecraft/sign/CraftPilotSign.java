package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.*;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftPilotEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.util.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

//TODO: This is not very pretty...
public class CraftPilotSign extends AbstractCraftPilotSign {

    static final Set<MovecraftLocation> PILOTING = Collections.synchronizedSet(new HashSet<>());

    public CraftPilotSign(CraftType craftType) {
        super(craftType);
    }

    @Override
    protected boolean isSignValid(Action clickType, AbstractSignListener.SignWrapper sign, Player player) {
        String header = sign.getRaw(0).trim();
        CraftType craftType = CraftManager.getInstance().getCraftTypeFromString(header);
        if (craftType != this.craftType) {
            return false;
        }
        if (!player.hasPermission("movecraft." + header + ".pilot")) {
            player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected boolean internalProcessSign(Action clickType, AbstractSignListener.SignWrapper sign, Player player, @javax.annotation.Nullable Craft craft) {
        if (this.craftType.getBoolProperty(CraftType.MUST_BE_SUBCRAFT) && craft == null) {
            return false;
        }
        World world = sign.block().getWorld();
        if (craft != null) {
            world = craft.getWorld();
        }
        Location loc = sign.block().getLocation();
        MovecraftLocation startPoint = new MovecraftLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        if (PILOTING.contains(startPoint)) {
            // Always return true
            return true;
        }

        runDetectTask(startPoint, player, sign, craft, world);

        return true;
    }

    protected void runDetectTask(MovecraftLocation startPoint, Player player, AbstractSignListener.SignWrapper signWrapper, Craft parentCraft, World world) {
        PILOTING.add(startPoint);
        CraftManager.getInstance().detect(
                startPoint,
                craftType, (type, w, p, parents) -> {
                    // Assert instructions are not available normally, also this is checked in beforehand sort of
                    assert p != null; // Note: This only passes in a non-null player.
                    if (type.getBoolProperty(CraftType.CRUISE_ON_PILOT)) {
                        if (parents.size() > 1)
                            return new Pair<>(Result.failWithMessage(I18nSupport.getInternationalisedString(
                                    "Detection - Failed - Already commanding a craft")), null);
                        if (parents.size() == 1) {
                            Craft parent = parents.iterator().next();
                            return new Pair<>(Result.succeed(),
                                    new CruiseOnPilotSubCraft(type, world, p, parent));
                        }

                        return new Pair<>(Result.succeed(),
                                new CruiseOnPilotCraft(type, world, p));
                    }
                    else {
                        if (parents.size() > 0)
                            return new Pair<>(Result.failWithMessage(I18nSupport.getInternationalisedString(
                                    "Detection - Failed - Already commanding a craft")), null);

                        return new Pair<>(Result.succeed(),
                                new PlayerCraftImpl(type, w, p));
                    }
                },
                world, player, player,
                craft -> () -> {
                    Bukkit.getServer().getPluginManager().callEvent(new CraftPilotEvent(craft, CraftPilotEvent.Reason.PLAYER));
                    if (craft instanceof SubCraft) { // Subtract craft from the parent
                        Craft parent = ((SubCraft) craft).getParent();
                        var newHitbox = parent.getHitBox().difference(craft.getHitBox());;
                        parent.setHitBox(newHitbox);
                        parent.setOrigBlockCount(parent.getOrigBlockCount() - craft.getHitBox().size());
                    }

                    if (craft.getType().getBoolProperty(CraftType.CRUISE_ON_PILOT)) {
                        // Setup cruise direction
                        BlockFace facing = signWrapper.facing();
                        craft.setCruiseDirection(CruiseDirection.fromBlockFace(facing));

                        // Start craft cruising
                        craft.setLastCruiseUpdate(System.currentTimeMillis());
                        craft.setCruising(true);

                        // Stop craft cruising and sink it in 15 seconds
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                craft.setCruising(false);
                                CraftManager.getInstance().sink(craft);
                            }
                        }.runTaskLater(Movecraft.getInstance(), (craftType.getIntProperty(CraftType.CRUISE_ON_PILOT_LIFETIME)));
                    }
                    else {
                        // Release old craft if it exists
                        Craft oldCraft = CraftManager.getInstance().getCraftByPlayer(player);
                        if (oldCraft != null)
                            CraftManager.getInstance().release(oldCraft, CraftReleaseEvent.Reason.PLAYER, false);
                    }
                }
        );
        // TODO: Move this to be directly called by the craftmanager post detection...
        // Or use the event handler or something
        new BukkitRunnable() {
            @Override
            public void run() {
                PILOTING.remove(startPoint);
            }
        }.runTaskLater(Movecraft.getInstance(), 4);

    }

    @Override
    public boolean processSignChange(SignChangeEvent event, AbstractSignListener.SignWrapper sign) {
        String header = sign.getRaw(0).trim();
        CraftType craftType = CraftManager.getInstance().getCraftTypeFromString(header);
        if (craftType != this.craftType) {
            return false;
        }
        if (Settings.RequireCreatePerm) {
            Player player = event.getPlayer();
            if (!player.hasPermission("movecraft." + header + ".create")) {
                player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }
}
