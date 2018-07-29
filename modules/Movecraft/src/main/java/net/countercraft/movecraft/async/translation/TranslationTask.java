package net.countercraft.movecraft.async.translation;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftTranslateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.update.*;
import net.countercraft.movecraft.utils.HashHitBox;
import org.bukkit.Bukkit;
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
    protected void excecute() {
        //Check if theres anything to move
        if(oldHitBox.isEmpty()){
            return;
        }
        final int minY = oldHitBox.getMinY();
        final int maxY = oldHitBox.getMaxY();

        //Check if the craft is too high
        if(craft.getType().getMaxHeightLimit() < craft.getHitBox().getMinY()){
            dy-=1;
        }else if(craft.getType().getMaxHeightAboveGround() > 0){
            final MovecraftLocation middle = oldHitBox.getMidPoint();
            int testY = minY;
            while (testY > 0){
                testY -= 1;
                if (craft.getW().getBlockTypeIdAt(middle.getX(),testY,middle.getZ()) != 0)
                    break;
            }
            if (minY - testY > craft.getType().getMaxHeightAboveGround()) {
                dy -= 1;
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

        //call event
        Bukkit.getServer().getPluginManager().callEvent(new CraftTranslateEvent(craft, oldHitBox, newHitBox));

        if(craft.getSinking()){
            for(MovecraftLocation location : collisionBox){
                if (craft.getType().getExplodeOnCrash() != 0.0F) {
                    if (System.currentTimeMillis() - craft.getOrigPilotTime() <= 1000) {
                        continue;
                    }
                    Location loc = location.toBukkit(craft.getW());
                    if (!loc.getBlock().getType().equals(Material.AIR)) {
                        updates.add(new ExplosionUpdateCommand( loc, craft.getType().getExplodeOnCrash()));
                        collisionExplosion=true;
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
            }
        }else{
            for(MovecraftLocation location : collisionBox){
                if (!(craft.getType().getCollisionExplosion() != 0.0F) || System.currentTimeMillis() - craft.getOrigPilotTime() <= 1000) {
                    continue;
                }
                float explosionKey;
                float explosionForce = craft.getType().getCollisionExplosion();
                if (craft.getType().getFocusedExplosion()) {
                    explosionForce *= oldHitBox.size();
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

        updates.add(new CraftTranslateCommand(craft, new MovecraftLocation(dx, dy, dz)));

        //prevents torpedo and rocket pilots
        if (craft.getType().getMoveEntities() && !craft.getSinking()) {
            for(Entity entity : craft.getW().getNearbyEntities(craft.getHitBox().getMidPoint().toBukkit(craft.getW()), craft.getHitBox().getXLength()/2.0 + 1, craft.getHitBox().getYLength()/2.0 + 1, craft.getHitBox().getZLength()/2.0 + 1)){
                if (entity.getType() == EntityType.PLAYER) {
                    Player player = (Player) entity;
                    craft.getMovedPlayers().put(player, System.currentTimeMillis());
                    Location tempLoc = entity.getLocation();

                    tempLoc = tempLoc.add(dx, dy, dz);
                    Location newPLoc = new Location(craft.getW(), tempLoc.getX(), tempLoc.getY(), tempLoc.getZ());
                    newPLoc.setPitch(entity.getLocation().getPitch());
                    newPLoc.setYaw(entity.getLocation().getYaw());

                    EntityUpdateCommand eUp = new EntityUpdateCommand( newPLoc, entity);
                    updates.add(eUp);
                } else if (!craft.getType().getOnlyMovePlayers() || entity.getType() == EntityType.PRIMED_TNT) {
                    Location tempLoc = entity.getLocation();
                    tempLoc = tempLoc.add(dx,dy,dz);
                    EntityUpdateCommand eUp = new EntityUpdateCommand(tempLoc, entity);
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
