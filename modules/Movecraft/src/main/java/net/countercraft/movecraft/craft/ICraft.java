package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.async.detection.DetectionTask;
import net.countercraft.movecraft.async.rotation.RotationTask;
import net.countercraft.movecraft.async.translation.TranslationTask;
import net.countercraft.movecraft.localisation.I18nSupport;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static net.countercraft.movecraft.utils.SignUtils.getFacing;

public class ICraft extends Craft {
    private final UUID id = UUID.randomUUID();

    public ICraft(@NotNull CraftType type, @NotNull World world) {
        super(type, world);
    }


    @Override
    public void detect(@Nullable Player player, @NotNull Player notificationPlayer, MovecraftLocation startPoint) {
        this.setNotificationPlayer(notificationPlayer);
        Movecraft.getInstance().getAsyncManager().submitTask(new DetectionTask(this, startPoint, player, notificationPlayer), this);
    }

    @Override
    public void translate(@NotNull World world, int dx, int dy, int dz) {
        // check to see if the craft is trying to move in a direction not permitted by the type
        if (!world.equals(w) && !(this.getType().getCanSwitchWorld() || Arrays.binarySearch(type.getDisableTeleportToWorlds(), w.getName()) >= 0) && !this.getSinking()) {
            world = w;
        }
        if (!this.getType().allowHorizontalMovement() && !this.getSinking()) {
            dx = 0;
            dz = 0;
        }
        if (!this.getType().allowVerticalMovement() && !this.getSinking()) {
            dy = 0;
        }
        if (dx == 0 && dy == 0 && dz == 0 && world.equals(w)) {
            return;
        }

        if (!this.getType().allowVerticalTakeoffAndLanding() && dy != 0 && !this.getSinking()) {
            if (dx == 0 && dz == 0) {
                return;
            }
        }

        Movecraft.getInstance().getAsyncManager().submitTask(new TranslationTask(this, world, dx, dy, dz), this);
    }

    @Override
    public void rotate(Rotation rotation, MovecraftLocation originPoint) {
        if(getLastRotateTime()+1e9>System.nanoTime()){
            if(getNotificationPlayer()!=null)
                getNotificationPlayer().sendMessage(I18nSupport.getInternationalisedString("Rotation - Turning Too Quickly"));
            return;
        }
        setLastRotateTime(System.nanoTime());
        Movecraft.getInstance().getAsyncManager().submitTask(new RotationTask(this, originPoint, rotation, this.getW()), this);
    }

    @Override
    public void rotate(Rotation rotation, MovecraftLocation originPoint, boolean isSubCraft) {
        Movecraft.getInstance().getAsyncManager().submitTask(new RotationTask(this, originPoint, rotation, this.getW(), isSubCraft), this);
    }

    @NotNull
    @Override
    public Set<Craft> getContacts() {
        final Set<Craft> contacts = new HashSet<>();
        for (Craft contact : CraftManager.getInstance().getCraftsInWorld(w)) {
            MovecraftLocation ccenter = this.getHitBox().getMidPoint();
            MovecraftLocation tcenter = contact.getHitBox().getMidPoint();
            int distsquared = ccenter.distanceSquared(tcenter);
            int detectionRange = (int) (contact.getOrigBlockCount() * (tcenter.getY() > 65 ? contact.getType().getDetectionMultiplier(contact.getW()) : contact.getType().getUnderwaterDetectionMultiplier(contact.getW())));
            detectionRange = detectionRange * 10;
            if (distsquared > detectionRange || contact.getNotificationPlayer() == this.getNotificationPlayer()) {
                continue;
            }
            contacts.add(contact);
        }
        return contacts;
    }

    @Override
    public void resetSigns(@NotNull Sign clicked) {
        for (final MovecraftLocation ml : hitBox) {
            final Block b = ml.toBukkit(w).getBlock();
            if (!(b.getState() instanceof Sign)) {
                continue;
            }
            final Sign sign = (Sign) b.getState();
            if (sign.equals(clicked)) {
                continue;
            }
            if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Cruise: ON")){
                sign.setLine(0, "Cruise: OFF");
            }
            else if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Cruise: OFF")
                    && ChatColor.stripColor(clicked.getLine(0)).equalsIgnoreCase("Cruise: ON")
                    && getFacing(sign) == getFacing(clicked)) {
                    sign.setLine(0,"Cruise: ON");
            }
            else if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Ascend: ON")){
                sign.setLine(0, "Ascend: OFF");
            }
            else if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Ascend: OFF")
                    && ChatColor.stripColor(clicked.getLine(0)).equalsIgnoreCase("Ascend: ON")){
                sign.setLine(0, "Ascend: ON");
            }
            else if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Descend: ON")){
                sign.setLine(0, "Descend: OFF");
            }
            else if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Descend: OFF")
                    && ChatColor.stripColor(clicked.getLine(0)).equalsIgnoreCase("Descend: ON")){
                sign.setLine(0, "Descend: ON");
            }
            sign.update();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ICraft)){
            return false;
        }
        return this.id.equals(((ICraft)obj).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
