package net.countercraft.movecraft.async.translation;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftCollisionEvent;
import net.countercraft.movecraft.events.CraftTranslateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.update.*;
import net.countercraft.movecraft.utils.HashHitBox;
import net.countercraft.movecraft.utils.HitBox;
import net.countercraft.movecraft.utils.MutableHitBox;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TranslationTask extends AsyncTask {
    private static final int[] FALL_THROUGH_BLOCKS = {0, 8, 9, 10, 11, 31, 37, 38, 39, 40, 50, 51, 55, 59, 63, 65, 68, 69, 70, 72, 75, 76, 77, 78, 83, 85, 93, 94, 111, 141, 142, 143, 171};

    private int dx, dy, dz;
    private HashHitBox newHitBox, oldHitBox;
    private boolean failed;
    private boolean collisionExplosion = false;
    private String failMessage;
    private Collection<UpdateCommand> updates = new HashSet<>();

    public TranslationTask(Craft c, int dx, int dy, int dz) {
        super(c);
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        newHitBox = new HashHitBox();
        oldHitBox = new HashHitBox(c.getHitBox());
    }

    @Override
    protected void execute() {

        //Check if theres anything to move
        if(oldHitBox.isEmpty()){
            return;
        }
        if (getCraft().getDisabled() && (!getCraft().getSinking())) {
            fail(I18nSupport.getInternationalisedString("Translation - Failed Craft Is Disabled"));
            return;
        }
        final int minY = oldHitBox.getMinY();
        final int maxY = oldHitBox.getMaxY();

        //Check if the craft is too high
        if(craft.getType().getMaxHeightLimit() < craft.getHitBox().getMinY()){
            dy = Math.min(dy,-1);
        }else if(craft.getType().getMaxHeightAboveGround() > 0){
            final MovecraftLocation middle = oldHitBox.getMidPoint();
            int testY = minY;
            while (testY > 0){
                testY--;
                if (craft.getW().getBlockTypeIdAt(middle.getX(),testY,middle.getZ()) != 0)
                    break;
            }
            if (minY - testY > craft.getType().getMaxHeightAboveGround()) {
                dy = Math.min(dy,-1);
            }
        }

        //Fail the movement if the craft is too high
        if (dy>0 && maxY + dy > craft.getType().getMaxHeightLimit()) {
            fail(I18nSupport.getInternationalisedString("Translation - Failed Craft hit height limit"));
            return;
        } else if (minY + dy < craft.getType().getMinHeightLimit() && dy < 0 && !craft.getSinking()) {
            fail(I18nSupport.getInternationalisedString("Translation - Failed Craft hit minimum height limit"));
            return;
        }

        //TODO: Check fuel
        if (!checkFuel()) {
            fail(I18nSupport.getInternationalisedString("Translation - Failed Craft out of fuel"));
            return;
        }
        if (craft.getType().getUseGravity() && !craft.getSinking()){
            if (inclineCraft(oldHitBox)){
                dy = 1;
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
                dy = -1;
            }
        }

        //TODO: Add and handle event for towny and factions
        final List<Material> harvestBlocks = craft.getType().getHarvestBlocks();
        final List<MovecraftLocation> harvestedBlocks = new ArrayList<>();
        final List<Material> harvesterBladeBlocks = craft.getType().getHarvesterBladeBlocks();
        final HashHitBox collisionBox = new HashHitBox();
        for(MovecraftLocation oldLocation : oldHitBox){
            final MovecraftLocation newLocation = oldLocation.translate(dx,dy,dz);
            //If the new location already exists in the old hitbox than this is unnecessary because a craft can't hit
            //itself
            if(oldHitBox.contains(newLocation)){
                newHitBox.add(newLocation);
                continue;
            }
            final Material testMaterial = newLocation.toBukkit(craft.getW()).getBlock().getType();

            if ((testMaterial.equals(Material.CHEST) || testMaterial.equals(Material.TRAPPED_CHEST)) && checkChests(testMaterial, newLocation)) {
                //prevent chests collision
                fail(String.format(I18nSupport.getInternationalisedString("Translation - Failed Craft is obstructed") + " @ %d,%d,%d,%s", newLocation.getX(), newLocation.getY(), newLocation.getZ(), newLocation.toBukkit(craft.getW()).getBlock().getType().toString()));
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

        if (!craft.getType().getCanHoverOverWater()){
            MovecraftLocation test = new MovecraftLocation(oldHitBox.getMidPoint().getX(), oldHitBox.getMinY(), oldHitBox.getMidPoint().getZ()).translate(dx, dy, dz);
            test = test.translate(0, -1, 0);
            while (test.toBukkit(craft.getW()).getBlock().getType().equals(Material.AIR) || craft.getType().getPassthroughBlocks().contains(test.toBukkit(craft.getW()).getBlock().getType())){
                test = test.translate(0, -1, 0);
            }
            Material testType = test.toBukkit(craft.getW()).getBlock().getType();
            if (craft.getType().getPassthroughBlocks().contains(testType)){
                fail(String.format(I18nSupport.getInternationalisedString("Translation - Failed Craft over passthrough block"), testType.name().toLowerCase().replace("_", " ")));
            }
        }
        //call event
        CraftTranslateEvent event = new CraftTranslateEvent(craft, oldHitBox, newHitBox);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if(event.isCancelled()){
            this.fail(event.getFailMessage());
            return;
        }

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
                        collisionExplosion=true;
                    }
                }
                List<MovecraftLocation> toRemove = new ArrayList<>();
                MovecraftLocation next = location;
                do {
                    toRemove.add(next);
                    next = next.add(new MovecraftLocation(0,1,0));
                }while (newHitBox.contains(next));
                craft.getCollapsedHitBox().addAll(toRemove);
                newHitBox.removeAll(toRemove);
            }

        }else{
            for(MovecraftLocation location : collisionBox){
                if (!(craft.getType().getCollisionExplosion() != 0.0F) || System.currentTimeMillis() - craft.getOrigPilotTime() <= 1000) {
                    continue;
                }
                float explosionKey;
                float explosionForce = craft.getType().getCollisionExplosion();
                if (craft.getType().getFocusedExplosion()) {
                    explosionForce *= Math.min(oldHitBox.size(), craft.getType().getMaxSize());
                }
                //TODO: Account for underwater explosions
                /*if (location.getY() < waterLine) { // underwater explosions require more force to do anything
                    explosionForce += 25;//TODO: find the correct amount
                }*/
                explosionKey = explosionForce;
                Location loc = location.toBukkit(craft.getW());
                if (!loc.getBlock().getType().equals(Material.AIR)) {
                    updates.add(new ExplosionUpdateCommand(loc, explosionKey));
                    collisionExplosion = true;
                }
                if (craft.getType().getFocusedExplosion()) { // don't handle any further collisions if it is set to focusedexplosion
                    break;
                }
            }
        }

        if(!collisionBox.isEmpty() && craft.getType().getCruiseOnPilot()){
            CraftManager.getInstance().removeCraft(craft);
            for(MovecraftLocation location : oldHitBox){
                updates.add(new BlockCreateCommand(craft.getW(), location, Material.AIR));
            }
            newHitBox = new HashHitBox();
        }

        if(!collisionBox.isEmpty()){
            Bukkit.getServer().getPluginManager().callEvent(new CraftCollisionEvent(craft, collisionBox));
        }

        updates.add(new CraftTranslateCommand(craft, new MovecraftLocation(dx, dy, dz)));

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
                    EntityUpdateCommand eUp = new EntityUpdateCommand(entity, dx, dy, dz, 0, 0);
                    updates.add(eUp);
                } else if (!craft.getType().getOnlyMovePlayers() || entity.getType() == EntityType.PRIMED_TNT) {
                    EntityUpdateCommand eUp = new EntityUpdateCommand(entity, dx, dy, dz, 0, 0);
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
            for (ItemStack drop : drops) {
                ItemStack retStack = putInToChests(drop, chests);
                if (retStack != null)
                    //drop items on position
                    updates.add(new ItemDropUpdateCommand(new Location(craft.getW(), harvestedBlock.getX(), harvestedBlock.getY(), harvestedBlock.getZ()), retStack));
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

    private boolean inclineCraft(HashHitBox hitBox){
        if (isOnGround(hitBox) && dy < 0){
            dy = 0;
        }
        HashHitBox collisionBox = new HashHitBox();
        for (MovecraftLocation ml : hitBox){
            MovecraftLocation nl = ml.translate(dx, dy, dz);
            if (hitBox.contains(nl))
                continue;
            if (ml.getY() > hitBox.getMinY())
                continue;
            collisionBox.add(nl);
        }
        for (MovecraftLocation ml : collisionBox){
            if (!ml.toBukkit(craft.getW()).getBlock().getType().equals(Material.AIR) && !craft.getType().getPassthroughBlocks().contains(ml.toBukkit(craft.getW()).getBlock().getType()) && !craft.getType().getHarvestBlocks().contains(ml.toBukkit(craft.getW()).getBlock().getType())){
                return true;
            }
        }

        return false;
    }

    private boolean isOnGround(HashHitBox hitBox){
        MutableHitBox bottomLocs = new HashHitBox();
        MutableHitBox translatedBottomLocs = new HashHitBox();
        for (MovecraftLocation location : hitBox){
            MovecraftLocation beneath = location.translate(0, -1, 0);
            //If the hitbox contains the location beneath this one, continue the loop
            if (hitBox.contains(beneath)){
                    continue;
            }
            //If not, check if it is an exterior location
            int y;
            for (y = beneath.getY() ; y >= hitBox.getMinY() ; y--){
                if (hitBox.contains(beneath.getX(), y, beneath.getZ())){
                    break;
                }
            }
            //Continue if the shift was an interior location
            if (y > hitBox.getMinY()){
                continue;
            }
            //Otherwise, add to bottom locations
            bottomLocs.add(location);
        }
        boolean bottomLocsOnGround = false;
        for (MovecraftLocation bottomLoc : bottomLocs){
            translatedBottomLocs.add(bottomLoc.translate(dx, dy, dz));
            Material testType = bottomLoc.translate(0, -1, 0).toBukkit(craft.getW()).getBlock().getType();
            //If the lowest part of the bottom locs touch the ground, return true anyways
            if (testType != Material.AIR && !craft.getType().getPassthroughBlocks().contains(testType)){
                bottomLocsOnGround = true;
            }
        }
        boolean translatedBottomLocsInAir = true;
        for (MovecraftLocation translatedBottomLoc : translatedBottomLocs){
            MovecraftLocation beneath = translatedBottomLoc.translate(0, -1, 0);
            Material testType = beneath.toBukkit(craft.getW()).getBlock().getType();
            if (bottomLocs.contains(beneath) || testType == Material.AIR || craft.getType().getPassthroughBlocks().contains(testType)){
                continue;
            }
            translatedBottomLocsInAir = false;
        }
        Bukkit.broadcastMessage("Bottom locs on ground: " + bottomLocsOnGround);
        Bukkit.broadcastMessage("translated Bottom Locs In Air: " + translatedBottomLocsInAir);
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

    public HashHitBox getNewHitBox() {
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

    public boolean isCollisionExplosion() {
        return collisionExplosion;
    }
}
