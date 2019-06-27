package net.countercraft.movecraft.async.translation;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftTranslateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.update.*;
import net.countercraft.movecraft.utils.HashHitBox;
import net.countercraft.movecraft.utils.HitBox;
import net.countercraft.movecraft.utils.LegacyUtils;
import net.countercraft.movecraft.utils.MutableHitBox;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TranslationTask extends AsyncTask {
    private static final Set<Material> FALL_THROUGH_BLOCKS = new HashSet<>();//Settings.IsLegacy ? new Material[]{}:new Material[]{Material.AIR,
            /*Material.WATER,
            Material.LAVA,
            Material.BROWN_MUSHROOM,
            Material.RED_MUSHROOM,
            Material.TORCH,
            Material.FIRE,
            Material.REDSTONE_WIRE,
            Material.LADDER,
            Material.WALL_SIGN,
            Material.LEVER,
            Material.STONE_BUTTON,
            Material.SNOW,
            Material.CARROT,
            Material.POTATO,
            };*/
    //private static final int[] FALL_THROUGH_BLOCKS = {0, 8, 9, 10, 11, 31, 37, 38, 39, 40, 50, 51, 55, 59, 63, 65, 68, 69, 70, 72, 75, 76, 77, 78, 83, 85, 93, 94, 111, 141, 142, 143, 171};

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

        FALL_THROUGH_BLOCKS.add(Material.AIR);
        FALL_THROUGH_BLOCKS.add(Material.WATER);
        FALL_THROUGH_BLOCKS.add(Material.LAVA);
        FALL_THROUGH_BLOCKS.add(Material.BROWN_MUSHROOM);
        FALL_THROUGH_BLOCKS.add(Material.RED_MUSHROOM);
        FALL_THROUGH_BLOCKS.add(Material.TORCH);
        FALL_THROUGH_BLOCKS.add(Material.FIRE);
        FALL_THROUGH_BLOCKS.add(Material.REDSTONE_WIRE);
        FALL_THROUGH_BLOCKS.add(Material.LADDER);
        if (Settings.is1_14){
            FALL_THROUGH_BLOCKS.add(Material.BIRCH_SIGN);
            FALL_THROUGH_BLOCKS.add(Material.OAK_SIGN);
            FALL_THROUGH_BLOCKS.add(Material.DARK_OAK_SIGN);
            FALL_THROUGH_BLOCKS.add(Material.JUNGLE_SIGN);
            FALL_THROUGH_BLOCKS.add(Material.SPRUCE_SIGN);
            FALL_THROUGH_BLOCKS.add(Material.ACACIA_SIGN);
            FALL_THROUGH_BLOCKS.add(Material.BIRCH_WALL_SIGN);
            FALL_THROUGH_BLOCKS.add(Material.OAK_WALL_SIGN);
            FALL_THROUGH_BLOCKS.add(Material.DARK_OAK_WALL_SIGN);
            FALL_THROUGH_BLOCKS.add(Material.JUNGLE_WALL_SIGN);
            FALL_THROUGH_BLOCKS.add(Material.SPRUCE_WALL_SIGN);
            FALL_THROUGH_BLOCKS.add(Material.ACACIA_WALL_SIGN);
        } else {
            FALL_THROUGH_BLOCKS.add(Material.getMaterial("SIGN"));
            FALL_THROUGH_BLOCKS.add(Material.getMaterial("WALL_SIGN"));
        }
        FALL_THROUGH_BLOCKS.add(Material.LEVER);
        FALL_THROUGH_BLOCKS.add(Material.STONE_BUTTON);
        FALL_THROUGH_BLOCKS.add(Material.SNOW);
        FALL_THROUGH_BLOCKS.add(Material.CARROT);
        FALL_THROUGH_BLOCKS.add(Material.POTATO);
        if (Settings.IsLegacy) {
            FALL_THROUGH_BLOCKS.add(LegacyUtils.STATIONARY_WATER);
            FALL_THROUGH_BLOCKS.add(LegacyUtils.STATIONARY_LAVA);
            FALL_THROUGH_BLOCKS.add(LegacyUtils.LONG_GRASS);
            FALL_THROUGH_BLOCKS.add(LegacyUtils.YELLOW_FLOWER);
            FALL_THROUGH_BLOCKS.add(LegacyUtils.RED_ROSE);
            FALL_THROUGH_BLOCKS.add(LegacyUtils.CROPS);
            FALL_THROUGH_BLOCKS.add(LegacyUtils.SIGN_POST);
            FALL_THROUGH_BLOCKS.add(LegacyUtils.STONE_PLATE);
            FALL_THROUGH_BLOCKS.add(LegacyUtils.REDSTONE_TORCH_ON);
            FALL_THROUGH_BLOCKS.add(LegacyUtils.REDSTONE_TORCH_OFF);
            FALL_THROUGH_BLOCKS.add(LegacyUtils.SUGAR_CANE_BLOCK);
            FALL_THROUGH_BLOCKS.add(LegacyUtils.FENCE);
            FALL_THROUGH_BLOCKS.add(LegacyUtils.DIODE_BLOCK_OFF);
            FALL_THROUGH_BLOCKS.add(LegacyUtils.DIODE_BLOCK_ON);
            FALL_THROUGH_BLOCKS.add(LegacyUtils.WATER_LILY);
            FALL_THROUGH_BLOCKS.add(LegacyUtils.WOOD_BUTTON);
            FALL_THROUGH_BLOCKS.add(LegacyUtils.CARPET);
        } else {
            FALL_THROUGH_BLOCKS.add(Material.BUBBLE_COLUMN);
            FALL_THROUGH_BLOCKS.add(Material.KELP);
            FALL_THROUGH_BLOCKS.add(Material.KELP_PLANT);
            FALL_THROUGH_BLOCKS.add(Material.SEAGRASS);
            FALL_THROUGH_BLOCKS.add(Material.TALL_SEAGRASS);

        }
    }

    @Override
    protected void execute() {

        //Check if theres anything to move
        if(oldHitBox.isEmpty()){
            return;
        }
        if (craft.getType().getFuelBurnRate() > 0.0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    // check for fuel, burn some from a furnace if needed. Blocks of coal are supported, in addition to coal and charcoal
                    double fuelBurnRate = craft.getType().getFuelBurnRate();
                    // going down doesn't require fuel
                    if (dy == -1 && dx == 0 && dz == 0)
                        fuelBurnRate = 0.0;
                    if (dy > 0)
                        fuelBurnRate *= 2;
                    if (fuelBurnRate == 0.0 || craft.getSinking()) {
                        return;
                    }
                    if (craft.getBurningFuel() >= fuelBurnRate) {
                        craft.setBurningFuel(craft.getBurningFuel() - fuelBurnRate);
                        return;
                    }
                    Block fuelHolder = null;
                    for (MovecraftLocation bTest : craft.getHitBox()) {
                        Block b = craft.getW().getBlockAt(bTest.getX(), bTest.getY(), bTest.getZ());
                        //Get all fuel holders
                        if (b.getType() == Material.FURNACE) {
                            InventoryHolder holder = (InventoryHolder) b.getState();
                            for (Material fuel : Settings.FuelTypes.keySet()) {
                                if (holder.getInventory().contains(fuel)) {
                                    fuelHolder = b;
                                    break;
                                }
                            }
                        }
                        if (fuelHolder != null) break;

                    }
                    if (fuelHolder == null) {
                        fail(I18nSupport.getInternationalisedString("Translation - Failed Craft out of fuel"));
                        return;
                    }
                    Furnace furnace = (Furnace) fuelHolder.getState();
                    for (Material fuel : Settings.FuelTypes.keySet()) {
                        if (furnace.getInventory().contains(fuel)) {
                            ItemStack item = furnace.getInventory().getItem(furnace.getInventory().first(fuel));
                            int amount = item.getAmount();
                            if (amount == 1) {
                                furnace.getInventory().remove(item);
                            } else {
                                item.setAmount(amount - 1);
                            }
                            craft.setBurningFuel(craft.getBurningFuel() + Settings.FuelTypes.get(item.getType()));
                        }
                    }
                }
            }.runTask(Movecraft.getInstance());
        }
        //Prevent disabled crafts from moving
        if (getCraft().getDisabled() && (!getCraft().getSinking())) {
            fail(I18nSupport.getInternationalisedString("Craft is disabled!"));
            return;
        }

        if (getCraft().isRepairing()){
            fail("You cannot move a repairing craft!");
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
            for (int y = minY;y > 0;y--) {
                if (y == minY)
                    continue;
                if (craft.getW().getBlockAt(middle.getX(), y, middle.getZ()).getType() == Material.AIR || !Settings.IsLegacy && (craft.getW().getBlockAt(middle.getX(), y, middle.getZ()).getType() == Material.CAVE_AIR || craft.getW().getBlockAt(middle.getX(), y, middle.getZ()).getType() == Material.VOID_AIR)){
                    continue;
                }
                testY = y;
                break;
            }
            if (minY - testY > craft.getType().getMaxHeightAboveGround()) {
                dy = 0;
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
        //if (!checkFuel()) {
            //fail(I18nSupport.getInternationalisedString("Translation - Failed Craft out of fuel"));
            //return;
        //}

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
                blockObstructed = !FALL_THROUGH_BLOCKS.contains(testMaterial);
            } else {
                if (Settings.IsLegacy) {
                    blockObstructed = !craft.getType().getPassthroughBlocks().contains(testMaterial) && !testMaterial.equals(Material.AIR);
                } else { //1.13 has more than just one air type
                    blockObstructed = !craft.getType().getPassthroughBlocks().contains(testMaterial) && (!testMaterial.toString().endsWith("AIR"));
                }
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
        CraftTranslateEvent event = new CraftTranslateEvent(craft, oldHitBox, newHitBox);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if(event.isCancelled()){
            this.fail(event.getFailMessage());
            return;
        }

        if(craft.getSinking()){
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
            Bukkit.getLogger().info("Collapsed hitbox: "+craft.getCollapsedHitBox().size() + ", New hitbox: " + newHitBox.size());
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
                int potEffRange = craft.getType().getEffectRange();
                Map<PotionEffect,Integer> potionEffects = craft.getType().getPotionEffectsToApply();
                Set<EntityType> entityTypes = craft.getType().getEntitiesToSpawn();
                int maxEntitiesToBeSpawned = craft.getType().getMaxEntitiesToBeSpawned();
                if (!loc.getBlock().getType().equals(Material.AIR)) {
                    updates.add(new ExplosionUpdateCommand(loc, explosionKey));
                    collisionExplosion = true;
                    if (potEffRange != 0 && !potionEffects.isEmpty())
                        updates.add(new PotionEffectsUpdateCommand(loc,potEffRange,potionEffects));
                    if (potEffRange != 0 && !entityTypes.isEmpty()){
                        updates.add(new EntitySpawnUpdateCommand(potEffRange,maxEntitiesToBeSpawned,loc,entityTypes));
                    }
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

        updates.add(new CraftTranslateCommand(craft, new MovecraftLocation(dx, dy, dz)));

        //prevents torpedo and rocket pilots
        if (craft.getType().getMoveEntities() && !(craft.getSinking() && craft.getType().getOnlyMovePlayers())) {
            for (Entity entity : craft.getW().getNearbyEntities(craft.getHitBox().getMidPoint().toBukkit(craft.getW()), craft.getHitBox().getXLength() / 2.0 + 1, craft.getHitBox().getYLength() / 2.0 + 1, craft.getHitBox().getZLength() / 2.0 + 1)) {
                if (entity.getType() == EntityType.PLAYER && !craft.getSinking()) {
                    Player player = (Player) entity;
                    craft.getMovedPlayers().put(player, System.currentTimeMillis());
                    EntityMoveUpdateCommand eUp = new EntityMoveUpdateCommand(entity, dx, dy, dz, 0, 0);
                    updates.add(eUp);
                } else if (!craft.getType().getOnlyMovePlayers() || entity.getType() == EntityType.PRIMED_TNT) {
                    EntityMoveUpdateCommand eUp = new EntityMoveUpdateCommand(entity, dx, dy, dz, 0, 0);
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
                craft.getW().playSound(location, Settings.IsLegacy ? LegacyUtils.ENITIY_IRONGOLEM_DEATH : Sound.ENTITY_IRON_GOLEM_DEATH, 5.0f, 5.0f);
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
            if (Settings.IsLegacy) {
                //generate seed drops
                if (block.getType() == LegacyUtils.CROPS) {
                    Random rand = new Random();
                    int amount = rand.nextInt(4);
                    if (amount > 0) {
                        ItemStack seeds = new ItemStack(LegacyUtils.SEEDS, amount);
                        drops.add(seeds);
                    }
                }
            } else {
                if (block.getType() == Material.WHEAT){
                    Random rand = new Random();
                    int amount = rand.nextInt(4);
                    if (amount > 0){
                        ItemStack seeds = new ItemStack(Material.WHEAT_SEEDS, amount);
                        drops.add(seeds);
                    }
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
