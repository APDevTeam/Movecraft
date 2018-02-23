package net.countercraft.movecraft.async.translation;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.api.config.Settings;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.update.BlockCreateCommand;
import net.countercraft.movecraft.mapUpdater.update.CraftTranslateCommand;
import net.countercraft.movecraft.mapUpdater.update.EntityUpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.ExplosionUpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.ItemDropUpdateCommand;
import net.countercraft.movecraft.mapUpdater.update.UpdateCommand;
import net.countercraft.movecraft.utils.BoundingBoxUtils;
import net.countercraft.movecraft.utils.TownyUtils;
import net.countercraft.movecraft.api.utils.TownyWorldHeightLimits;
import net.countercraft.movecraft.utils.WGCustomFlagsUtils;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.mapUpdater.update.*;
import net.countercraft.movecraft.utils.HitBox;
import net.countercraft.movecraft.utils.MathUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@SuppressWarnings("Duplicates")
public class TranslationTask extends AsyncTask {
    private static final int[] fallThroughBlocks = new int[]{0, 8, 9, 10, 11, 31, 37, 38, 39, 40, 50, 51, 55, 59, 63, 65, 68, 69, 70, 72, 75, 76, 77, 78, 83, 85, 93, 94, 111, 141, 142, 143, 171};
    private int dx,dy,dz;
    private boolean failed = false;
    private String failMessage;
    private Collection<UpdateCommand> updates = new HashSet<>();
    private boolean collisionExplosion;
    private final HitBox newHitBox;
    private final HitBox oldHitBox;

    public TranslationTask(Craft c, int dx, int dy, int dz) {
        super(c);
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        newHitBox = new HitBox();
        oldHitBox = new HitBox(c.getHitBox());
    }

    @Override
    protected void excecute() {
        int minY = oldHitBox.getMinY();
        int maxY = oldHitBox.getMaxY();
        int maxX = oldHitBox.getMaxX();
        int maxZ = oldHitBox.getMaxZ();  // safe because if the first x array doesn't have a z array, then it wouldn't be the first x array
        int minX = oldHitBox.getMinX();
        int minZ = oldHitBox.getMinZ();

        boolean waterCraft = craft.getSinking() || !craft.getType().blockedByWater();

        if (craft.getDisabled() && (!craft.getSinking())) {
            fail(I18nSupport.getInternationalisedString("Craft is disabled!"));
        }

        // check the maxheightaboveground limitation, move 1 down if that limit is exceeded
        if (craft.getType().getMaxHeightAboveGround() > 0 && dy >= 0) {
            int x = oldHitBox.getMaxX() + oldHitBox.getMinX();
            x = x >> 1;
            int y = oldHitBox.getMaxY();
            int z = oldHitBox.getMaxZ() + oldHitBox.getMinZ();
            z = z >> 1;
            int cy = oldHitBox.getMinY();
            boolean done = false;
            while (!done) {
                cy = cy - 1;
                if (craft.getW().getBlockTypeIdAt(x, cy, z) != 0)
                    done = true;
                if (cy <= 1)
                    done = true;
            }
            if (y - cy > craft.getType().getMaxHeightAboveGround()) {
                dy=-1;
            }
        }

        // Find the waterline from the surrounding terrain or from the static level in the craft type
        int waterLine = 0;
        if (waterCraft) {
            if (craft.getType().getStaticWaterLevel() != 0) {
                if (waterLine <= maxY + 1) {
                    waterLine = craft.getType().getStaticWaterLevel();
                }
            } else {
                // figure out the water level by examining blocks next to the outer boundaries of the craft
                for (int posY = maxY + 1; (posY >= minY - 1) && (waterLine == 0); posY--) {
                    int numWater = 0;
                    int numAir = 0;
                    int posX;
                    int posZ;
                    posZ = minZ - 1;
                    for (posX = minX - 1; posX <= maxX + 1; posX++) {
                        int typeID = craft.getW().getBlockAt(posX, posY, posZ).getTypeId();
                        if (typeID == 9)
                            numWater++;
                        if (typeID == 0)
                            numAir++;
                    }
                    posZ = maxZ + 1;
                    for (posX = minX - 1; posX <= maxX + 1; posX++) {
                        int typeID = craft.getW().getBlockAt(posX, posY, posZ).getTypeId();
                        if (typeID == 9)
                            numWater++;
                        if (typeID == 0)
                            numAir++;
                    }
                    posX = minX - 1;
                    for (posZ = minZ; posZ <= maxZ; posZ++) {
                        int typeID = craft.getW().getBlockAt(posX, posY, posZ).getTypeId();
                        if (typeID == 9)
                            numWater++;
                        if (typeID == 0)
                            numAir++;
                    }
                    posX = maxX + 1;
                    for (posZ = minZ; posZ <= maxZ; posZ++) {
                        int typeID = craft.getW().getBlockAt(posX, posY, posZ).getTypeId();
                        if (typeID == 9)
                            numWater++;
                        if (typeID == 0)
                            numAir++;
                    }
                    if (numWater > numAir) {
                        waterLine = posY;
                    }
                }
            }

            // now add all the air blocks found within the craft's hitbox immediately above the waterline and below to the craft blocks so they will be translated
            int posY = waterLine + 1;
            for (int posX = minX; posX < maxX; posX++) {
                for (int posZ = minZ; posZ < maxZ; posZ++) {
                    int localMaxY=oldHitBox.getLocalMaxY(posX,posZ);
                    if(localMaxY != -1){
                        if (craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 0 && posY < localMaxY && posY > oldHitBox.getLocalMinY(posX,posZ)) {
                            MovecraftLocation l = new MovecraftLocation(posX, posY, posZ);
                            oldHitBox.add(l);
                        }
                    }
                }
            }
            // dont check the hitbox for the underwater portion. Otherwise open-hulled ships would flood.
            for (posY = waterLine; posY >= minY; posY--) {
                for (int posX = minX; posX < maxX; posX++) {
                    for (int posZ = minZ; posZ < maxZ; posZ++) {
                        if (craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 0) {
                            MovecraftLocation l = new MovecraftLocation(posX, posY, posZ);
                            oldHitBox.add(l);
                        }
                    }
                }
            }
        }

        if(!checkFuel()){
            return;
        }

        //TODO: Add EventListener for towny
        //TODO: Add EventListener for Worlguard
        HitBox collisionBox = new HitBox();
        List<Material> harvestBlocks = craft.getType().getHarvestBlocks();
        List<MovecraftLocation> harvestedBlocks = new ArrayList<>();
        List<MovecraftLocation> destroyedBlocks = new ArrayList<>();
        List<Material> harvesterBladeBlocks = craft.getType().getHarvesterBladeBlocks();
        for(MovecraftLocation oldLocation : oldHitBox){
            MovecraftLocation newLocation = oldLocation.translate(dx, dy, dz);

            if (dy>0 && craft.getType().getMaxHeightLimit()<newLocation.getY()) {
                fail(I18nSupport.getInternationalisedString("Translation - Failed Craft hit height limit"));
                break;
            } else if (newLocation.getY() < craft.getType().getMinHeightLimit() && newLocation.getY() < oldLocation.getY() && !craft.getSinking()) {
                fail(I18nSupport.getInternationalisedString("Translation - Failed Craft hit minimum height limit"));
                break;
            }

            //check for chests around
            Material testMaterial = newLocation.toBukkit(craft.getW()).getBlock().getType();
            if ((testMaterial.equals(Material.CHEST) || testMaterial.equals(Material.TRAPPED_CHEST)) && !checkChests(testMaterial, newLocation)) {
                //prevent chests collision
                fail(String.format(I18nSupport.getInternationalisedString("Translation - Failed Craft is obstructed") + " @ %d,%d,%d,%s", newLocation.getX(), newLocation.getY(), newLocation.getZ(), newLocation.toBukkit(craft.getW()).getBlock().getType().toString()));
                break;
            }
            boolean blockObstructed;
            if (craft.getSinking()) {
                int testID = newLocation.toBukkit(craft.getW()).getBlock().getTypeId();
                blockObstructed = !(Arrays.binarySearch(fallThroughBlocks, testID) >= 0) && !oldHitBox.contains(newLocation);
            } else if (!waterCraft) {
                // New block is not air or a piston head and is not part of the existing ship
                testMaterial = newLocation.toBukkit(craft.getW()).getBlock().getType();
                blockObstructed = (!testMaterial.equals(Material.AIR)) && !oldHitBox.contains(newLocation);
            } else {
                // New block is not air or water or a piston head and is not part of the existing ship
                testMaterial = newLocation.toBukkit(craft.getW()).getBlock().getType();
                blockObstructed = (!testMaterial.equals(Material.AIR) && !testMaterial.equals(Material.STATIONARY_WATER)
                        && !testMaterial.equals(Material.WATER)) && !oldHitBox.contains(newLocation);
            }

            boolean ignoreBlock = false;
            // air never obstructs anything (changed 4/18/2017 to prevent drilling machines)
            if (oldLocation.toBukkit(craft.getW()).getBlock().getType().equals(Material.AIR) && blockObstructed) {
                ignoreBlock = true;
            }

            testMaterial = newLocation.toBukkit(craft.getW()).getBlock().getType();
            if (blockObstructed) {
                if (harvestBlocks.size() > 0) {
                    // New block is not harvested block
                    if (harvestBlocks.contains(testMaterial) && !oldHitBox.contains(newLocation)) {
                        Material tmpType = oldLocation.toBukkit(craft.getW()).getBlock().getType();
                        if (harvesterBladeBlocks.size() > 0) {
                            if (harvesterBladeBlocks.contains(tmpType)) {
                                blockObstructed = false;
                                tryPutToDestroyBox(testMaterial, newLocation, harvestedBlocks, destroyedBlocks);
                                harvestedBlocks.add(newLocation);
                            }
                        }
                    }
                }
            }

            if (blockObstructed) {
                if (!craft.getSinking() && craft.getType().getCollisionExplosion() == 0.0F) {
                    fail(String.format(I18nSupport.getInternationalisedString("Translation - Failed Craft is obstructed") + " @ %d,%d,%d,%s", oldLocation.getX(), oldLocation.getY(), oldLocation.getZ(), oldLocation.toBukkit(craft.getW()).getBlock().getType().toString()));
                    break;
                }
                collisionBox.add(newLocation);
            } else {
                if (!ignoreBlock) {
                    newHitBox.add(newLocation);
                }
            } //END OF: if (blockObstructed)
        }
        for(MovecraftLocation location : collisionBox){
            // handle sinking ship collisions
            if (craft.getSinking()) {
                if (craft.getType().getExplodeOnCrash() != 0.0F) {
                    float explosionKey = craft.getType().getExplodeOnCrash();
                    if (System.currentTimeMillis() - craft.getOrigPilotTime() > 1000){
                        Location loc = location.toBukkit(craft.getW());
                        if (!loc.getBlock().getType().equals(Material.AIR)) {
                            updates.add(new ExplosionUpdateCommand( loc, explosionKey));
                            collisionExplosion=true;
                        }
                    }
                } else {
                    // use the explosion code to clean up the craft, but not with enough force to do anything
                    int explosionKey = 1;
                    Location loc = location.toBukkit(craft.getW());
                    if (!loc.getBlock().getType().equals(Material.AIR)) {
                        updates.add(new ExplosionUpdateCommand(loc, explosionKey));
                        collisionExplosion = true;
                    }
                }
            } else {
                // Explode if the craft is set to have a CollisionExplosion. Also keep moving for spectacular ramming collisions
                if (craft.getType().getCollisionExplosion() != 0.0F) {
                    if (System.currentTimeMillis() - craft.getOrigPilotTime() > 1000) {
                        float explosionKey;
                        float explosionForce = craft.getType().getCollisionExplosion();
                        if (craft.getType().getFocusedExplosion()) {
                            explosionForce = explosionForce * oldHitBox.size();
                        }
                        if (location.getY() < waterLine) { // underwater explosions require more force to do anything
                            explosionForce += 25;//TODO: find the correct amount
                        }
                        explosionKey = explosionForce;
                        Location loc = location.toBukkit(craft.getW());
                        if (!loc.getBlock().getType().equals(Material.AIR)) {
                            updates.add(new ExplosionUpdateCommand(loc, explosionKey));
                            collisionExplosion=true;
                        }
                        if (craft.getType().getFocusedExplosion()) { // don't handle any further collisions if it is set to focusedexplosion
                            break;
                        }
                    }
                }
            }
        }

        if (collisionExplosion) {
            // mark the craft to check for sinking, remove the exploding blocks from the blocklist, and submit the explosions for map update
            for (UpdateCommand m : updates) {
                if(!(m instanceof ExplosionUpdateCommand))
                    continue;
                ExplosionUpdateCommand explosionCommand = (ExplosionUpdateCommand)m;
                MovecraftLocation explosionLocation = MathUtils.bukkit2MovecraftLoc(explosionCommand.getLocation());
                if (oldHitBox.contains(explosionLocation)) {
                    oldHitBox.remove(explosionLocation);
                    if (Settings.FadeWrecksAfter > 0) {
                        int typeID = explosionCommand.getLocation().getBlock().getTypeId();
                        if (typeID != 0 && typeID != 9) {
                            Movecraft.getInstance().blockFadeTimeMap.put(explosionLocation, System.currentTimeMillis());
                            Movecraft.getInstance().blockFadeTypeMap.put(explosionLocation, typeID);
                            if (explosionCommand.getLocation().getY() <= waterLine) {
                                Movecraft.getInstance().blockFadeWaterMap.put(explosionLocation, true);
                            } else {
                                Movecraft.getInstance().blockFadeWaterMap.put(explosionLocation, false);
                            }
                            Movecraft.getInstance().blockFadeWorldMap.put(explosionLocation, craft.getW());
                        }
                    }
                }

                // if the craft is sinking, remove all solid blocks above the one that hit the ground from the craft for smoothing sinking
                if (craft.getSinking() && (craft.getType().getExplodeOnCrash() == 0.0)) {
                    MovecraftLocation location = MathUtils.bukkit2MovecraftLoc(explosionCommand.getLocation());
                    int posy = (int) explosionCommand.getLocation().getY() + 1;
                    int testID = craft.getW().getBlockAt(location.getX(), posy, location.getZ()).getTypeId();
                    while (posy <= maxY && !(Arrays.binarySearch(fallThroughBlocks, testID) >= 0)) {
                        MovecraftLocation testLoc = new MovecraftLocation(location.getX(), posy, location.getZ());
                        if (oldHitBox.contains(testLoc)) {
                            oldHitBox.remove(testLoc);
                            if (Settings.FadeWrecksAfter > 0) {
                                int typeID = craft.getW().getBlockAt(testLoc.getX(), testLoc.getY(), testLoc.getZ()).getTypeId();
                                if (typeID != 0 && typeID != 9) {
                                    Movecraft.getInstance().blockFadeTimeMap.put(testLoc, System.currentTimeMillis());
                                    Movecraft.getInstance().blockFadeTypeMap.put(testLoc, typeID);
                                    if (testLoc.getY() <= waterLine) {
                                        Movecraft.getInstance().blockFadeWaterMap.put(testLoc, true);
                                    } else {
                                        Movecraft.getInstance().blockFadeWaterMap.put(testLoc, false);
                                    }
                                    Movecraft.getInstance().blockFadeWorldMap.put(testLoc, craft.getW());
                                }
                            }
                        }
                        posy++;
                        testID = craft.getW().getBlockAt(location.getX(), posy, location.getZ()).getTypeId();
                    }
                }
            }

            newHitBox.clear();
            newHitBox.addAll(oldHitBox);
            fail(I18nSupport.getInternationalisedString("Translation - Failed Craft is obstructed"));

            if (!craft.getSinking()) {   // FROG changed from ==true, think that was a typo
                if (craft.getType().getSinkPercent() != 0.0) {
                    craft.setLastBlockCheck(0);
                }
                craft.setLastCruisUpdate(System.currentTimeMillis() - 30000);
            }
        }
        if(failed)
            return;
        updates.add(new CraftTranslateCommand(craft, new MovecraftLocation(dx, dy, dz)));

        //prevents torpedo and rocket pilots :)
        if (craft.getType().getMoveEntities() && !craft.getSinking()) {
            // Move entities within the craft
            List<Entity> eList = craft.getW().getEntities();
            /*int numTries = 0;
            while ((eList == null) && (numTries < 100)) {
                try {
                    eList = craft.getW().getEntities();
                } catch (java.util.ConcurrentModificationException e) {
                    numTries++;
                }
            }*/

            for (Entity pTest : eList) {
                //                                if ( MathUtils.playerIsWithinBoundingPolygon( oldHitBox, craft.getMinX(), craft.getMinZ(), MathUtils.bukkit2MovecraftLoc( pTest.getLocation() ) ) ) {
                if (MathUtils.locIsNearCraftFast(craft, MathUtils.bukkit2MovecraftLoc(pTest.getLocation()))) {
                    if (pTest.getType() == EntityType.PLAYER) {
                        Player player = (Player) pTest;
                        craft.getMovedPlayers().put(player, System.currentTimeMillis());
                        Location tempLoc = pTest.getLocation();

//                                        Direct control no longer locks the player in place
//                                       if(craft.getPilotLocked()==true && pTest==CraftManager.getInstance().getPlayerFromCraft(craft)) {
//                                            tempLoc.setX(craft.getPilotLockedX());
//                                            tempLoc.setY(craft.getPilotLockedY());
//                                            tempLoc.setZ(craft.getPilotLockedZ());
//                                        }
                        tempLoc = tempLoc.add(dx, dy, dz);
                        Location newPLoc = new Location(craft.getW(), tempLoc.getX(), tempLoc.getY(), tempLoc.getZ());
                        newPLoc.setPitch(pTest.getLocation().getPitch());
                        newPLoc.setYaw(pTest.getLocation().getYaw());

                        EntityUpdateCommand eUp = new EntityUpdateCommand( newPLoc, pTest);
                        updates.add(eUp);
                        }


                    if (Settings.MoveAllEntities)
						//Move any entity on a craft
                        if ((pTest.getType() != EntityType.DROPPED_ITEM)||(pTest.getType() == EntityType.DROPPED_ITEM)) {
                       		Location tempLoc = pTest.getLocation();
                            tempLoc = tempLoc.add(dx,dy,dz);
                            EntityUpdateCommand eUp = new EntityUpdateCommand(tempLoc, pTest);
                            updates.add(eUp);
                        }
                         

//                                        if(craft.getPilotLocked()==true && pTest==CraftManager.getInstance().getPlayerFromCraft(craft)) {
//                                            craft.setPilotLockedX(tempLoc.getX());
//                                            craft.setPilotLockedY(tempLoc.getY());
//                                            craft.setPilotLockedZ(tempLoc.getZ());
//                                        }
                    }
                    if (pTest.getType() == EntityType.PRIMED_TNT) {
                        Location tempLoc = pTest.getLocation();
                        tempLoc = tempLoc.add(dx,dy,dz);
                        EntityUpdateCommand eUp = new EntityUpdateCommand(tempLoc, pTest);
                        updates.add(eUp);

                    }

                }
            } else {
            //add releaseTask without playermove to manager
            if (!craft.getType().getCruiseOnPilot() && !craft.getSinking()) {  // not necessary to release cruiseonpilot crafts, because they will already be released
                CraftManager.getInstance().addReleaseTask(craft);
            }
        }

        // remove water near sinking crafts
        if (craft.getSinking()) {
            int posX;
            int posY = maxY;
            int posZ;
            if (posY > waterLine) {
                for (posX = minX - 1; posX <= maxX + 1; posX++) {
                    for (posZ = minZ - 1; posZ <= maxZ + 1; posZ++) {
                        if (craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 9 || craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 8) {
                            MovecraftLocation loc = new MovecraftLocation(posX, posY, posZ);
                            updates.add(new BlockCreateCommand(loc, Material.AIR, (byte) 0, craft));
                        }
                    }
                }
            }
            for (posY = maxY + 1; (posY >= minY - 1) && (posY > waterLine); posY--) {
                posZ = minZ - 1;
                for (posX = minX - 1; posX <= maxX + 1; posX++) {
                    if (craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 9 || craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 8) {
                        MovecraftLocation loc = new MovecraftLocation(posX, posY, posZ);
                        updates.add(new BlockCreateCommand(loc, Material.AIR, (byte) 0, craft));
                    }
                }
                posZ = maxZ + 1;
                for (posX = minX - 1; posX <= maxX + 1; posX++) {
                    if (craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 9 || craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 8) {
                        MovecraftLocation loc = new MovecraftLocation(posX, posY, posZ);

                        updates.add(new BlockCreateCommand(loc, Material.AIR, (byte) 0, craft));
                    }
                }
                posX = minX - 1;
                for (posZ = minZ - 1; posZ <= maxZ + 1; posZ++) {
                    if (craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 9 || craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 8) {
                        MovecraftLocation loc = new MovecraftLocation(posX, posY, posZ);
                        updates.add(new BlockCreateCommand(loc, Material.AIR, (byte) 0, craft));
                    }
                }
                posX = maxX + 1;
                for (posZ = minZ - 1; posZ <= maxZ + 1; posZ++) {
                    if (craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 9 || craft.getW().getBlockAt(posX, posY, posZ).getTypeId() == 8) {
                        MovecraftLocation loc = new MovecraftLocation(posX, posY, posZ);
                        updates.add(new BlockCreateCommand(loc, Material.AIR, (byte) 0, craft));
                    }
                }
            }
        }
        captureYield(harvestedBlocks);
    }

    private void fail(String message) {
        failed=true;
        failMessage=message;
        Player craftPilot = CraftManager.getInstance().getPlayerFromCraft(craft);
        if (craftPilot != null) {
            Location location = craftPilot.getLocation();
            if (!craft.getDisabled()) {
                craft.getW().playSound(location, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.25f);
                //craft.setCurTickCooldown(craft.getType().getCruiseTickCooldown());
            } else {
                craft.getW().playSound(location, Sound.ENTITY_IRONGOLEM_DEATH, 5.0f, 5.0f);
                //craft.setCurTickCooldown(craft.getType().getCruiseTickCooldown());
            }
        }
    }

    private boolean checkFuel(){
        // check for fuel, burn some from a furnace if needed. Blocks of coal are supported, in addition to coal and charcoal
        double fuelBurnRate = craft.getType().getFuelBurnRate();
        // going down doesn't require fuel
        if (dy == -1 && dx == 0 && dz == 0)
            fuelBurnRate = 0.0;

        if (fuelBurnRate == 0.0 || craft.getSinking()) {
            return true;
        }
        if (craft.getBurningFuel() >= fuelBurnRate) {
            craft.setBurningFuel(craft.getBurningFuel() - fuelBurnRate);
            return true;
        }
        Block fuelHolder = null;
        for (MovecraftLocation bTest : oldHitBox) {
            Block b = craft.getW().getBlockAt(bTest.getX(), bTest.getY(), bTest.getZ());
            if (b.getTypeId() == 61) {
                InventoryHolder inventoryHolder = (InventoryHolder) b.getState();
                if (inventoryHolder.getInventory().contains(263) || inventoryHolder.getInventory().contains(173)) {
                    fuelHolder = b;
                }
            }
        }
        if (fuelHolder == null) {
            fail(I18nSupport.getInternationalisedString("Translation - Failed Craft out of fuel"));
            return false;
        }
        InventoryHolder inventoryHolder = (InventoryHolder) fuelHolder.getState();
        if (inventoryHolder.getInventory().contains(263)) {
            ItemStack iStack = inventoryHolder.getInventory().getItem(inventoryHolder.getInventory().first(263));
            int amount = iStack.getAmount();
            if (amount == 1) {
                inventoryHolder.getInventory().remove(iStack);
            } else {
                iStack.setAmount(amount - 1);
            }
            craft.setBurningFuel(craft.getBurningFuel() + 7.0);
        } else {
            ItemStack iStack = inventoryHolder.getInventory().getItem(inventoryHolder.getInventory().first(173));
            int amount = iStack.getAmount();
            if (amount == 1) {
                inventoryHolder.getInventory().remove(iStack);
            } else {
                iStack.setAmount(amount - 1);
            }
            craft.setBurningFuel(craft.getBurningFuel() + 79.0);

        }
        return true;
    }

    private void captureYield(List<MovecraftLocation> harvestedBlocks) {
        if (harvestedBlocks.isEmpty()) {
            return;
        }
        ArrayList<Inventory> chests = new ArrayList<>();
        HashSet<ItemDropUpdateCommand> itemDropUpdateSet = new HashSet<>();
        //find chests
        for (MovecraftLocation loc : oldHitBox) {
            Block block = craft.getW().getBlockAt(loc.getX(), loc.getY(), loc.getZ());
            if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST)
                chests.add(((InventoryHolder) (block.getState())).getInventory());
        }

        for (MovecraftLocation harvestedBlock : harvestedBlocks) {
            Block block = craft.getW().getBlockAt(harvestedBlock.getX(), harvestedBlock.getY(), harvestedBlock.getZ());
            ItemStack[] drops = block.getDrops().toArray(new ItemStack[block.getDrops().size()]);
            //generate seed drops
            if (block.getType() == Material.CROPS) {
                Random rand = new Random();
                int amount = rand.nextInt(4);
                if (amount > 0) {
                    ItemStack seeds = new ItemStack(Material.SEEDS, amount);
                    HashSet<ItemStack> d = new HashSet<>(Arrays.asList(drops));
                    d.add(seeds);
                    drops = d.toArray(new ItemStack[d.size()]);
                }
            }
            //get contents of inventories before deposting
            if (block.getState() instanceof InventoryHolder) {
                if (block.getState() instanceof Chest) {
                    //Inventory inv = ((DoubleChest) block.getState()).getRightSide().getInventory().getLocation().equals(block.getLocation()) ?((DoubleChest) block.getState()).getRightSide().getInventory(): ((DoubleChest) block.getState()).getLeftSide().getInventory();
                    //HashSet<ItemStack> d = new HashSet<ItemStack>(Arrays.asList(inv.getContents()));
                    HashSet<ItemStack> d = new HashSet<>(Arrays.asList(((Chest) block.getState()).getBlockInventory().getContents()));
                    d.addAll(block.getDrops());
                    drops = d.toArray(new ItemStack[d.size()]);
                } else {
                    HashSet<ItemStack> d = new HashSet<>(Arrays.asList((((InventoryHolder) block.getState()).getInventory().getContents())));
                    d.addAll(block.getDrops());
                    drops = d.toArray(new ItemStack[d.size()]);
                }
            }
            for (ItemStack drop : drops) {
                ItemStack retStack = putInToChests(drop, chests);
                if (retStack != null)
                    //drop items on position
                    itemDropUpdateSet.add(new ItemDropUpdateCommand(new Location(craft.getW(), harvestedBlock.getX(), harvestedBlock.getY(), harvestedBlock.getZ()), retStack));
            }
        }
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

    private boolean checkChests(Material mBlock, MovecraftLocation newLoc) {
        Material testMaterial;
        MovecraftLocation aroundNewLoc;

        aroundNewLoc = newLoc.translate(1, 0, 0);
        testMaterial = craft.getW().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)) {
            if (!oldHitBox.contains(aroundNewLoc)) {
                return false;
            }
        }

        aroundNewLoc = newLoc.translate(-1, 0, 0);
        testMaterial = craft.getW().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)) {
            if (!oldHitBox.contains(aroundNewLoc)) {
                return false;
            }
        }

        aroundNewLoc = newLoc.translate(0, 0, 1);
        testMaterial = craft.getW().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        if (testMaterial.equals(mBlock)) {
            if (!oldHitBox.contains(aroundNewLoc)) {
                return false;
            }
        }

        aroundNewLoc = newLoc.translate(0, 0, -1);
        testMaterial = craft.getW().getBlockAt(aroundNewLoc.getX(), aroundNewLoc.getY(), aroundNewLoc.getZ()).getType();
        return !testMaterial.equals(mBlock) || oldHitBox.contains(aroundNewLoc);
    }

    private void tryPutToDestroyBox(Material mat, MovecraftLocation loc, List<MovecraftLocation> harvestedBlocks, List<MovecraftLocation> destroyedBlocks) {
        if (mat.equals(Material.DOUBLE_PLANT) || mat.equals(Material.WOODEN_DOOR) || mat.equals(Material.IRON_DOOR_BLOCK)) {
            if (craft.getW().getBlockAt(loc.getX(), loc.getY() + 1, loc.getZ()).getType().equals(mat)) {
                MovecraftLocation tmpLoc = loc.translate(0, 1, 0);
                if (!destroyedBlocks.contains(tmpLoc) && !harvestedBlocks.contains(tmpLoc)) {
                    destroyedBlocks.add(tmpLoc);
                }
            } else if (craft.getW().getBlockAt(loc.getX(), loc.getY() - 1, loc.getZ()).getType().equals(mat)) {
                MovecraftLocation tmpLoc = loc.translate(0, -1, 0);
                if (!destroyedBlocks.contains(tmpLoc) && !harvestedBlocks.contains(tmpLoc)) {
                    destroyedBlocks.add(tmpLoc);
                }
            }
        } else if (mat.equals(Material.CACTUS) || mat.equals(Material.SUGAR_CANE_BLOCK)) {
            MovecraftLocation tmpLoc = loc.translate(0, 1, 0);
            Material tmpType = craft.getW().getBlockAt(tmpLoc.getX(), tmpLoc.getY(), tmpLoc.getZ()).getType();
            while (tmpType.equals(mat)) {
                if (!harvestedBlocks.contains(tmpLoc)) {
                    harvestedBlocks.add(tmpLoc);
                    destroyedBlocks.add(tmpLoc);
                }
                tmpLoc = tmpLoc.translate(0, 1, 0);
                tmpType = craft.getW().getBlockAt(tmpLoc.getX(), tmpLoc.getY(), tmpLoc.getZ()).getType();
            }
        } else if (mat.equals(Material.BED_BLOCK)) {
            if (craft.getW().getBlockAt(loc.getX() + 1, loc.getY(), loc.getZ()).getType().equals(mat)) {
                MovecraftLocation tmpLoc = loc.translate(1, 0, 0);
                if (!destroyedBlocks.contains(tmpLoc) && !harvestedBlocks.contains(tmpLoc)) {
                    destroyedBlocks.add(tmpLoc);
                }
            } else if (craft.getW().getBlockAt(loc.getX() - 1, loc.getY(), loc.getZ()).getType().equals(mat)) {
                MovecraftLocation tmpLoc = loc.translate(-1, 0, 0);
                if (!destroyedBlocks.contains(tmpLoc) && !harvestedBlocks.contains(tmpLoc)) {
                    destroyedBlocks.add(tmpLoc);
                }
            }
            if (craft.getW().getBlockAt(loc.getX(), loc.getY(), loc.getZ() + 1).getType().equals(mat)) {
                MovecraftLocation tmpLoc = loc.translate(0, 0, 1);
                if (!destroyedBlocks.contains(tmpLoc) && !harvestedBlocks.contains(tmpLoc)) {
                    destroyedBlocks.add(tmpLoc);
                }
            } else if (craft.getW().getBlockAt(loc.getX(), loc.getY(), loc.getZ() - 1).getType().equals(mat)) {
                MovecraftLocation tmpLoc = loc.translate(0, 0, -1);
                if (!destroyedBlocks.contains(tmpLoc) && !harvestedBlocks.contains(tmpLoc)) {
                    destroyedBlocks.add(tmpLoc);
                }
            }
        }
        //clear from previous because now it is in harvest
        if (destroyedBlocks.contains(loc)) {
            destroyedBlocks.remove(loc);
        }
    }

    public boolean failed(){
        return failed;
    }

    public String getFailMessage() {
        return failMessage;
    }

    public boolean collisionExplosion(){
        return collisionExplosion;
    }

    public HitBox getNewHitBox() {
        return newHitBox;
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
}
