package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.async.detection.DetectionTask;
import net.countercraft.movecraft.async.rotation.RotationTask;
import net.countercraft.movecraft.async.translation.TranslationTask;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.HashHitBox;
import net.countercraft.movecraft.utils.MathUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

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

        Movecraft.getInstance().getAsyncManager().submitTask(new TranslationTask(this, dx, dy, dz), this);
    }

    @Override
    public void rotate(Rotation rotation, MovecraftLocation originPoint) {
        if(getLastRotateTime()+1e9>System.nanoTime()){
            if(getNotificationPlayer()!=null)
                getNotificationPlayer().sendMessage(I18nSupport.getInternationalisedString("Rotation - Turning Too Quickly"));
            return;
        }
        setLastRotateTime(System.nanoTime());
        @NotNull final MoveOnRotate move = getMoveOnRotate(rotation, originPoint);
        if (move != MoveOnRotate.NONE) {
            @NotNull final Craft craft = this;
            BukkitRunnable runnable = null;
            switch (move){
                case UP:
                    translate(0, 1, 0);
                    runnable = new BukkitRunnable() {
                        @Override
                        public void run() {
                            Movecraft.getInstance().getAsyncManager().submitTask(new RotationTask(craft, originPoint, rotation, craft.getW()), craft);
                        }
                    };
                    break;
                case DOWN:
                    Movecraft.getInstance().getAsyncManager().submitTask(new RotationTask(craft, originPoint, rotation, craft.getW()), craft);
                    runnable = new BukkitRunnable() {
                        @Override
                        public void run() {
                            translate(0,-1,0);
                        }
                    };
                    break;
            }
            assert runnable != null;
            runnable.runTaskLaterAsynchronously(Movecraft.getInstance(), 2);
            return;
        }

        Movecraft.getInstance().getAsyncManager().submitTask(new RotationTask(this, originPoint, rotation, this.getW()), this);
    }

    @Override
    public void rotate(Rotation rotation, MovecraftLocation originPoint, boolean isSubCraft) {
        @NotNull final MoveOnRotate move = getMoveOnRotate(rotation, originPoint);
        if (move != MoveOnRotate.NONE) {
            @NotNull final Craft craft = this;
            BukkitRunnable runnable = null;
            switch (move){
                case UP:
                    translate(0, 1, 0);
                    runnable = new BukkitRunnable() {
                        @Override
                        public void run() {
                            Movecraft.getInstance().getAsyncManager().submitTask(new RotationTask(craft, originPoint, rotation, craft.getW()), craft);
                        }
                    };
                    break;
                case DOWN:
                    Movecraft.getInstance().getAsyncManager().submitTask(new RotationTask(craft, originPoint, rotation, craft.getW()), craft);
                    runnable = new BukkitRunnable() {
                        @Override
                        public void run() {
                            translate(0,-1,0);
                        }
                    };
                    break;
            }
            assert runnable != null;
            runnable.runTaskLaterAsynchronously(Movecraft.getInstance(), 2);
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

    private MoveOnRotate getMoveOnRotate(Rotation rotation, MovecraftLocation originPoint){
        HashHitBox newHitBox = new HashHitBox();
        if (!getType().getUseGravity()){
            return MoveOnRotate.NONE;
        }
        for (MovecraftLocation origLoc : getHitBox()) {
            MovecraftLocation newLoc = MathUtils.rotateVec(rotation, origLoc.subtract(originPoint)).add(originPoint);
            if (getHitBox().contains(newLoc)){
                continue;
            }
            newHitBox.add(newLoc);
        }
        for (MovecraftLocation newLoc : newHitBox){
            Location bukkitLoc = newLoc.toBukkit(getW());
            Material testObstackle = bukkitLoc.getBlock().getType();
            if (!testObstackle.equals(Material.AIR) && !getType().getPassthroughBlocks().contains(testObstackle) && !getType().getHarvestBlocks().contains(testObstackle)) {
                return MoveOnRotate.UP;
            }
        }
        for (MovecraftLocation newLoc : newHitBox){
            Rotation invertedRotation;
            if (rotation.equals(Rotation.CLOCKWISE)){
                invertedRotation = Rotation.ANTICLOCKWISE;
            } else if (rotation.equals(Rotation.ANTICLOCKWISE)){
                invertedRotation = Rotation.CLOCKWISE;
            } else {
                invertedRotation = rotation;
            }
            MovecraftLocation origLoc = MathUtils.rotateVec(invertedRotation , newLoc.subtract(originPoint)).add(originPoint);
            Location bukkitLoc = newLoc.toBukkit(getW());
            Material testType = bukkitLoc.getBlock().getRelative(0, -1, 0).getType();
            if (!testType.equals(Material.AIR) &&
                    !getType().getPassthroughBlocks().contains(testType) || newLoc.getY() <= getType().getMinHeightLimit()) {
                if (getType().getHarvestBlocks().contains(testType) && getType().getHarvesterBladeBlocks().contains(origLoc.toBukkit(getW()).getBlock().getType())) {
                    continue;
                }
                return MoveOnRotate.NONE;
            }
        }
        return MoveOnRotate.DOWN;
    }

    private enum MoveOnRotate{
        NONE, UP, DOWN
    }
}
