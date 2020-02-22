package net.countercraft.movecraft.async.translation;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.async.AsyncTask;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CraftType;
import net.countercraft.movecraft.events.CraftCollisionEvent;
import net.countercraft.movecraft.events.CraftTranslateEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.mapUpdater.update.*;
import net.countercraft.movecraft.utils.HashHitBox;
import net.countercraft.movecraft.utils.HitBox;
import net.countercraft.movecraft.utils.LegacyUtils;
import net.countercraft.movecraft.utils.MutableHitBox;
import org.bukkit.*;
import org.bukkit.block.*;
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
    private int dx, dy, dz;
    private HashHitBox newHitBox, oldHitBox;
    private boolean failed;
    private boolean collisionExplosion = false;
    private String failMessage;
    private Collection<UpdateCommand> updates = new LinkedList<>();

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

        //Prevent disabled crafts from moving
        if (getCraft().getDisabled() && (!getCraft().getSinking())) {
            fail(I18nSupport.getInternationalisedString("Translation - Failed Craft Is Disabled"));
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
            dy = Math.min(dy,-1);
        }else if(craft.getType().getMaxHeightAboveGround() > 0){
            final MovecraftLocation middle = oldHitBox.getMidPoint();
            int testY = minY;
            while (testY > 0){
                testY--;
                if (!craft.getW().getBlockAt(middle.getX(),testY,middle.getZ()).getType().name().endsWith("AIR"))
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
                    for (MovecraftLocation bTest : oldHitBox) {
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
                    for (Material fuel : Settings.FuelTypes.keySet()){
                        if (furnace.getInventory().contains(fuel)){
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
        //Process gravity
        if (craft.getType().getUseGravity() && !craft.getSinking()){
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
        }
        //Fail the movement if the craft is too high and if the craft is not explosive
        if (dy>0 && maxY + dy > craft.getType().getMaxHeightLimit() && craft.getType().getCollisionExplosion() <= 0f) {
            fail(I18nSupport.getInternationalisedString("Translation - Failed Craft hit height limit"));
            return;
        } else if (dy>0 && maxY + dy > craft.getType().getMaxHeightLimit()) { //If explosive and too high, set dy to 0
            dy = 0;
        } else if (minY + dy < craft.getType().getMinHeightLimit() && dy < 0 && !craft.getSinking() && !craft.getType().getUseGravity()) {
            fail(I18nSupport.getInternationalisedString("Translation - Failed Craft hit minimum height limit"));
            return;
        } else if (minY + dy < craft.getType().getMinHeightLimit() && dy < 0 && craft.getType().getUseGravity()) {
            //if a craft using gravity hits the minimum height limit, set dy = 0 instead of failing
            dy = 0;
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
            final Block b = newLocation.toBukkit(craft.getW()).getBlock();
            final Material testMaterial = b.getType();

            if ((testMaterial.equals(Material.CHEST) || testMaterial.equals(Material.TRAPPED_CHEST)) && checkChests(testMaterial, newLocation)) {
                //prevent chests collision
                fail(String.format(I18nSupport.getInternationalisedString("Translation - Failed Craft is obstructed") + " @ %d,%d,%d,%s", newLocation.getX(), newLocation.getY(), newLocation.getZ(), newLocation.toBukkit(craft.getW()).getBlock().getType().toString()));
                return;
            }

            if (!craft.getW().getWorldBorder().isInside(newLocation.toBukkit(craft.getW()))) {
                fail(I18nSupport.getInternationalisedString("Translation - Failed Craft cannot pass world border"));
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

        if (craft.getType().getForbiddenHoverOverBlocks().size() > 0){
            MovecraftLocation test = new MovecraftLocation(newHitBox.getMidPoint().getX(), newHitBox.getMinY(), newHitBox.getMidPoint().getZ());
            test = test.translate(0, -1, 0);
            while (test.toBukkit(craft.getW()).getBlock().getType() == Material.AIR){
                test = test.translate(0, -1, 0);
            }
            Material testType = test.toBukkit(craft.getW()).getBlock().getType();
            if (craft.getType().getForbiddenHoverOverBlocks().contains(testType)){
                fail(String.format(I18nSupport.getInternationalisedString("Translation - Failed Craft over block"), testType.name().toLowerCase().replace("_", " ")));
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
                    Location loc = location.translate( dx, dy, dz).toBukkit(craft.getW());
                    if (!loc.getBlock().getType().equals(Material.AIR)  && ThreadLocalRandom.current().nextDouble(1) < .05) {
                        updates.add(new ExplosionUpdateCommand( loc, craft.getType().getExplodeOnCrash()));
                        collisionExplosion=true;
                    }
                }
                List<MovecraftLocation> toRemove = new ArrayList<>();
                MovecraftLocation next = location.translate(-dx,-dy,-dz);
                while(oldHitBox.contains(next)) {
                    toRemove.add(next);
                    next = next.translate(0,1,0);
                }
                craft.getCollapsedHitBox().addAll(toRemove);
                newHitBox.removeAll(toRemove);

            }
            if (Settings.Debug){
                Bukkit.getLogger().info("Collapsed hitbox: "+craft.getCollapsedHitBox().size() + ", New hitbox: " + newHitBox.size());
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

                int potEffRange = craft.getType().getEffectRange();
                Map<PotionEffect,Integer> potionEffects = craft.getType().getPotionEffectsToApply();

                Location loc = location.toBukkit(craft.getW());
                if (!loc.getBlock().getType().equals(Material.AIR)) {
                    updates.add(new ExplosionUpdateCommand(loc, explosionKey));
                    collisionExplosion = true;
                    if (potEffRange != 0 && !potionEffects.isEmpty())
                        updates.add(new PotionEffectsUpdateCommand(loc,potEffRange,potionEffects));
                }
                if (craft.getType().getFocusedExplosion()) { // don't handle any further collisions if it is set to focusedexplosion
                    break;
                }
            }
        }

        if(!collisionBox.isEmpty() && craft.getType().getCruiseOnPilot()){
            CraftManager.getInstance().removeCraft(craft);
            for(MovecraftLocation location : oldHitBox){
                updates.add(new BlockCreateCommand(craft.getW(), location, craft.getPhaseBlocks().getOrDefault(location, Material.AIR)));
            }
            newHitBox = new HashHitBox();
        }

        if(!collisionBox.isEmpty()){
            Bukkit.getServer().getPluginManager().callEvent(new CraftCollisionEvent(craft, collisionBox, true));
        }
        if (oldHitBox.isEmpty())
            return;
        if (!craft.getType().getMoveEntities() && !(craft.getSinking() && craft.getType().getOnlyMovePlayers())) {
            //add releaseTask without playermove to manager
            if (!craft.getType().getCruiseOnPilot() && !craft.getSinking())  // not necessary to release cruiseonpilot crafts, because they will already be released
                CraftManager.getInstance().addReleaseTask(craft);
        }
        updates.add(new CraftTranslateCommand(craft, new MovecraftLocation(dx, dy, dz)));



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
                craft.getW().playSound(location, Settings.IsLegacy ? (Settings.IsPre1_9 ? LegacyUtils.IRONGOLEM_DEATH : LegacyUtils.ENITIY_IRONGOLEM_DEATH) : Sound.ENTITY_IRON_GOLEM_DEATH, 5.0f, 5.0f);
                //craft.setCurTickCooldown(craft.getType().getCruiseTickCooldown());
            }
        }
        if (craft.isTranslating()) {
            craft.setTranslating(false);
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
        new BukkitRunnable() {
            @Override
            public void run() {
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
                        if (block.getType() == Material.WHEAT) {
                            Random rand = new Random();
                            int amount = rand.nextInt(4);
                            if (amount > 0) {
                                ItemStack seeds = new ItemStack(Material.WHEAT_SEEDS, amount);
                                drops.add(seeds);
                            }
                        }
                    }
                    //get contents of inventories before deposting

                    if (block.getState() instanceof InventoryHolder) {
                        if (block.getState() instanceof Chest) {
                            drops.addAll(Arrays.asList(((Chest) block.getState()).getBlockInventory().getContents()));
                        } else if (block.getState() instanceof Barrel) {
                            drops.addAll(Arrays.asList(((Barrel) block.getState()).getInventory().getContents()));
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

        }.runTask(Movecraft.getInstance());
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
                surfaceLoc.getY() + 1 > craft.getType().getMaxHeightLimit());
        return surfaceLoc;
    }

    private int inclineCraft(HashHitBox hitBox){
        if (isOnGround(hitBox) && dy < 0){
            dy = 0;
        }
        HashHitBox collisionBox = new HashHitBox();
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
        HashHitBox movedCollBox = new HashHitBox();
        for (MovecraftLocation ml : collisionBox) {
            movedCollBox.add(ml.translate(0, elevation, 0));

        }
        return movedCollBox.getMinY() - hitBox.getMinY();
    }

    private int dropDistance (HashHitBox hitBox) {
        MutableHitBox bottomLocs = new HashHitBox();
        for (MovecraftLocation location : hitBox){
            if (location.getY() != hitBox.getLocalMinY(location.getX(), location.getZ())) {
                continue;
            }
            //Otherwise, add to bottom locations
            bottomLocs.add(location.translate(0, -1, 0));
        }
        int dropDistance = 0;

        do {
            boolean hitGround = false;

            for (MovecraftLocation ml : bottomLocs) {
                final MovecraftLocation translated = ml.translate(dx, dy, dz);
                //This has to be subtracted by one, or non-passthrough blocks will be within the y drop path
                //obstructing the craft
                Material testType = translated.translate(0, dropDistance , 0).toBukkit(craft.getW()).getBlock().getType();
                hitGround = testType != Material.AIR &&
                        !craft.getType().getPassthroughBlocks().contains(testType) &&
                        !(craft.getType().getHarvestBlocks().contains(testType) &&
                        craft.getType().getHarvesterBladeBlocks().contains(ml.translate(0, 1, 0).toBukkit(craft.getW()).getBlock().getType())) ||
                        craft.getType().getMinHeightLimit() == translated.translate(0, dropDistance + 1 , 0).getY();

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

    private boolean isOnGround(HashHitBox hitBox){
        MutableHitBox bottomLocs = new HashHitBox();
        MutableHitBox translatedBottomLocs = new HashHitBox();
        if (hitBox.getMinY() <= craft.getType().getMinHeightLimit()) {
            return true;
        }
        for (MovecraftLocation location : hitBox){
            if (location.getY() > hitBox.getLocalMinY(location.getX(), location.getZ())) {
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
            if (testType.name().endsWith("AIR")){
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
