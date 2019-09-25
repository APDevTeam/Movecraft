package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.async.detection.DetectionTask;
import net.countercraft.movecraft.async.rotation.RotationTask;
import net.countercraft.movecraft.async.translation.TranslationTask;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.sync.EntityProcessor;
import net.countercraft.movecraft.utils.HashHitBox;
import net.countercraft.movecraft.utils.MathUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
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


        if (isClimbing()){
            setClimbing(false);
        }
        TranslationTask task = new TranslationTask(this, dx, dy, dz);

        task.getUpdates().addAll(EntityProcessor.translateEntities(this, dx, dy, dz));
        Movecraft.getInstance().getAsyncManager().submitTask(task, this);
    }

    @Override
    public void rotate(Rotation rotation, MovecraftLocation originPoint) {
        if(getLastRotateTime()+1e9>System.nanoTime()){
            if(getNotificationPlayer()!=null)
                getNotificationPlayer().sendMessage(I18nSupport.getInternationalisedString("Rotation - Turning Too Quickly"));
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
        if (getType().getUseGravity()) {
            MoveOnRotate move = moveUp(rotation, originPoint);
            final Craft craft = this;
            if (move.equals(MoveOnRotate.UP)) {
                translate(0, 1, 0);
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        RotationTask task = new RotationTask(craft, originPoint, rotation, craft.getW());
                        task.getUpdates().addAll(EntityProcessor.rotateEntities(craft, originPoint, rotation));
                        Movecraft.getInstance().getAsyncManager().submitTask(task, craft);
                    }
                }.runTaskLaterAsynchronously(Movecraft.getInstance(), 5);
                return;
            } else if (move.equals(MoveOnRotate.DOWN)) {
                RotationTask task = new RotationTask(craft, originPoint, rotation, craft.getW());
                task.getUpdates().addAll(EntityProcessor.rotateEntities(craft, originPoint, rotation));
                Movecraft.getInstance().getAsyncManager().submitTask(task, craft);
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        translate(0, -1, 0);
                    }
                }.runTaskLaterAsynchronously(Movecraft.getInstance(), 5);
                return;
            }
        }
        RotationTask task = new RotationTask(this, originPoint, rotation, this.getW());
        task.getUpdates().addAll(EntityProcessor.rotateEntities(this, originPoint, rotation));
        Movecraft.getInstance().getAsyncManager().submitTask(task, this);

    }

    @Override
    public void rotate(Rotation rotation, MovecraftLocation originPoint, boolean isSubCraft) {
        if (getType().getUseGravity()) {
            MoveOnRotate move = moveUp(rotation, originPoint);
            final Craft craft = this;
            if (move.equals(MoveOnRotate.UP)) {
                translate(0, 1, 0);
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        RotationTask task = new RotationTask(craft, originPoint, rotation, craft.getW());
                        task.getUpdates().addAll(EntityProcessor.rotateEntities(craft, originPoint, rotation));
                        Movecraft.getInstance().getAsyncManager().submitTask(task, craft);
                    }
                }.runTaskLaterAsynchronously(Movecraft.getInstance(), 5);
                return;
            } else if (move.equals(MoveOnRotate.DOWN)) {
                RotationTask task = new RotationTask(craft, originPoint, rotation, craft.getW());
                task.getUpdates().addAll(EntityProcessor.rotateEntities(craft, originPoint, rotation));
                Movecraft.getInstance().getAsyncManager().submitTask(task, craft);
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        translate(0, -1, 0);
                    }
                }.runTaskLaterAsynchronously(Movecraft.getInstance(), 5);
                return;
            }
        }
        RotationTask task = new RotationTask(this, originPoint, rotation, this.getW(), isSubCraft);
        task.getUpdates().addAll(EntityProcessor.rotateEntities(this, originPoint, rotation));
        Movecraft.getInstance().getAsyncManager().submitTask(task, this);
    }

    private MoveOnRotate moveUp(Rotation rotation, MovecraftLocation originPoint){
        HashHitBox newHitBox = new HashHitBox();
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
            Material testSurface = bukkitLoc.getBlock().getRelative(0, -1, 0).getType();
            if (!testSurface.equals(Material.AIR) &&
                    !getType().getPassthroughBlocks().contains(testSurface) || newLoc.getY() <= getType().getMinHeightLimit()) {
                if (getType().getHarvestBlocks().contains(testSurface) && getType().getHarvesterBladeBlocks().contains(origLoc.toBukkit(getW()).getBlock().getType())) {
                    continue;
                }
                return MoveOnRotate.NONE;
            }
        }





        if (getType().getCanHover()){
            MovecraftLocation bottomPoint = new MovecraftLocation(getHitBox().getMidPoint().getX(), getHitBox().getMinY(), getHitBox().getMidPoint().getZ());
            MovecraftLocation surface = bottomPoint;
            while (surface.toBukkit(getW()).getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR || getType().getPassthroughBlocks().contains(surface.toBukkit(getW()).getBlock().getRelative(BlockFace.DOWN).getType())){
                surface = surface.translate(0, -1, 0);
            }
            int distance = bottomPoint.getY() - surface.getY();
            if (distance <= getType().getHoverLimit()){
                return MoveOnRotate.NONE;
            }
        }
        return MoveOnRotate.DOWN;
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

    private enum MoveOnRotate{
        NONE, UP, DOWN
    }
}
