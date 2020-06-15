package net.countercraft.movecraft.mapUpdater.update;

import com.google.common.collect.Lists;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.WorldHandler;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.countercraft.movecraft.mapUpdater.MapUpdateManager;
import net.countercraft.movecraft.utils.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CraftTranslateCommand extends UpdateCommand {
    @NotNull private final Craft craft;
    @NotNull private final MovecraftLocation displacement;
    @NotNull private final World world;
    @Nullable private final Sound sound;
    private final float volume;

    public CraftTranslateCommand(@NotNull Craft craft, @NotNull MovecraftLocation displacement){
        this.craft = craft;
        this.displacement = displacement;
        this.world = craft.getW();
        sound = null;
        volume = 0f;
    }
    
    public CraftTranslateCommand(@NotNull Craft craft, @NotNull MovecraftLocation displacement, @NotNull World world, @Nullable Sound sound, float volume){
        this.craft = craft;
        this.displacement = displacement;
        this.world = world;
        this.sound = sound;
        this.volume = volume;
    }




    @Override
    public void doUpdate() {
        final Logger logger = Movecraft.getInstance().getLogger();
        if(craft.getHitBox().isEmpty()){
            logger.warning("Attempted to move craft with empty HashHitBox!");
            CraftManager.getInstance().removeCraft(craft);
            return;
        }
        long time = System.nanoTime();
        World oldWorld = craft.getW();
        final Set<Material> passthroughBlocks = new HashSet<>(craft.getType().getPassthroughBlocks());
        final Set<UpdateCommand> toMove = new HashSet<>();
        if(craft.getSinking()){
            passthroughBlocks.add(Material.WATER);//Same in the 1.13 API, but other values are different
            if (Settings.IsLegacy){ //use pre-1.13 values if running on 1.12.2 or lower
                passthroughBlocks.add(LegacyUtils.STATIONARY_WATER);
                passthroughBlocks.add(LegacyUtils.LEAVES);
                passthroughBlocks.add(LegacyUtils.LEAVES_2);
                passthroughBlocks.add(LegacyUtils.LONG_GRASS);
                passthroughBlocks.add(LegacyUtils.DOUBLE_PLANT);
            } else {//otherwise, use 1.13+ types
                //Leaves
                passthroughBlocks.add(Material.ACACIA_LEAVES);
                passthroughBlocks.add(Material.BIRCH_LEAVES);
                passthroughBlocks.add(Material.DARK_OAK_LEAVES);
                passthroughBlocks.add(Material.JUNGLE_LEAVES);
                passthroughBlocks.add(Material.OAK_LEAVES);
                passthroughBlocks.add(Material.SPRUCE_LEAVES);
                //Grass
                passthroughBlocks.add(Material.GRASS);
                //Double plants
                passthroughBlocks.add(Material.ROSE_BUSH);
                passthroughBlocks.add(Material.SUNFLOWER);
                passthroughBlocks.add(Material.LILAC);
                passthroughBlocks.add(Material.PEONY);
                passthroughBlocks.add(Material.KELP);
                passthroughBlocks.add(Material.KELP_PLANT);
                passthroughBlocks.add(Material.TALL_SEAGRASS);
                passthroughBlocks.add(Material.SEA_PICKLE);
                passthroughBlocks.add(Material.SEAGRASS);

                passthroughBlocks.add(Material.BUBBLE_COLUMN);

            }
        } else if (craft.getType().getMoveEntities() && !craft.getSinking()){
            final Location midpoint = craft.getHitBox().getMidPoint().subtract(displacement).toBukkit(craft.getWorld()).add(.5,.5,.5);
            final Collection<Entity> entities = craft.getWorld().getNearbyEntities(midpoint, craft.getHitBox().getXLength() / 2.0 + 1, craft.getHitBox().getYLength() / 2.0 + 2, craft.getHitBox().getZLength() / 2.0 + 1);
            Entity vehicle;
            Entity playerVehicle = null;
            for (Entity entity : entities){
                vehicle = entity.getVehicle();
                if (entity.getType() == EntityType.PLAYER) {
                    //Only teleport one player if more players ride the same vehicle
                    if (playerVehicle != null && playerVehicle.getPassengers().contains(entity)) {
                        continue;
                    }
                    playerVehicle = entity.getVehicle();
                    //If player has a vehicle and craft should only move players, eject its passengers
                    if (playerVehicle != null && craft.getType().getOnlyMovePlayers()) {
                        playerVehicle.eject();
                    }
                    toMove.add(new EntityUpdateCommand(entity, displacement.getX(), displacement.getY(), displacement.getZ(), 0, 0, world, sound, volume));
                } else if (!craft.getType().getOnlyMovePlayers() || entity.getType() == EntityType.PRIMED_TNT) {
                    boolean isPlayerVehicle = false;
                    for (Entity pass : entity.getPassengers()) {
                        if (pass.getType() != EntityType.PLAYER) {
                            continue;
                        }
                        isPlayerVehicle = true;
                        break;
                    }
                    if (vehicle != null && vehicle.getPassengers().contains(entity) || entity == playerVehicle || isPlayerVehicle) {
                        continue;
                    }
                    toMove.add(new EntityUpdateCommand(entity, displacement.getX(), displacement.getY(), displacement.getZ(), 0, 0, world));
                }
            }
        }
        MutableHitBox interior = new BitmapHitBox();
        MutableHitBox exterior = new BitmapHitBox();
        if(passthroughBlocks.isEmpty()){
            //translate the craft
            Movecraft.getInstance().getWorldHandler().translateCraft(craft,displacement,world);
            craft.setW(world);
            //trigger sign events
            this.sendSignEvents();
        } else {
            BitmapHitBox originalLocations = new BitmapHitBox();
            for (MovecraftLocation movecraftLocation : craft.getHitBox()) {
                originalLocations.add(movecraftLocation.subtract(displacement));
            }
            final HitBox to = craft.getHitBox().difference(originalLocations);
            //place phased blocks
            for (MovecraftLocation location : to) {
                Block b = location.toBukkit(world).getBlock();
                Material material = b.getType();
                Object data = Settings.IsLegacy ? b.getData() : b.getBlockData();
                if (passthroughBlocks.contains(material)) {
                    craft.getPhaseBlocks().put(location.toBukkit(world), new Pair<>(material, data));
                }
            }
            //The subtraction of the set of coordinates in the HitBox cube and the HitBox itself
            final BitmapHitBox invertedHitBox = new BitmapHitBox(craft.getHitBox().boundingHitBox()).difference(craft.getHitBox());

            //A set of locations that are confirmed to be "exterior" locations
            final BitmapHitBox confirmed = new BitmapHitBox();
            final BitmapHitBox failed = new BitmapHitBox();

            //place phased blocks
            final Set<Location> overlap = new HashSet<>(craft.getPhaseBlocks().keySet());
            overlap.retainAll(craft.getHitBox().asSet().stream().map(l -> l.toBukkit(world)).collect(Collectors.toSet()));
            final int minX = craft.getHitBox().getMinX();
            final int maxX = craft.getHitBox().getMaxX();
            final int minY = craft.getHitBox().getMinY();
            final int maxY = overlap.isEmpty() ? craft.getHitBox().getMaxY() : Collections.max(overlap, Comparator.comparingInt(Location::getBlockY)).getBlockY();
            final int minZ = craft.getHitBox().getMinZ();
            final int maxZ = craft.getHitBox().getMaxZ();
            final HitBox[] surfaces = {
                    new SolidHitBox(new MovecraftLocation(minX, minY, minZ), new MovecraftLocation(minX, maxY, maxZ)),
                    new SolidHitBox(new MovecraftLocation(minX, minY, minZ), new MovecraftLocation(maxX, maxY, minZ)),
                    new SolidHitBox(new MovecraftLocation(maxX, minY, maxZ), new MovecraftLocation(minX, maxY, maxZ)),
                    new SolidHitBox(new MovecraftLocation(maxX, minY, maxZ), new MovecraftLocation(maxX, maxY, minZ)),
                    new SolidHitBox(new MovecraftLocation(minX, minY, minZ), new MovecraftLocation(maxX, minY, maxZ))};
            final BitmapHitBox validExterior = new BitmapHitBox();
            for (HitBox hitBox : surfaces) {
                validExterior.addAll(new BitmapHitBox(hitBox).difference(craft.getHitBox()));
            }

            //Check to see which locations in the from set are actually outside of the craft
            //use a modified BFS for multiple origin elements
            BitmapHitBox visited = new BitmapHitBox();
            Queue<MovecraftLocation> queue = Lists.newLinkedList(validExterior);
            while (!queue.isEmpty()) {
                MovecraftLocation node = queue.poll();
                if(visited.contains(node))
                    continue;
                visited.add(node);
                //If the node is already a valid member of the exterior of the HitBox, continued search is unitary.
                for (MovecraftLocation neighbor : CollectionUtils.neighbors(invertedHitBox, node)) {
                    queue.add(neighbor);
                }
            }
            confirmed.addAll(visited);

            if(craft.getSinking()){
                confirmed.addAll(invertedHitBox);
            }
            failed.addAll(invertedHitBox.difference(confirmed));

            final WorldHandler handler = Movecraft.getInstance().getWorldHandler();
            for (MovecraftLocation location : invertedHitBox.difference(confirmed)) {
                Block b = location.toBukkit(world).getBlock();
                Material material = b.getType();
                Object data = Settings.IsLegacy ? b.getData() : b.getBlockData();
                if (!passthroughBlocks.contains(material)) {
                    continue;
                }
                craft.getPhaseBlocks().put(location.toBukkit(world), new Pair<>(material, data));
            }
            //translate the craft
            handler.translateCraft(craft, displacement,world);
            craft.setWorld(world);
            //trigger sign events
            this.sendSignEvents();

            for (MovecraftLocation l : failed){
                MovecraftLocation orig = l.subtract(displacement);
                if (craft.getHitBox().contains(orig) || failed.contains(orig)){
                    continue;
                }
                confirmed.add(orig);

            }

            //place confirmed blocks if they have been un-phased
            for (MovecraftLocation location : confirmed) {
                Location bukkit = location.toBukkit(craft.getW());
                if (!craft.getPhaseBlocks().containsKey(bukkit)) {
                    continue;
                }
                //Do not place if it is at a collapsed HitBox location
                if (!craft.getCollapsedHitBox().isEmpty() && craft.getCollapsedHitBox().contains(location))
                    continue;
                Pair<Material, Object> phaseBlock = craft.getPhaseBlocks().remove(bukkit);
                handler.setBlockFast(bukkit, phaseBlock.getLeft(), phaseBlock.getRight());
                craft.getPhaseBlocks().remove(bukkit);
            }

            for(MovecraftLocation location : originalLocations){
                Location bukkit = location.toBukkit(oldWorld);
                if(!craft.getHitBox().contains(location) && craft.getPhaseBlocks().containsKey(bukkit)){
                    Pair<Material, Object> phaseBlock = craft.getPhaseBlocks().remove(bukkit);
                    handler.setBlockFast(bukkit, phaseBlock.getLeft(), phaseBlock.getRight());
                }
            }

            for (MovecraftLocation location : failed) {
                Location bukkit = location.toBukkit(oldWorld);
                Block b = bukkit.getBlock();
                Material material = b.getType();
                Object data = Settings.IsLegacy ? b.getData() : b.getBlockData();
                if (passthroughBlocks.contains(material)) {
                    craft.getPhaseBlocks().put(bukkit, new Pair<>(material, data));
                    handler.setBlockFast(bukkit, Material.AIR, (byte) 0);

                }
            }
            interior.addAll(failed);

        }
        MapUpdateManager.getInstance().scheduleUpdates(toMove);
        if (!Settings.IsLegacy) {
            WaterlogUtils.waterlogBlocksOnCraft(craft, interior);
        }
        if (!craft.isNotProcessing()) {
            craft.setProcessing(false);
        }
        if (craft.isTranslating()) {
            craft.setTranslating(false);
        }
        time = System.nanoTime() - time;
        if(Settings.Debug)
            logger.info("Total time: " + (time / 1e6) + " milliseconds. Moving with cooldown of " + craft.getTickCooldown() + ". Speed of: " + String.format("%.2f", craft.getSpeed()) + ". Displacement of: " + displacement);

        // Only add cruise time if cruising
        if(craft.getCruising() && displacement.getY() == 0 && (displacement.getX() == 0 || displacement.getZ() == 0))
            craft.addCruiseTime(time / 1e9f);
    }

    private void sendSignEvents(){
        Map<String[], List<MovecraftLocation>> signs = new HashMap<>();
        for (MovecraftLocation location : craft.getHitBox()) {
            Block block = location.toBukkit(craft.getWorld()).getBlock();
            if (SignUtils.isSign(block)) {
                Sign sign = (Sign) block.getState();
                if(!signs.containsKey(sign.getLines()))
                    signs.put(sign.getLines(), new ArrayList<>());
                signs.get(sign.getLines()).add(location);
            }
        }
        for(Map.Entry<String[], List<MovecraftLocation>> entry : signs.entrySet()){
            Bukkit.getServer().getPluginManager().callEvent(new SignTranslateEvent(craft, entry.getKey(), entry.getValue()));
            for(MovecraftLocation loc : entry.getValue()){
                Block block = loc.toBukkit(craft.getWorld()).getBlock();
                if (!(block.getState() instanceof Sign)) {
                    continue;
                }
                Sign sign = (Sign) block.getState();
                for(int i = 0; i<4; i++){
                    sign.setLine(i, entry.getKey()[i]);
                }
                sign.update();
            }
        }
    }

    @NotNull
    public Craft getCraft(){
        return craft;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof CraftTranslateCommand)){
            return false;
        }
        CraftTranslateCommand other = (CraftTranslateCommand) obj;
        return other.craft.equals(this.craft) &&
                other.displacement.equals(this.displacement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(craft, displacement);
    }
}
