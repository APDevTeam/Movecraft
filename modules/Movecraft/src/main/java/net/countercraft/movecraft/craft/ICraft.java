package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.async.detection.DetectionTask;
import net.countercraft.movecraft.async.rotation.RotationTask;
import net.countercraft.movecraft.async.translation.TranslationTask;
import net.countercraft.movecraft.localisation.I18nSupport;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static net.countercraft.movecraft.utils.SignUtils.getFacing;

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
    public void translate(World world, int dx, int dy, int dz) {
        // check to see if the craft is trying to move in a direction not permitted by the type
    	if (!world.equals(w) && !this.getType().getCanSwitchWorld() && !this.getSinking()) {
    		return;
    	}
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
        
        // ensure chunks are loaded only if teleportation or world switching is allowed and change in location is large
        if (this.getType().getCanTeleport() || this.getType().getCanSwitchWorld()) {
	        if (!world.equals(w) || dx + hitBox.getXLength() >= Bukkit.getServer().getViewDistance() * 16
	        		|| dz + hitBox.getZLength() >= Bukkit.getServer().getViewDistance() * 16) {
	        	
	        	loadChunks(world, dx, dy, dz);
	        	
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
            int detectionRange = (int) (contact.getOrigBlockCount() * (tcenter.getY() > 65 ? contact.getType().getDetectionMultiplier() : contact.getType().getUnderwaterDetectionMultiplier()));
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
    
    private void loadChunks(World world, int dx, int dy, int dz) {
    	// find all chunks in the old and new hitboxes
    	List<Chunk> chunks = new ArrayList<Chunk>();
    	for (MovecraftLocation location : hitBox) {
    		Chunk oldChunk = location.toBukkit(world).getChunk();
        	Chunk newChunk = location.translate(dx, dy, dz).toBukkit(world).getChunk();
        	if (!chunks.contains(oldChunk)) chunks.add(oldChunk);
        	if (!chunks.contains(newChunk)) chunks.add(newChunk);
        }
    	
    	// add all chunks surrounding the included chunks
    	List<Chunk> list = new ArrayList<Chunk>();
    	list.addAll(chunks);
    	for (Chunk chunk : list) {
    		if (chunk.isLoaded()) continue;
    		for (int x = -1; x <= 1; x++) {
    			for (int z = -1; z <= 1; z++) {
    				Chunk c = chunk.getWorld().getChunkAt(chunk.getX() + x, chunk.getZ() + z);
    				if (!chunks.contains(c)) chunks.add(c);
    			}
    		}
    	}
    	
    	// keep those chunks loaded for 10 seconds while the craft teleports
		ChunkManager.addChunksToLoad(chunks);
    }

}
