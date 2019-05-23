package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.async.detection.DetectionTask;
import net.countercraft.movecraft.async.rotation.RotationTask;
import net.countercraft.movecraft.async.translation.TranslationTask;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.mapUpdater.update.FuelBurnUpdateCommand;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class ICraft extends Craft {
    private final UUID id = UUID.randomUUID();

    public ICraft(@NotNull CraftType type, @NotNull World world) {
        super(type, world);
    }


    @Override
    public void detect(Player player, Player notificationPlayer, MovecraftLocation startPoint) {
        this.setNotificationPlayer(notificationPlayer);
        Movecraft.getInstance().getAsyncManager().submitTask(new DetectionTask(this, startPoint, player), this);
    }

    @Override
    public void translate(int dx, int dy, int dz) {
        // check to see if the craft is trying to move in a direction not permitted by the type
        if (!this.getType().allowHorizontalMovement() && !this.getSinking()) {
            dx = 0;
            dz = 0;
        }
        if (!this.getType().allowVerticalMovement() && !this.getSinking()) {
            dy = 0;
        }
        if (dx == 0 && dy == 0 && dz == 0) {
            return;
        }

        if (!this.getType().allowVerticalTakeoffAndLanding() && dy != 0 && !this.getSinking()) {
            if (dx == 0 && dz == 0) {
                return;
            }
        }

        // find region that will need to be loaded to translate this craft
        /*int cminX = minX;
        int cmaxX = minX;
        if (dx < 0)
            cminX = cminX + dx;
        int cminZ = minZ;
        int cmaxZ = minZ;
        if (dz < 0)
            cminZ = cminZ + dz;
        for (MovecraftLocation m : blockList) {
            if (m.getX() > cmaxX)
                cmaxX = m.getX();
            if (m.getZ() > cmaxZ)
                cmaxZ = m.getZ();
        }
        if (dx > 0)
            cmaxX = cmaxX + dx;
        if (dz > 0)
            cmaxZ = cmaxZ + dz;
        cminX = cminX >> 4;
        cminZ = cminZ >> 4;
        cmaxX = cmaxX >> 4;
        cmaxZ = cmaxZ >> 4;


        // load all chunks that will be needed to translate this craft
        for (int posX = cminX - 1; posX <= cmaxX + 1; posX++) {
            for (int posZ = cminZ - 1; posZ <= cmaxZ + 1; posZ++) {
                if (!this.getW().isChunkLoaded(posX, posZ)) {
                    this.getW().loadChunk(posX, posZ);
                }
            }
        }*/
        if (!checkFuel(dx,dy,dz)){
            getNotificationPlayer().playSound(getNotificationPlayer().getLocation(),Sound.BLOCK_ANVIL_LAND,1,0);
            getNotificationPlayer().sendMessage(I18nSupport.getInternationalisedString("Translation - Failed Craft out of fuel"));
            return;
        }
        Movecraft.getInstance().getAsyncManager().submitTask(new TranslationTask(this, dx, dy, dz), this);
    }

    @Override
    public void rotate(Rotation rotation, MovecraftLocation originPoint) {
        if(getLastRotateTime()+1e9>System.nanoTime()){
            if(getNotificationPlayer()!=null)
                getNotificationPlayer().sendMessage("You're turning too quickly!");
            return;
        }
        setLastRotateTime(System.nanoTime());
        // find region that will need to be loaded to rotate this craft
        /*int cminX = minX;
        int cmaxX = minX;
        int cminZ = minZ;
        int cmaxZ = minZ;
        for (MovecraftLocation m : blockList) {
            if (m.getX() > cmaxX)
                cmaxX = m.getX();
            if (m.getZ() > cmaxZ)
                cmaxZ = m.getZ();
        }
        int distX = cmaxX - cminX;
        int distZ = cmaxZ - cminZ;
        if (distX > distZ) {
            cminZ -= (distX - distZ) / 2;
            cmaxZ += (distX - distZ) / 2;
        }
        if (distZ > distX) {
            cminX -= (distZ - distX) / 2;
            cmaxX += (distZ - distX) / 2;
        }
        cminX = cminX >> 4;
        cminZ = cminZ >> 4;
        cmaxX = cmaxX >> 4;
        cmaxZ = cmaxZ >> 4;


        // load all chunks that will be needed to rotate this craft
        for (int posX = cminX; posX <= cmaxX; posX++) {
            for (int posZ = cminZ; posZ <= cmaxZ; posZ++) {
                if (!this.getW().isChunkLoaded(posX, posZ)) {
                    this.getW().loadChunk(posX, posZ);
                }
            }
        }*/
        if (!checkFuel(0,0,0)){
            getNotificationPlayer().sendMessage(I18nSupport.getInternationalisedString("Translation - Failed Craft out of fuel"));
            return;
        }
        Movecraft.getInstance().getAsyncManager().submitTask(new RotationTask(this, originPoint, rotation, this.getW()), this);
    }

    @Override
    public void rotate(Rotation rotation, MovecraftLocation originPoint, boolean isSubCraft) {
        if (!checkFuel(0,0,0)){
            getNotificationPlayer().sendMessage(I18nSupport.getInternationalisedString("Translation - Failed Craft out of fuel"));
            return;
        }
        Movecraft.getInstance().getAsyncManager().submitTask(new RotationTask(this, originPoint, rotation, this.getW(), isSubCraft), this);
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
    private boolean checkFuel(int dx, int dy, int dz){
        // check for fuel, burn some from a furnace if needed. Blocks of coal are supported, in addition to coal and charcoal
        double fuelBurnRate = getType().getFuelBurnRate();
        // going down doesn't require fuel
        if (dy == -1 && dx == 0 && dz == 0)
            fuelBurnRate = 0.0;

        if (fuelBurnRate == 0.0 || getSinking()) {
            return true;
        }
        if (getBurningFuel() >= fuelBurnRate) {
            setBurningFuel(getBurningFuel() - fuelBurnRate);
            return true;
        }
        final List<Block> fuelHolders = new ArrayList<>();
        Block fuelHolder = null;
        for (MovecraftLocation bTest : getHitBox()) {
            Block b = getW().getBlockAt(bTest.getX(), bTest.getY(), bTest.getZ());
            //Get all fuel holders
            if (b.getType() == Material.FURNACE) {
                InventoryHolder holder = (InventoryHolder) b.getState();
                for (Material fuel : Settings.FuelTypes.keySet()){
                    if (holder.getInventory().contains(fuel)){
                        fuelHolder = b;
                        break;
                    }
                }
            }
            if (fuelHolder != null) break;

        }
        if (fuelHolder == null){
            return false;
        }
        MapUpdateManager.getInstance().scheduleUpdate(new FuelBurnUpdateCommand(this,fuelHolder));
        return true;
    }
}
