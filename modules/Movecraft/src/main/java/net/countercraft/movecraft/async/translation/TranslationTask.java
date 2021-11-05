package net.countercraft.movecraft.async.translation;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftChunk;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.ChunkManager;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftCollisionEvent;
import net.countercraft.movecraft.events.CraftCollisionExplosionEvent;
import net.countercraft.movecraft.events.CraftPreTranslateEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.events.CraftTranslateEvent;
import net.countercraft.movecraft.events.ItemHarvestEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.update.BlockCreateCommand;
import net.countercraft.movecraft.mapUpdater.update.CraftTranslateCommand;
import net.countercraft.movecraft.mapUpdater.update.EntityUpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.ExplosionUpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.ItemDropUpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.countercraft.movecraft.util.hitboxes.MutableHitBox;
import net.countercraft.movecraft.util.hitboxes.SolidHitBox;
import net.countercraft.movecraft.util.hitboxes.SetHitBox;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import static net.countercraft.movecraft.util.MathUtils.withinWorldBorder;

public class TranslationTask extends AsyncTask {
    private static final EnumSet<Material> FALL_THROUGH_BLOCKS = EnumSet.noneOf(Material.class);
    static {
        FALL_THROUGH_BLOCKS.add(Material.AIR);
        FALL_THROUGH_BLOCKS.add(Material.WATER);
        FALL_THROUGH_BLOCKS.add(Material.LAVA);
        FALL_THROUGH_BLOCKS.add(Material.DEAD_BUSH);
        FALL_THROUGH_BLOCKS.addAll(Tag.CORAL_PLANTS.getValues());
        FALL_THROUGH_BLOCKS.add(Material.BROWN_MUSHROOM);
        FALL_THROUGH_BLOCKS.add(Material.RED_MUSHROOM);
        FALL_THROUGH_BLOCKS.add(Material.TORCH);
        FALL_THROUGH_BLOCKS.add(Material.FIRE);
        FALL_THROUGH_BLOCKS.add(Material.REDSTONE_WIRE);
        FALL_THROUGH_BLOCKS.add(Material.LADDER);
        FALL_THROUGH_BLOCKS.addAll(Tag.SIGNS.getValues());
        FALL_THROUGH_BLOCKS.add(Material.LEVER);
        FALL_THROUGH_BLOCKS.add(Material.STONE_BUTTON);
        FALL_THROUGH_BLOCKS.add(Material.SNOW);
        FALL_THROUGH_BLOCKS.add(Material.CARROT);
        FALL_THROUGH_BLOCKS.add(Material.POTATO);
        FALL_THROUGH_BLOCKS.addAll(Tag.FENCES.getValues());
    }

    private World world;
    private int dx, dy, dz;
    private SetHitBox newHitBox;
    private HitBox oldHitBox;
    private SetHitBox oldFluidList;
    private SetHitBox newFluidList;
    private boolean failed;
    private boolean collisionExplosion = false;
    private String failMessage;
    private Collection<UpdateCommand> updates = new HashSet<>();
    private Sound sound = null;
    private float volume = 0.0f;

    public TranslationTask(Craft c, World world, int dx, int dy, int dz) {
        super(c);
        this.world = world;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        newHitBox = new SetHitBox();
        oldHitBox = c.getHitBox();
        oldFluidList = new SetHitBox(c.getFluidLocations());
        newFluidList = new SetHitBox();
    }

    @Override
    protected void execute() throws InterruptedException, ExecutionException {

        //Check if theres anything to move
        if(oldHitBox.isEmpty()){
            return;
        }
        if (getCraft().getDisabled() && (!getCraft().getSinking())) {
            fail(I18nSupport.getInternationalisedString("Translation - Failed Craft Is Disabled"));
            return;
        }
        //call event
        final CraftPreTranslateEvent preTranslateEvent = new CraftPreTranslateEvent(craft, dx, dy, dz, world);
        Bukkit.getServer().getPluginManager().callEvent(preTranslateEvent);
        if (preTranslateEvent.isCancelled()) {
            fail(preTranslateEvent.getFailMessage(), preTranslateEvent.isPlayingFailSound());
            return;
        }
        if (dx != preTranslateEvent.getDx()) {
            dx = preTranslateEvent.getDx();
        }
        if (dy != preTranslateEvent.getDy()) {
            dy = preTranslateEvent.getDy();
        }
        if (dz != preTranslateEvent.getDz()) {
            dz = preTranslateEvent.getDz();
        }
        world = preTranslateEvent.getWorld();

        final int minY = oldHitBox.getMinY();
        final int maxY = oldHitBox.getMaxY();

        // proccess nether portals
        if (Settings.CraftsUseNetherPortals && craft.getWorld().getEnvironment() != Environment.THE_END
                && world.equals(craft.getWorld())) {

            // ensure chunks are loaded for portal checking only if change in location is
            // large
            Set<MovecraftChunk> chunksToLoad = ChunkManager.getChunks(oldHitBox, world, dx, dy, dz);
            MovecraftChunk.addSurroundingChunks(chunksToLoad, 2);
            ChunkManager.checkChunks(chunksToLoad);
            if (!chunksToLoad.isEmpty()) {
                ChunkManager.syncLoadChunks(chunksToLoad).get();
            }

            for (MovecraftLocation oldLocation : oldHitBox) {

                Location location = oldLocation.translate(dx, dy, dz).toBukkit(craft.getWorld());
                Block block = craft.getWorld().getBlockAt(location);
                if (block.getType() == Material.NETHER_PORTAL) {

                    if (processNetherPortal(block)) {
                        sound = Sound.BLOCK_PORTAL_TRAVEL;
                        volume = 0.25f;
                        break;
                    }

                }

            }

        }

        // ensure chunks are loaded only if world is different or change in location is
        // large
        // !world.equals(craft.getW()) || Math.abs(dx) + oldHitBox.getXLength() >=
        // (Bukkit.getServer().getViewDistance() - 1) * 16 || Math.abs(dz) +
        // oldHitBox.getZLength() >= (Bukkit.getServer().getViewDistance() - 1) * 16
        Set<MovecraftChunk> chunksToLoad = ChunkManager.getChunks(oldHitBox, craft.getWorld());
        chunksToLoad.addAll(ChunkManager.getChunks(oldHitBox, world, dx, dy, dz));
        MovecraftChunk.addSurroundingChunks(chunksToLoad, 1);
        ChunkManager.checkChunks(chunksToLoad);
        if (!chunksToLoad.isEmpty()) {
            ChunkManager.syncLoadChunks(chunksToLoad).get();
        }

        // Only modify dy when not switching worlds
        //Check if the craft is too high
        if(world.equals(craft.getWorld()) && (int) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_MAX_HEIGHT_LIMIT, craft.getWorld()) < craft.getHitBox().getMinY()){
            dy = Math.min(dy,-1);
        }else if(world.equals(craft.getWorld()) && (int) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_MAX_HEIGHT_ABOVE_GROUND, craft.getWorld()) > 0){
            final MovecraftLocation middle = oldHitBox.getMidPoint();
            int testY = minY;
            while (testY > 0){
                testY--;
                if (craft.getWorld().getBlockAt(middle.getX(),testY,middle.getZ()).getType() != Material.AIR)
                    break;
            }
            if (maxY - testY > (int) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_MAX_HEIGHT_ABOVE_GROUND, world)) {
                dy = Math.min(dy,-1);
            }
        }
        //Process gravity
        if (world.equals(craft.getWorld()) && craft.getType().getBoolProperty(CraftType.USE_GRAVITY) && !craft.getSinking()) {
            int incline = inclineCraft(oldHitBox);
            if (incline > 0){
                boolean tooSteep = craft.getType().getIntProperty(CraftType.GRAVITY_INCLINE_DISTANCE) > -1 && incline > craft.getType().getIntProperty(CraftType.GRAVITY_INCLINE_DISTANCE);
                if (tooSteep && craft.getType().getFloatProperty(CraftType.COLLISION_EXPLOSION) <= 0F) {
                    fail(I18nSupport.getInternationalisedString("Translation - Failed Incline too steep"));
                    return;
                }
                dy = tooSteep ? 0 : incline;
            } else if (!isOnGround(oldHitBox) && craft.getType().getBoolProperty(CraftType.CAN_HOVER)) {
                MovecraftLocation midPoint = oldHitBox.getMidPoint();
                int centreMinY = oldHitBox.getMinYAt(midPoint.getX(), midPoint.getZ());
                int groundY = centreMinY;
                World w = craft.getWorld();
                while (w.getBlockAt(midPoint.getX(), groundY - 1, midPoint.getZ()).getType() == Material.AIR || craft.getType().getMaterialSetProperty(CraftType.PASSTHROUGH_BLOCKS).contains(w.getBlockAt(midPoint.getX(), groundY - 1, midPoint.getZ()).getType())){
                    groundY--;
                }
                if (centreMinY - groundY > craft.getType().getIntProperty(CraftType.HOVER_LIMIT)){
                    dy = -1;
                }
            } else if (!isOnGround(oldHitBox)){
                dy = dropDistance(oldHitBox);
            }
            if (Settings.Debug) {
                Movecraft.getInstance().getLogger().info("dy: " + dy);
            }
        }
        //Fail the movement if the craft is too high and if the craft is not explosive
        int maxHeightLimit = (int) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_MAX_HEIGHT_LIMIT, world);
        int minHeightLimit = (int) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_MIN_HEIGHT_LIMIT, world);
        if(dy > 0 && maxY + dy > maxHeightLimit && craft.getType().getFloatProperty(CraftType.COLLISION_EXPLOSION) <= 0F) {
            fail(I18nSupport.getInternationalisedString("Translation - Failed Craft hit height limit"));
            return;
        } else if (dy>0 && maxY + dy > maxHeightLimit) { //If explosive and too high, set dy to 0
            dy = 0;
        } else if (minY + dy < minHeightLimit && dy < 0 && !craft.getSinking() && !craft.getType().getBoolProperty(CraftType.USE_GRAVITY)) {
            fail(I18nSupport.getInternationalisedString("Translation - Failed Craft hit minimum height limit"));
            return;
        } else if (minY + dy < minHeightLimit && dy < 0 && craft.getType().getBoolProperty(CraftType.USE_GRAVITY)) {
            //if a craft using gravity hits the minimum height limit, set dy = 0 instead of failing
            dy = 0;
        }

        if (!(dy < 0 && dx == 0 && dz == 0) && !checkFuel()) {
            fail(I18nSupport.getInternationalisedString("Translation - Failed Craft out of fuel"));
            return;
        }


        final EnumSet<Material> harvestBlocks = craft.getType().getMaterialSetProperty(CraftType.HARVEST_BLOCKS);
        final List<MovecraftLocation> harvestedBlocks = new ArrayList<>();
        final EnumSet<Material> harvesterBladeBlocks = craft.getType().getMaterialSetProperty(CraftType.HARVESTER_BLADE_BLOCKS);
        final SetHitBox collisionBox = new SetHitBox();
        for(MovecraftLocation oldLocation : oldHitBox){
            final MovecraftLocation newLocation = oldLocation.translate(dx,dy,dz);
            //If the new location already exists in the old hitbox than this is unnecessary because a craft can't hit
            //itself
            if(world.equals(craft.getWorld()) && oldHitBox.contains(newLocation)){
                newHitBox.add(newLocation);
                continue;
            }
            final Material testMaterial = newLocation.toBukkit(world).getBlock().getType();

            if ((testMaterial.equals(Material.CHEST) || testMaterial.equals(Material.TRAPPED_CHEST)) && checkChests(testMaterial, newLocation)) {
                //prevent chests collision
                fail(String.format(I18nSupport.getInternationalisedString("Translation - Failed Craft is obstructed") + " @ %d,%d,%d,%s", newLocation.getX(), newLocation.getY(), newLocation.getZ(), newLocation.toBukkit(craft.getWorld()).getBlock().getType().toString()));
                return;
            }
            if (!withinWorldBorder(world, newLocation)) {
                fail(I18nSupport.getInternationalisedString("Translation - Failed Craft cannot pass world border") + String.format(" @ %d,%d,%d", newLocation.getX(), newLocation.getY(), newLocation.getZ()));
                return;
            }

            boolean blockObstructed;
            if (craft.getSinking()) {
                blockObstructed = !FALL_THROUGH_BLOCKS.contains(testMaterial);
            } else {
                blockObstructed = !craft.getType().getMaterialSetProperty(CraftType.PASSTHROUGH_BLOCKS).contains(testMaterial) && !testMaterial.equals(Material.AIR);
            }

            boolean ignoreBlock = false;
            // air never obstructs anything (changed 4/18/2017 to prevent drilling machines)
            if (oldLocation.toBukkit(craft.getWorld()).getBlock().getType().equals(Material.AIR) && blockObstructed) {
                ignoreBlock = true;
            }

            if (blockObstructed && !harvestBlocks.isEmpty() && harvestBlocks.contains(testMaterial)) {
                Material tmpType = oldLocation.toBukkit(craft.getWorld()).getBlock().getType();
                if (harvesterBladeBlocks.size() > 0 && harvesterBladeBlocks.contains(tmpType)) {
                    blockObstructed = false;
                    harvestedBlocks.add(newLocation);
                }
            }



            if (blockObstructed) {
                if (!craft.getSinking() && craft.getType().getFloatProperty(CraftType.COLLISION_EXPLOSION) <= 0F) {
                    fail(String.format(I18nSupport.getInternationalisedString("Translation - Failed Craft is obstructed") + " @ %d,%d,%d,%s", newLocation.getX(), newLocation.getY(), newLocation.getZ(), testMaterial.toString()));
                    return;
                }
                collisionBox.add(newLocation);
            } else {
                if (!ignoreBlock) {
                    newHitBox.add(newLocation);
                }
            } //END OF: if (blockObstructed)
        }

        if (!oldFluidList.isEmpty()) {
            for (MovecraftLocation fluidLoc : oldFluidList) {
                newFluidList.add(fluidLoc.translate(dx, dy, dz));
            }
        }

        if (craft.getType().getMaterialSetProperty(CraftType.FORBIDDEN_HOVER_OVER_BLOCKS).size() > 0){
            MovecraftLocation test = new MovecraftLocation(newHitBox.getMidPoint().getX(), newHitBox.getMinY(), newHitBox.getMidPoint().getZ());
            test = test.translate(0, -1, 0);
            while (test.toBukkit(world).getBlock().getType() == Material.AIR){
                test = test.translate(0, -1, 0);
            }
            Material testType = test.toBukkit(world).getBlock().getType();
            if (craft.getType().getMaterialSetProperty(CraftType.FORBIDDEN_HOVER_OVER_BLOCKS).contains(testType)){
                fail(String.format(I18nSupport.getInternationalisedString("Translation - Failed Craft over block"), testType.name().toLowerCase().replace("_", " ")));
            }
        }
        //call event
        CraftTranslateEvent translateEvent = new CraftTranslateEvent(craft, oldHitBox, newHitBox, world);
        Bukkit.getServer().getPluginManager().callEvent(translateEvent);
        if(translateEvent.isCancelled()){
            this.fail(translateEvent.getFailMessage(), translateEvent.isPlayingFailSound());
            return;
        }

        // do not switch world if sinking
        if(craft.getSinking()){
            List<MovecraftLocation> air = new ArrayList<>();
            for(MovecraftLocation location: oldHitBox){
                if(location.toBukkit(craft.getWorld()).getBlock().getType() == Material.AIR){
                    air.add(location.translate(dx,dy,dz));
                }
            }
            newHitBox.removeAll(air);
            for(MovecraftLocation location : collisionBox){
                if (craft.getType().getFloatProperty(CraftType.EXPLODE_ON_CRASH) > 0F) {
                    if (System.currentTimeMillis() - craft.getOrigPilotTime() <= 1000) {
                        continue;
                    }
                    Location loc = location.toBukkit(craft.getWorld());
                    if (!loc.getBlock().getType().equals(Material.AIR)  && ThreadLocalRandom.current().nextDouble(1) < .05) {
                        updates.add(new ExplosionUpdateCommand( loc, craft.getType().getFloatProperty(CraftType.EXPLODE_ON_CRASH)));
                        collisionExplosion = true;
                    }
                }
                SetHitBox toRemove = new SetHitBox();
                MovecraftLocation next = location.translate(-dx,-dy,-dz);
                while(oldHitBox.contains(next)) {
                    toRemove.add(next);
                    next = next.translate(0,1,0);
                }
                craft.getCollapsedHitBox().addAll(toRemove);
                newHitBox.removeAll(toRemove);
            }
        } else if ((craft.getType().getFloatProperty(CraftType.COLLISION_EXPLOSION) > 0F) && System.currentTimeMillis() - craft.getOrigPilotTime() > Settings.CollisionPrimer) {
            for(MovecraftLocation location : collisionBox) {
                float explosionForce = craft.getType().getFloatProperty(CraftType.COLLISION_EXPLOSION);
                if (craft.getType().getBoolProperty(CraftType.FOCUSED_EXPLOSION)) {
                    explosionForce *= Math.min(oldHitBox.size(), craft.getType().getIntProperty(CraftType.MAX_SIZE));
                }
                //TODO: Account for underwater explosions
                /*if (location.getY() < waterLine) { // underwater explosions require more force to do anything
                    explosionForce += 25;//TODO: find the correct amount
                }*/
                Location oldLocation = location.translate(-dx,-dy,-dz).toBukkit(craft.getWorld());
                Location newLocation = location.toBukkit(world);
                if (!oldLocation.getBlock().getType().equals(Material.AIR)) {
                    CraftCollisionExplosionEvent e = new CraftCollisionExplosionEvent(craft, newLocation, craft.getWorld());
                    Bukkit.getServer().getPluginManager().callEvent(e);
                    if(!e.isCancelled()) {
                        updates.add(new ExplosionUpdateCommand(newLocation, explosionForce));
                        collisionExplosion = true;
                    }
                }
                if (craft.getType().getBoolProperty(CraftType.FOCUSED_EXPLOSION)) { // don't handle any further collisions if it is set to focusedexplosion
                    break;
                }
            }
        }

        if(!collisionBox.isEmpty() && craft.getType().getBoolProperty(CraftType.CRUISE_ON_PILOT)){
            CraftManager.getInstance().removeCraft(craft, CraftReleaseEvent.Reason.EMPTY);
            for(MovecraftLocation location : oldHitBox){
                BlockData phaseBlock = craft.getPhaseBlocks().getOrDefault(location.toBukkit(craft.getWorld()), Material.AIR.createBlockData());
                updates.add(new BlockCreateCommand(craft.getWorld(), location, phaseBlock));
            }
            newHitBox = new SetHitBox();
        }

        if(!collisionBox.isEmpty()){
            Bukkit.getServer().getPluginManager().callEvent(new CraftCollisionEvent(craft, collisionBox, world));
        }

        updates.add(new CraftTranslateCommand(craft, new MovecraftLocation(dx, dy, dz), world));

        //prevents torpedo and rocket pilots
        if (craft.getType().getBoolProperty(CraftType.MOVE_ENTITIES) && !(craft.getSinking() && craft.getType().getBoolProperty(CraftType.ONLY_MOVE_PLAYERS))) {
            Location midpoint = new Location(
                    craft.getWorld(),
                    (oldHitBox.getMaxX() + oldHitBox.getMinX())/2.0,
                    (oldHitBox.getMaxY() + oldHitBox.getMinY())/2.0,
                    (oldHitBox.getMaxZ() + oldHitBox.getMinZ())/2.0);
            for (Entity entity : craft.getWorld().getNearbyEntities(midpoint, oldHitBox.getXLength() / 2.0 + 1, oldHitBox.getYLength() / 2.0 + 2, oldHitBox.getZLength() / 2.0 + 1)) {
                if (entity.getType() == EntityType.PLAYER) {
                    if(craft.getSinking()){
                        continue;
                    }
                    EntityUpdateCommand eUp = new EntityUpdateCommand(entity, dx, dy, dz, 0, 0, world, sound, volume);
                    updates.add(eUp);
                } else if (!craft.getType().getBoolProperty(CraftType.ONLY_MOVE_PLAYERS) || entity.getType() == EntityType.PRIMED_TNT) {
                    EntityUpdateCommand eUp = new EntityUpdateCommand(entity, dx, dy, dz, 0, 0, world);
                    updates.add(eUp);
                }
            }
        } else {
            //add releaseTask without playermove to manager
            if (!craft.getType().getBoolProperty(CraftType.CRUISE_ON_PILOT) && !craft.getSinking())  // not necessary to release cruiseonpilot crafts, because they will already be released
                CraftManager.getInstance().addReleaseTask(craft);
        }
        captureYield(harvestedBlocks);
    }

    private void fail(@NotNull String failMessage) {
        fail(failMessage, true);
    }

    private void fail(@NotNull String message, boolean playSound) {
        failed=true;
        failMessage=message;
        if (craft.getDisabled()) {
            craft.getAudience().playSound(net.kyori.adventure.sound.Sound.sound(Key.key("entity.iron_golem.death"), net.kyori.adventure.sound.Sound.Source.NEUTRAL, 5.0f, 5.0f));
            return;
        }
        if (!playSound) {
            return;
        }
        var object = craft.getType().getObjectProperty(CraftType.COLLISION_SOUND);
        if(!(object instanceof net.kyori.adventure.sound.Sound))
            throw new IllegalStateException("COLLISION_SOUND must be of type Sound");
        craft.getAudience().playSound((net.kyori.adventure.sound.Sound) object);
    }

    private static final MovecraftLocation[] SHIFTS = {
            new MovecraftLocation(1,0,0),
            new MovecraftLocation(-1,0,0),
            new MovecraftLocation(0,0,1),
            new MovecraftLocation(0,0,-1)};

    private boolean checkChests(Material mBlock, MovecraftLocation newLoc) {
        for(MovecraftLocation shift : SHIFTS){
            MovecraftLocation aroundNewLoc = newLoc.add(shift);
            Material testMaterial = craft.getWorld().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
            if (testMaterial.equals(mBlock) && !oldHitBox.contains(aroundNewLoc)) {
                return true;
            }
        }
        return false;
    }

    private void captureYield(List<MovecraftLocation> harvestedBlocks) {
        if (harvestedBlocks.isEmpty()) {
            return;
        }
        ArrayList<Inventory> chests = new ArrayList<>();
        //find chests
        for (MovecraftLocation loc : oldHitBox) {
            Block block = craft.getWorld().getBlockAt(loc.getX(), loc.getY(), loc.getZ());
            if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST)
                chests.add(((InventoryHolder) (block.getState())).getInventory());
        }

        for (MovecraftLocation harvestedBlock : harvestedBlocks) {
            Block block = craft.getWorld().getBlockAt(harvestedBlock.getX(), harvestedBlock.getY(), harvestedBlock.getZ());
            List<ItemStack> drops = new ArrayList<>(block.getDrops());
            //generate seed drops
            if (block.getType() == Material.WHEAT) {
                Random rand = new Random();
                int amount = rand.nextInt(4);
                if (amount > 0) {
                    ItemStack seeds = new ItemStack(Material.WHEAT_SEEDS, amount);
                    drops.add(seeds);
                }
            }
            //get contents of inventories before deposting
            if (block.getState() instanceof InventoryHolder) {
                if (block.getState() instanceof Chest) {
                    drops.addAll(Arrays.asList(((Chest) block.getState()).getBlockInventory().getContents()));
                } else {
                    drops.addAll(Arrays.asList((((InventoryHolder) block.getState()).getInventory().getContents())));
                }
            }
            //call event
            final ItemHarvestEvent harvestEvent = new ItemHarvestEvent(craft, drops, harvestedBlock.toBukkit(craft.getWorld()));
            Bukkit.getServer().getPluginManager().callEvent(harvestEvent);
            for (ItemStack drop : drops) {
                ItemStack retStack = putInToChests(drop, chests);
                if (retStack != null)
                    //drop items on position
                    updates.add(new ItemDropUpdateCommand(new Location(craft.getWorld(), harvestedBlock.getX(), harvestedBlock.getY(), harvestedBlock.getZ()), retStack));
            }
        }
    }
    
    private boolean processNetherPortal(@NotNull Block block) {

        int portalX = 0;
        int portalZ = 0;
        Location portalNegCorner = new Location(block.getWorld(), 0, 0, 0);
        Location portalPosCorner = new Location(block.getWorld(), 0, 0, 0);
        if (block.getData() == 2) portalZ = 1;
        else portalX = 1;

        Material testMaterial = null;
        int testX = block.getX();
        int testY = block.getY();
        int testZ = block.getZ();

        // find lowest x or z
        do {
            testX -= portalX;
            testZ -= portalZ;
            testMaterial = block.getWorld().getBlockAt(testX, testY, testZ).getType();
        } while (testMaterial == Material.NETHER_PORTAL);
        portalNegCorner.setX(testX + portalX);
        portalNegCorner.setZ(testZ + portalZ);

        testX = block.getX();
        testZ = block.getZ();

        // find highest x or z
        do {
            testX += portalX;
            testZ += portalZ;
            testMaterial = block.getWorld().getBlockAt(testX, testY, testZ).getType();
        } while (testMaterial == Material.NETHER_PORTAL);
        portalPosCorner.setX(testX - portalX);
        portalPosCorner.setZ(testZ - portalZ);

        testX = block.getX();
        testZ = block.getZ();

        // find lowest y
        do {
            testY -= 1;
            testMaterial = block.getWorld().getBlockAt(testX, testY, testZ).getType();
        } while (testMaterial == Material.NETHER_PORTAL);
        portalNegCorner.setY(testY + 1);

        testY = block.getY();

        // find highest y
        do {
            testY += 1;
            testMaterial = block.getWorld().getBlockAt(testX, testY, testZ).getType();
        } while (testMaterial == Material.NETHER_PORTAL);
        portalPosCorner.setY(testY - 1);

        if (portalX == 1) { // if portal is on x axis fail if craft x length does not fit in portal
            if (oldHitBox.getMinX() + dx < portalNegCorner.getBlockX())
                return false;
            if (oldHitBox.getMaxX() + dx > portalPosCorner.getBlockX())
                return false;
        } else { // if portal is on z axis fail if craft z length does not fit in portal
            if (oldHitBox.getMinZ() + dz < portalNegCorner.getBlockZ())
                return false;
            if (oldHitBox.getMaxZ() + dz > portalPosCorner.getBlockZ())
                return false;
        }

        // fail if craft y length does not fit in portal
        if (oldHitBox.getMinY() + dy < portalNegCorner.getBlockY())
            return false;
        if (oldHitBox.getMaxY() + dy > portalPosCorner.getBlockY())
            return false;

        String worldName = craft.getWorld().getName();
        double scaleFactor = 1.0;
        if (craft.getWorld().getEnvironment() == Environment.NETHER) { // if in nether
            world = Bukkit.getWorld(worldName.substring(0, worldName.length() - 7)); // remove _nether from world name
            scaleFactor = 8.0;
        } else { // if in overworld
            world = Bukkit.getWorld(worldName += "_nether"); // add _nether to world name
            scaleFactor = 0.125;
        }

        // scale destination x and z based on negative most corner of portal
        int scaleDx = (int) (portalNegCorner.getBlockX() * scaleFactor - portalNegCorner.getBlockX());
        int scaleDz = (int) (portalNegCorner.getBlockZ() * scaleFactor - portalNegCorner.getBlockZ());
        dx += scaleDx;
        dz += scaleDz;

        MovecraftLocation midpoint = oldHitBox.getMidPoint();
        if (portalX == 0) { // if portal is facing x axis
            if (midpoint.getX() < block.getX()) { // craft is on negative side of portal
                dx += oldHitBox.getXLength() + 1;
            } else { // craft is on positive side of portal
                dx -= oldHitBox.getXLength() + 1;
            }
        } else { // if portal is facing z axis
            if (midpoint.getZ() < block.getZ()) { // craft is on negative side of portal
                dz += oldHitBox.getZLength() + 1;
            } else { // craft is on positive side of portal
                dz -= oldHitBox.getZLength() + 1;
            }
        }

        return true;

    }

    private ItemStack putInToChests(ItemStack stack, ArrayList<Inventory> inventories) {
        if (stack == null)
            return null;
        if (inventories == null || inventories.isEmpty())
            return stack;
        for (Inventory inv : inventories) {
            int capacity = 0;
            for (ItemStack itemStack : inv) {
                if (itemStack == null || itemStack.getType() == Material.AIR) {
                    capacity += stack.getMaxStackSize();
                } else if (itemStack.isSimilar(stack)) {
                    capacity += stack.getMaxStackSize() - itemStack.getAmount();
                }
            }
            if (stack.getAmount() > capacity) {
                ItemStack tempItem = stack.clone();
                tempItem.setAmount(capacity);
                stack.setAmount(stack.getAmount() - capacity);
                inv.addItem(tempItem);
            } else {
                inv.addItem(stack);
                return null;
            }

        }
        return stack;
    }

    private MovecraftLocation surfaceLoc(MovecraftLocation ml) {
        MovecraftLocation surfaceLoc = ml;
        Material testType;
        do {
            surfaceLoc = surfaceLoc.translate(0, 1, 0);
            testType = surfaceLoc.toBukkit(craft.getWorld()).getBlock().getType();
        } while ((testType != Material.AIR &&
                !craft.getType().getMaterialSetProperty(CraftType.PASSTHROUGH_BLOCKS).contains(testType) &&
                !oldHitBox.contains(surfaceLoc)) &&
                surfaceLoc.getY() + 1 > (int) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_MAX_HEIGHT_LIMIT, craft.getWorld()));
        return surfaceLoc;
    }

    private int inclineCraft(HitBox hitBox){
        if (isOnGround(hitBox) && dy < 0){
            dy = 0;
        }
        SetHitBox collisionBox = new SetHitBox();
        for (MovecraftLocation ml : hitBox){
            MovecraftLocation nl = ml.translate(dx, dy, dz);
            if (hitBox.contains(nl))
                continue;
            collisionBox.add(nl);
        }

        int elevation = 0;
        for (MovecraftLocation ml : collisionBox){
            Material testType = ml.toBukkit(craft.getWorld()).getBlock().getType();
            if (testType == Material.AIR ||
                    craft.getType().getMaterialSetProperty(CraftType.PASSTHROUGH_BLOCKS).contains(testType) ||
                    (craft.getType().getMaterialSetProperty(CraftType.HARVEST_BLOCKS).contains(testType) &&
                            craft.getType().getMaterialSetProperty(CraftType.HARVESTER_BLADE_BLOCKS).contains(ml.translate(-dx, -dy, -dz).toBukkit(craft.getWorld()).getBlock().getType()))) {
                continue;
            }
            MovecraftLocation surfaceLoc = surfaceLoc(ml);
            if (elevation < surfaceLoc.subtract(ml).getY()) {
                elevation = surfaceLoc.subtract(ml).getY();
            }

        }
        if (elevation == 0) {
            return 0;
        }
        SetHitBox movedCollBox = new SetHitBox();
        for (MovecraftLocation ml : collisionBox) {
            movedCollBox.add(ml.translate(0, elevation, 0));

        }
        return movedCollBox.getMinY() - hitBox.getMinY();
    }

    private int dropDistance (HitBox hitBox) {
        MutableHitBox bottomLocs = new SetHitBox();
        MovecraftLocation corner1 = new MovecraftLocation(hitBox.getMinX(), 0, hitBox.getMinZ());
        MovecraftLocation corner2 = new MovecraftLocation(hitBox.getMaxX(), 0, hitBox.getMaxZ());
        for(MovecraftLocation location : new SolidHitBox(corner1, corner2)){
            int test = hitBox.getMinYAt(location.getX(), location.getZ());
            if(test == -1){
                continue;
            }
            bottomLocs.add(new MovecraftLocation(location.getX(), test, location.getZ()));
        }
        int dropDistance = 0;

        do {
            boolean hitGround = false;

            for (MovecraftLocation ml : bottomLocs) {
                final MovecraftLocation translated = ml.translate(dx, dy, dz);
                //This has to be subtracted by one, or non-passthrough blocks will be within the y drop path
                //obstructing the craft
                MovecraftLocation dropped = translated.translate(0, dropDistance - 1 , 0);
                Material testType = dropped.toBukkit(craft.getWorld()).getBlock().getType();
                hitGround = testType != Material.AIR &&
                        !craft.getType().getMaterialSetProperty(CraftType.PASSTHROUGH_BLOCKS).contains(testType) &&
                        !(craft.getType().getMaterialSetProperty(CraftType.HARVEST_BLOCKS).contains(testType) &&
                        craft.getType().getMaterialSetProperty(CraftType.HARVESTER_BLADE_BLOCKS).contains(ml.toBukkit(craft.getWorld()).getBlock().getType())) ||
                        (int) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_MIN_HEIGHT_LIMIT, craft.getWorld()) == translated.translate(0, dropDistance + 1 , 0).getY();

                if (hitGround) {
                    break;
                }
            }
            if (hitGround) {
                break;
            }
            dropDistance--;

        } while (dropDistance > craft.getType().getIntProperty(CraftType.GRAVITY_DROP_DISTANCE));

        return dropDistance;
    }

    private boolean isOnGround(HitBox hitBox){
        MutableHitBox bottomLocs = new SetHitBox();
        MutableHitBox translatedBottomLocs = new SetHitBox();
        if (hitBox.getMinY() <= (int) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_MIN_HEIGHT_LIMIT, craft.getWorld())) {
            return true;
        }
        MovecraftLocation corner1 = new MovecraftLocation(hitBox.getMinX(), 0, hitBox.getMinZ());
        MovecraftLocation corner2 = new MovecraftLocation(hitBox.getMaxX(), 0, hitBox.getMaxZ());
        for(MovecraftLocation location : new SolidHitBox(corner1, corner2)){
            int test = hitBox.getMinYAt(location.getX(), location.getZ());
            if(test == -1){
                continue;
            }
            bottomLocs.add(new MovecraftLocation(location.getX(), test, location.getZ()));
        }

        boolean bottomLocsOnGround = false;
        for (MovecraftLocation bottomLoc : bottomLocs){
            translatedBottomLocs.add(bottomLoc.translate(dx, dy, dz));
            Material testType = bottomLoc.translate(0, -1, 0).toBukkit(craft.getWorld()).getBlock().getType();
            //If the lowest part of the bottom locs touch the ground, return true anyways
            if (testType == Material.AIR){
                continue;
            } else if (craft.getType().getMaterialSetProperty(CraftType.PASSTHROUGH_BLOCKS).contains(testType)) {
                continue;
            } else if (craft.getType().getMaterialSetProperty(CraftType.HARVEST_BLOCKS).contains(testType) && craft.getType().getMaterialSetProperty(CraftType.HARVESTER_BLADE_BLOCKS).contains(bottomLoc.toBukkit(craft.getWorld()).getBlock().getType())) {
                continue;
            }

            bottomLocsOnGround = true;
        }
        boolean translatedBottomLocsInAir = true;
        for (MovecraftLocation translatedBottomLoc : translatedBottomLocs){
            MovecraftLocation beneath = translatedBottomLoc.translate(0, -1, 0);
            Material testType = beneath.toBukkit(craft.getWorld()).getBlock().getType();
            final CraftType type = craft.getType();
            if (hitBox.contains(beneath) ||
                    bottomLocs.contains(beneath) ||
                    testType == Material.AIR ||
                    type.getMaterialSetProperty(CraftType.PASSTHROUGH_BLOCKS).contains(testType) ||
                    (type.getMaterialSetProperty(CraftType.HARVEST_BLOCKS).contains(testType) && type.getMaterialSetProperty(CraftType.HARVESTER_BLADE_BLOCKS).contains(translatedBottomLoc.translate(-dx, -dy, -dz).toBukkit(craft.getWorld()).getBlock().getType()))){
                continue;
            }
            translatedBottomLocsInAir = false;
            break;
        }
        if (Settings.Debug) {
            final Logger log = Movecraft.getInstance().getLogger();
            log.info("Translated bottom locs in air: " + translatedBottomLocsInAir);
            log.info("Bottom locs on ground: " + bottomLocsOnGround);
        }
        if (dy > 0){
            return bottomLocsOnGround && translatedBottomLocsInAir;
        }
        return !translatedBottomLocsInAir;
    }

    public boolean failed(){
        return failed;
    }

    public String getFailMessage() {
        return failMessage;
    }

    public MutableHitBox getNewHitBox() {
        return newHitBox;
    }

    public MutableHitBox getNewFluidList() {
        return newFluidList;
    }

    public Collection<UpdateCommand> getUpdates() {
        return updates;
    }

    public int getDx(){
        return dx;
    }

    public int getDy() {
        return dy;
    }

    public int getDz() {
        return dz;
    }

    public boolean isCollisionExplosion() {
        return collisionExplosion;
    }
}
