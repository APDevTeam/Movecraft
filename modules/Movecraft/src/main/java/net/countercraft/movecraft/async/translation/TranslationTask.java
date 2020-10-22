package net.countercraft.movecraft.async.translation;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftChunk;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.ChunkManager;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.events.*;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.update.*;
import net.countercraft.movecraft.utils.*;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import static net.countercraft.movecraft.utils.MathUtils.withinWorldBorder;

public class TranslationTask extends AsyncTask {
    private static final int[] FALL_THROUGH_BLOCKS = {0, 8, 9, 10, 11, 31, 37, 38, 39, 40, 50, 51, 55, 59, 63, 65, 68, 69, 70, 72, 75, 76, 77, 78, 83, 85, 93, 94, 111, 141, 142, 143, 171};

    private World world;
    private int dx, dy, dz;
    private BitmapHitBox newHitBox, oldHitBox, oldFluidList, newFluidList;
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
        newHitBox = new BitmapHitBox();
        oldHitBox = new BitmapHitBox(c.getHitBox());
        oldFluidList = new BitmapHitBox(c.getFluidLocations());
        newFluidList = new BitmapHitBox();
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
        if (Settings.CraftsUseNetherPortals && craft.getW().getEnvironment() != Environment.THE_END
                && world.equals(craft.getW())) {

            // ensure chunks are loaded for portal checking only if change in location is
            // large
            List<MovecraftChunk> chunksToLoad = ChunkManager.getChunks(oldHitBox, world, dx, dy, dz);
            MovecraftChunk.addSurroundingChunks(chunksToLoad, 2);
            ChunkManager.checkChunks(chunksToLoad);
            if (!chunksToLoad.isEmpty()) {
                ChunkManager.syncLoadChunks(chunksToLoad).get();
            }

            for (MovecraftLocation oldLocation : oldHitBox) {

                Location location = oldLocation.translate(dx, dy, dz).toBukkit(craft.getW());
                Block block = craft.getW().getBlockAt(location);
                if (block.getType() == Material.PORTAL) {

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
        List<MovecraftChunk> chunksToLoad = ChunkManager.getChunks(oldHitBox, craft.getW());
        chunksToLoad.addAll(ChunkManager.getChunks(oldHitBox, world, dx, dy, dz));
        MovecraftChunk.addSurroundingChunks(chunksToLoad, 1);
        ChunkManager.checkChunks(chunksToLoad);
        if (!chunksToLoad.isEmpty()) {
            ChunkManager.syncLoadChunks(chunksToLoad).get();
        }

        // Only modify dy when not switching worlds
        //Check if the craft is too high
        if(world.equals(craft.getW()) && craft.getType().getMaxHeightLimit(craft.getW()) < craft.getHitBox().getMinY()){
            dy = Math.min(dy,-1);
        }else if(world.equals(craft.getW()) && craft.getType().getMaxHeightAboveGround(craft.getW()) > 0){
            final MovecraftLocation middle = oldHitBox.getMidPoint();
            int testY = minY;
            while (testY > 0){
                testY--;
                if (craft.getW().getBlockTypeIdAt(middle.getX(),testY,middle.getZ()) != 0)
                    break;
            }
            if (minY - testY > craft.getType().getMaxHeightAboveGround(world)) {
                dy = Math.min(dy,-1);
            }
        }
        //Process gravity
        if (world.equals(craft.getW()) && craft.getType().getUseGravity() && !craft.getSinking()){
            int incline = inclineCraft(oldHitBox);
            if (incline > 0){
                boolean tooSteep = craft.getType().getGravityInclineDistance() > -1 && incline > craft.getType().getGravityInclineDistance();
                if (tooSteep && craft.getType().getCollisionExplosion() <= 0f) {
                    fail(I18nSupport.getInternationalisedString("Translation - Failed Incline too steep"));
                    return;
                }
                dy = tooSteep ? 0 : incline;
            } else if (!isOnGround(oldHitBox) && craft.getType().getCanHover()){
                MovecraftLocation midPoint = oldHitBox.getMidPoint();
                int centreMinY = oldHitBox.getLocalMinY(midPoint.getX(), midPoint.getZ());
                int groundY = centreMinY;
                World w = craft.getW();
                while (w.getBlockAt(midPoint.getX(), groundY - 1, midPoint.getZ()).getType() == Material.AIR || craft.getType().getPassthroughBlocks().contains(w.getBlockAt(midPoint.getX(), groundY - 1, midPoint.getZ()).getType())){
                    groundY--;
                }
                if (centreMinY - groundY > craft.getType().getHoverLimit()){
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
        if (dy>0 && maxY + dy > craft.getType().getMaxHeightLimit(world) && craft.getType().getCollisionExplosion() <= 0f) {
            fail(I18nSupport.getInternationalisedString("Translation - Failed Craft hit height limit"));
            return;
        } else if (dy>0 && maxY + dy > craft.getType().getMaxHeightLimit(world)) { //If explosive and too high, set dy to 0
            dy = 0;
        } else if (minY + dy < craft.getType().getMinHeightLimit(world) && dy < 0 && !craft.getSinking() && !craft.getType().getUseGravity()) {
            fail(I18nSupport.getInternationalisedString("Translation - Failed Craft hit minimum height limit"));
            return;
        } else if (minY + dy < craft.getType().getMinHeightLimit(world) && dy < 0 && craft.getType().getUseGravity()) {
            //if a craft using gravity hits the minimum height limit, set dy = 0 instead of failing
            dy = 0;
        }

        //TODO: Check fuel
        if (!(dy < 0 && dx == 0 && dz == 0) && !checkFuel()) {
            fail(I18nSupport.getInternationalisedString("Translation - Failed Craft out of fuel"));
            return;
        }


        //TODO: Add and handle event for towny and factions
        final List<Material> harvestBlocks = craft.getType().getHarvestBlocks();
        final List<MovecraftLocation> harvestedBlocks = new ArrayList<>();
        final List<Material> harvesterBladeBlocks = craft.getType().getHarvesterBladeBlocks();
        final BitmapHitBox collisionBox = new BitmapHitBox();
        for(MovecraftLocation oldLocation : oldHitBox){
            final MovecraftLocation newLocation = oldLocation.translate(dx,dy,dz);
            //If the new location already exists in the old hitbox than this is unnecessary because a craft can't hit
            //itself
            if(world.equals(craft.getW()) && oldHitBox.contains(newLocation)){
                newHitBox.add(newLocation);
                continue;
            }
            final Material testMaterial = newLocation.toBukkit(world).getBlock().getType();

            if ((testMaterial.equals(Material.CHEST) || testMaterial.equals(Material.TRAPPED_CHEST)) && checkChests(testMaterial, newLocation)) {
                //prevent chests collision
                fail(String.format(I18nSupport.getInternationalisedString("Translation - Failed Craft is obstructed") + " @ %d,%d,%d,%s", newLocation.getX(), newLocation.getY(), newLocation.getZ(), newLocation.toBukkit(craft.getW()).getBlock().getType().toString()));
                return;
            }
            if (!withinWorldBorder(world, newLocation)) {
                fail(I18nSupport.getInternationalisedString("Translation - Failed Craft cannot pass world border") + String.format(" @ %d,%d,%d", newLocation.getX(), newLocation.getY(), newLocation.getZ()));
                return;
            }

            boolean blockObstructed;
            if (craft.getSinking()) {
                blockObstructed = !(Arrays.binarySearch(FALL_THROUGH_BLOCKS, testMaterial.getId()) >= 0);
            } else {
                blockObstructed = !craft.getType().getPassthroughBlocks().contains(testMaterial) && !testMaterial.equals(Material.AIR);
            }

            boolean ignoreBlock = false;
            // air never obstructs anything (changed 4/18/2017 to prevent drilling machines)
            if (oldLocation.toBukkit(craft.getW()).getBlock().getType().equals(Material.AIR) && blockObstructed) {
                ignoreBlock = true;
            }

            if (blockObstructed && !harvestBlocks.isEmpty() && harvestBlocks.contains(testMaterial)) {
                Material tmpType = oldLocation.toBukkit(craft.getW()).getBlock().getType();
                if (harvesterBladeBlocks.size() > 0 && harvesterBladeBlocks.contains(tmpType)) {
                    blockObstructed = false;
                    harvestedBlocks.add(newLocation);
                }
            }



            if (blockObstructed) {
                if (!craft.getSinking() && craft.getType().getCollisionExplosion() == 0.0F) {
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

        if (craft.getType().getForbiddenHoverOverBlocks().size() > 0){
            MovecraftLocation test = new MovecraftLocation(newHitBox.getMidPoint().getX(), newHitBox.getMinY(), newHitBox.getMidPoint().getZ());
            test = test.translate(0, -1, 0);
            while (test.toBukkit(world).getBlock().getType() == Material.AIR){
                test = test.translate(0, -1, 0);
            }
            Material testType = test.toBukkit(world).getBlock().getType();
            if (craft.getType().getForbiddenHoverOverBlocks().contains(testType)){
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
                if(location.toBukkit(craft.getW()).getBlock().getType() == Material.AIR){
                    air.add(location.translate(dx,dy,dz));
                }
            }
            newHitBox.removeAll(air);
            for(MovecraftLocation location : collisionBox){
                if (craft.getType().getExplodeOnCrash() > 0.0F) {
                    if (System.currentTimeMillis() - craft.getOrigPilotTime() <= 1000) {
                        continue;
                    }
                    Location loc = location.toBukkit(craft.getW());
                    if (!loc.getBlock().getType().equals(Material.AIR)  && ThreadLocalRandom.current().nextDouble(1) < .05) {
                        updates.add(new ExplosionUpdateCommand( loc, craft.getType().getExplodeOnCrash()));
                        collisionExplosion = true;
                    }
                }
                BitmapHitBox toRemove = new BitmapHitBox();
                MovecraftLocation next = location.translate(-dx,-dy,-dz);
                while(oldHitBox.contains(next)) {
                    toRemove.add(next);
                    next = next.translate(0,1,0);
                }
                craft.getCollapsedHitBox().addAll(toRemove);
                newHitBox.removeAll(toRemove);
            }
        } else if ((craft.getType().getCollisionExplosion() != 0.0F) && System.currentTimeMillis() - craft.getOrigPilotTime() > Settings.CollisionPrimer) {
            for(MovecraftLocation location : collisionBox) {
                float explosionForce = craft.getType().getCollisionExplosion();
                if (craft.getType().getFocusedExplosion()) {
                    explosionForce *= Math.min(oldHitBox.size(), craft.getType().getMaxSize());
                }
                //TODO: Account for underwater explosions
                /*if (location.getY() < waterLine) { // underwater explosions require more force to do anything
                    explosionForce += 25;//TODO: find the correct amount
                }*/
                Location oldLocation = location.translate(-dx,-dy,-dz).toBukkit(craft.getW());
                Location newLocation = location.toBukkit(world);
                if (!oldLocation.getBlock().getType().equals(Material.AIR)) {
                    CraftCollisionExplosionEvent e = new CraftCollisionExplosionEvent(craft, newLocation, craft.getW());
                    Bukkit.getServer().getPluginManager().callEvent(e);
                    updates.add(new ExplosionUpdateCommand(newLocation, explosionForce));
                    collisionExplosion = true;
                }
                if (craft.getType().getFocusedExplosion()) { // don't handle any further collisions if it is set to focusedexplosion
                    break;
                }
            }
        }

        if(!collisionBox.isEmpty() && craft.getType().getCruiseOnPilot()){
            CraftManager.getInstance().removeCraft(craft, CraftReleaseEvent.Reason.EMPTY);
            for(MovecraftLocation location : oldHitBox){
                Pair<Material, Byte> phaseBlock = craft.getPhaseBlocks().getOrDefault(location.toBukkit(craft.getW()), new Pair<>(Material.AIR, (byte) 0));
                updates.add(new BlockCreateCommand(craft.getW(), location, phaseBlock.getLeft(), phaseBlock.getRight()));
            }
            newHitBox = new BitmapHitBox();
        }

        if(!collisionBox.isEmpty()){
            Bukkit.getServer().getPluginManager().callEvent(new CraftCollisionEvent(craft, collisionBox, world));
        }

        updates.add(new CraftTranslateCommand(craft, new MovecraftLocation(dx, dy, dz), world));

        //prevents torpedo and rocket pilots
        if (craft.getType().getMoveEntities() && !(craft.getSinking() && craft.getType().getOnlyMovePlayers())) {
            Location midpoint = new Location(
                    craft.getW(),
                    (oldHitBox.getMaxX() + oldHitBox.getMinX())/2.0,
                    (oldHitBox.getMaxY() + oldHitBox.getMinY())/2.0,
                    (oldHitBox.getMaxZ() + oldHitBox.getMinZ())/2.0);
            for (Entity entity : craft.getW().getNearbyEntities(midpoint, oldHitBox.getXLength() / 2.0 + 1, oldHitBox.getYLength() / 2.0 + 2, oldHitBox.getZLength() / 2.0 + 1)) {
                if (entity.getType() == EntityType.PLAYER) {
                    if(craft.getSinking()){
                        continue;
                    }
                    EntityUpdateCommand eUp = new EntityUpdateCommand(entity, dx, dy, dz, 0, 0, world, sound, volume);
                    updates.add(eUp);
                } else if (!craft.getType().getOnlyMovePlayers() || entity.getType() == EntityType.PRIMED_TNT) {
                    EntityUpdateCommand eUp = new EntityUpdateCommand(entity, dx, dy, dz, 0, 0, world);
                    updates.add(eUp);
                }
            }
        } else {
            //add releaseTask without playermove to manager
            if (!craft.getType().getCruiseOnPilot() && !craft.getSinking())  // not necessary to release cruiseonpilot crafts, because they will already be released
                CraftManager.getInstance().addReleaseTask(craft);
        }
        captureYield(harvestedBlocks);
    }

    private static HitBox translateHitBox(HitBox hitBox, MovecraftLocation shift){
        MutableHitBox output = new HashHitBox();
        for(MovecraftLocation location : hitBox){
            output.add(location.add(shift));
        }
        return output;
    }

    private void fail(@NotNull String failMessage) {
        fail(failMessage, true);
    }

    private void fail(@NotNull String message, boolean playSound) {
        failed=true;
        failMessage=message;
        Player craftPilot = CraftManager.getInstance().getPlayerFromCraft(craft);
        if (craftPilot == null) {
            return;
        }
        Location location = craftPilot.getLocation();
        if (craft.getDisabled()) {
            craft.getW().playSound(location, Sound.ENTITY_IRONGOLEM_DEATH, 5.0f, 5.0f);
            return;
        }
        if (!playSound) {
            return;
        }
        craft.getW().playSound(location, craft.getType().getCollisionSound(), 1.0f, 0.25f);
    }

    private static final MovecraftLocation[] SHIFTS = {
            new MovecraftLocation(1,0,0),
            new MovecraftLocation(-1,0,0),
            new MovecraftLocation(0,0,1),
            new MovecraftLocation(0,0,-1)};

    private boolean checkChests(Material mBlock, MovecraftLocation newLoc) {
        for(MovecraftLocation shift : SHIFTS){
            MovecraftLocation aroundNewLoc = newLoc.add(shift);
            Material testMaterial = craft.getW().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
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
            Block block = craft.getW().getBlockAt(loc.getX(), loc.getY(), loc.getZ());
            if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST)
                chests.add(((InventoryHolder) (block.getState())).getInventory());
        }

        for (MovecraftLocation harvestedBlock : harvestedBlocks) {
            Block block = craft.getW().getBlockAt(harvestedBlock.getX(), harvestedBlock.getY(), harvestedBlock.getZ());
            List<ItemStack> drops = new ArrayList<>(block.getDrops());
            //generate seed drops
            if (block.getType() == Material.CROPS) {
                Random rand = new Random();
                int amount = rand.nextInt(4);
                if (amount > 0) {
                    ItemStack seeds = new ItemStack(Material.SEEDS, amount);
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
            final ItemHarvestEvent harvestEvent = new ItemHarvestEvent(craft, drops, harvestedBlock.toBukkit(craft.getW()));
            Bukkit.getServer().getPluginManager().callEvent(harvestEvent);
            for (ItemStack drop : drops) {
                ItemStack retStack = putInToChests(drop, chests);
                if (retStack != null)
                    //drop items on position
                    updates.add(new ItemDropUpdateCommand(new Location(craft.getW(), harvestedBlock.getX(), harvestedBlock.getY(), harvestedBlock.getZ()), retStack));
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
        } while (testMaterial == Material.PORTAL);
        portalNegCorner.setX(testX + portalX);
        portalNegCorner.setZ(testZ + portalZ);

        testX = block.getX();
        testZ = block.getZ();

        // find highest x or z
        do {
            testX += portalX;
            testZ += portalZ;
            testMaterial = block.getWorld().getBlockAt(testX, testY, testZ).getType();
        } while (testMaterial == Material.PORTAL);
        portalPosCorner.setX(testX - portalX);
        portalPosCorner.setZ(testZ - portalZ);

        testX = block.getX();
        testZ = block.getZ();

        // find lowest y
        do {
            testY -= 1;
            testMaterial = block.getWorld().getBlockAt(testX, testY, testZ).getType();
        } while (testMaterial == Material.PORTAL);
        portalNegCorner.setY(testY + 1);

        testY = block.getY();

        // find highest y
        do {
            testY += 1;
            testMaterial = block.getWorld().getBlockAt(testX, testY, testZ).getType();
        } while (testMaterial == Material.PORTAL);
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

        String worldName = craft.getW().getName();
        double scaleFactor = 1.0;
        if (craft.getW().getEnvironment() == Environment.NETHER) { // if in nether
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
            testType = surfaceLoc.toBukkit(craft.getW()).getBlock().getType();
        } while ((testType != Material.AIR &&
                !craft.getType().getPassthroughBlocks().contains(testType) &&
                !oldHitBox.contains(surfaceLoc)) &&
                surfaceLoc.getY() + 1 > craft.getType().getMaxHeightLimit(craft.getW()));
        return surfaceLoc;
    }

    private int inclineCraft(BitmapHitBox hitBox){
        if (isOnGround(hitBox) && dy < 0){
            dy = 0;
        }
        BitmapHitBox collisionBox = new BitmapHitBox();
        for (MovecraftLocation ml : hitBox){
            MovecraftLocation nl = ml.translate(dx, dy, dz);
            if (hitBox.contains(nl))
                continue;
            collisionBox.add(nl);
        }

        int elevation = 0;
        for (MovecraftLocation ml : collisionBox){
            Material testType = ml.toBukkit(craft.getW()).getBlock().getType();
            if (testType == Material.AIR ||
                    craft.getType().getPassthroughBlocks().contains(testType) ||
                    (craft.getType().getHarvestBlocks().contains(testType) &&
                            craft.getType().getHarvesterBladeBlocks().contains(ml.translate(-dx, -dy, -dz).toBukkit(craft.getW()).getBlock().getType()))) {
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
        BitmapHitBox movedCollBox = new BitmapHitBox();
        for (MovecraftLocation ml : collisionBox) {
            movedCollBox.add(ml.translate(0, elevation, 0));

        }
        return movedCollBox.getMinY() - hitBox.getMinY();
    }

    private int dropDistance (BitmapHitBox hitBox) {
        MutableHitBox bottomLocs = new BitmapHitBox();
        MovecraftLocation corner1 = new MovecraftLocation(hitBox.getMinX(), 0, hitBox.getMinZ());
        MovecraftLocation corner2 = new MovecraftLocation(hitBox.getMaxX(), 0, hitBox.getMaxZ());
        for(MovecraftLocation location : new SolidHitBox(corner1, corner2)){
            int test = hitBox.getLocalMinY(location.getX(), location.getZ());
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
                Material testType = dropped.toBukkit(craft.getW()).getBlock().getType();
                hitGround = testType != Material.AIR &&
                        !craft.getType().getPassthroughBlocks().contains(testType) &&
                        !(craft.getType().getHarvestBlocks().contains(testType) &&
                        craft.getType().getHarvesterBladeBlocks().contains(ml.toBukkit(craft.getW()).getBlock().getType())) &&
                        !hitBox.contains(dropped) ||
                        craft.getType().getMinHeightLimit(craft.getW()) == translated.translate(0, dropDistance + 1 , 0).getY();

                if (hitGround) {
                    break;
                }
            }
            if (hitGround) {
                break;
            }
            dropDistance--;

        } while (dropDistance > craft.getType().getGravityDropDistance());

        return dropDistance;
    }

    private boolean isOnGround(BitmapHitBox hitBox){
        MutableHitBox bottomLocs = new BitmapHitBox();
        MutableHitBox translatedBottomLocs = new BitmapHitBox();
        if (hitBox.getMinY() <= craft.getType().getMinHeightLimit(craft.getW())) {
            return true;
        }
        MovecraftLocation corner1 = new MovecraftLocation(hitBox.getMinX(), 0, hitBox.getMinZ());
        MovecraftLocation corner2 = new MovecraftLocation(hitBox.getMaxX(), 0, hitBox.getMaxZ());
        for(MovecraftLocation location : new SolidHitBox(corner1, corner2)){
            int test = hitBox.getLocalMinY(location.getX(), location.getZ());
            if(test == -1){
                continue;
            }
            bottomLocs.add(new MovecraftLocation(location.getX(), test, location.getZ()));
        }

        boolean bottomLocsOnGround = false;
        for (MovecraftLocation bottomLoc : bottomLocs){
            translatedBottomLocs.add(bottomLoc.translate(dx, dy, dz));
            Material testType = bottomLoc.translate(0, -1, 0).toBukkit(craft.getW()).getBlock().getType();
            //If the lowest part of the bottom locs touch the ground, return true anyways
            if (testType == Material.AIR){
                continue;
            } else if (craft.getType().getPassthroughBlocks().contains(testType)) {
                continue;
            } else if (craft.getType().getHarvestBlocks().contains(testType) && craft.getType().getHarvesterBladeBlocks().contains(bottomLoc.toBukkit(craft.getW()).getBlock().getType())) {
                continue;
            }

            bottomLocsOnGround = true;
        }
        boolean translatedBottomLocsInAir = true;
        for (MovecraftLocation translatedBottomLoc : translatedBottomLocs){
            MovecraftLocation beneath = translatedBottomLoc.translate(0, -1, 0);
            Material testType = beneath.toBukkit(craft.getW()).getBlock().getType();
            final CraftType type = craft.getType();
            if (hitBox.contains(beneath) ||
                    bottomLocs.contains(beneath) ||
                    testType == Material.AIR ||
                    type.getPassthroughBlocks().contains(testType) ||
                    (type.getHarvestBlocks().contains(testType) && type.getHarvesterBladeBlocks().contains(translatedBottomLoc.translate(-dx, -dy, -dz).toBukkit(craft.getW()).getBlock().getType()))){
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

    public BitmapHitBox getNewHitBox() {
        return newHitBox;
    }

    public BitmapHitBox getNewFluidList() {
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
