package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.async.detection.DetectionTask;
import net.countercraft.movecraft.async.rotation.RotationTask;
import net.countercraft.movecraft.async.translation.TranslationTask;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.localisation.I18nSupport;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public void detect(@Nullable Player player, @NotNull Player notificationPlayer, MovecraftLocation startPoint) {
        this.setNotificationPlayer(notificationPlayer);
        Movecraft.getInstance().getAsyncManager().submitTask(new DetectionTask(this, startPoint, player, notificationPlayer), this);
    }

    @Override
    public void translate(World world, int dx, int dy, int dz) {
        // check to see if the craft is trying to move in a direction not permitted by the type
    	if (!world.equals(w) && !this.getType().getCanSwitchWorld() && !this.getSinking()) {
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
    
    /**
     * @param which 1 = current, 2 = destination, 3 = both
     * @param radius amount of buffer chunks around the craft
     */
    @Override
    public List<List<Object>> getChunks(int which, int radius, int dx, int dy, int dz, World world) {
    	
    	List<List<Object>> chunks = new ArrayList<List<Object>>();
    	
    	for (MovecraftLocation location : hitBox) {
    		if ((which & 01) != 0) {
    			int chunkX = location.getX() / 16;
    			if (location.getX() < 0) chunkX--;
    			int chunkZ = location.getZ() / 16;
    			if (location.getZ() < 0) chunkZ--;
    			
    			List<Object> chunk = new ArrayList<Object>();
    			chunk.add(chunkX); chunk.add(chunkZ); chunk.add(w);
    			if (!chunks.contains(chunk)) chunks.add(chunk);
    		}
    		if ((which & 10) != 0) {
    			MovecraftLocation newLocation = location.translate(dx, dy, dz);
    			int chunkX = newLocation.getX() / 16;
    			if (newLocation.getX() < 0) chunkX--;
    			int chunkZ = newLocation.getZ() / 16;
    			if (newLocation.getZ() < 0) chunkZ--;
    			
    			List<Object> chunk = new ArrayList<Object>();
    			chunk.add(chunkX); chunk.add(chunkZ); chunk.add(world);
    			if (!chunks.contains(chunk)) chunks.add(chunk);
    		}
        }
    	
    	// add all chunks surrounding the included chunks
    	if (radius > 0) {
	    	List<List<Object>> tmp = new ArrayList<List<Object>>();
	    	tmp.addAll(chunks);
	    	for (List<Object> chunk : tmp) {
	    		for (int x = -radius; x <= radius; x++) {
	    			for (int z = -radius; z <= radius; z++) {
	    				List<Object> c = new ArrayList<Object>();
	    				c.add((int) (chunk.get(0)) + x); c.add((int) (chunk.get(1)) + z); c.add(chunk.get(2));
	    				if (!chunks.contains(c)) chunks.add(c);
	    			}
	    		}
	    	}
    	}
    	
    	return chunks;
    	
    }
    
    @Override
    public void loadChunks(List<List<Object>> list, Object notify) {
    	if (Settings.Debug)
    		Movecraft.getInstance().getLogger().info("Loading " + list.size() + " chunks...");
    	
    	new BukkitRunnable() {
			
			@Override
			public void run() {
				synchronized (notify) {
			    	
			    	// keep those chunks loaded for 10 seconds while the craft teleports
					List<Chunk> chunks = new ArrayList<Chunk>();
					for (List<Object> chunk : list) {
						chunks.add(((World) chunk.get(2)).getChunkAt((int) chunk.get(0), (int) chunk.get(1)));
					}
					ChunkManager.addChunksToLoad(chunks);
					notify.notifyAll();
					
				}
				
			}
			
		}.runTask(Movecraft.getInstance());
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