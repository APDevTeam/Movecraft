package net.countercraft.movecraft.mapUpdater.update;

import com.google.common.collect.Lists;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.WorldHandler;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.countercraft.movecraft.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CraftTranslateCommand extends UpdateCommand {
    @NotNull private final Craft craft;
    @NotNull private final MovecraftLocation displacement;
    @NotNull private final World world;

    public CraftTranslateCommand(@NotNull Craft craft, @NotNull MovecraftLocation displacement){
        this.craft = craft;
        this.displacement = displacement;
        this.world = craft.getW();
    }
    
    public CraftTranslateCommand(@NotNull Craft craft, @NotNull MovecraftLocation displacement, @NotNull World world){
        this.craft = craft;
        this.displacement = displacement;
        this.world = world;
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
        if(craft.getSinking()){
            passthroughBlocks.add(Material.STATIONARY_WATER);
            passthroughBlocks.add(Material.WATER);
            passthroughBlocks.add(Material.LEAVES);
            passthroughBlocks.add(Material.LEAVES_2);
            passthroughBlocks.add(Material.LONG_GRASS);
            passthroughBlocks.add(Material.DOUBLE_PLANT);
        }
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
                byte data = b.getData();
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
                byte data = b.getData();
                if (!passthroughBlocks.contains(material)) {
                    continue;
                }
                craft.getPhaseBlocks().put(location.toBukkit(world), new Pair<>(material, data));
            }
            //translate the craft
            handler.translateCraft(craft, displacement,world);
            craft.setW(world);
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
                Pair<Material, Byte> phaseBlock = craft.getPhaseBlocks().remove(bukkit);
                handler.setBlockFast(bukkit, phaseBlock.getLeft(), phaseBlock.getRight());
                craft.getPhaseBlocks().remove(bukkit);
            }

            for(MovecraftLocation location : originalLocations){
                Location bukkit = location.toBukkit(oldWorld);
                if(!craft.getHitBox().contains(location) && craft.getPhaseBlocks().containsKey(bukkit)){
                    Pair<Material, Byte> phaseBlock = craft.getPhaseBlocks().remove(bukkit);
                    handler.setBlockFast(bukkit, phaseBlock.getLeft(), phaseBlock.getRight());
                }
            }

            for (MovecraftLocation location : failed) {
                Location bukkit = location.toBukkit(oldWorld);
                Block b = bukkit.getBlock();
                Material material = b.getType();
                byte data = b.getData();
                if (passthroughBlocks.contains(material)) {
                    craft.getPhaseBlocks().put(bukkit, new Pair<>(material, data));
                    handler.setBlockFast(bukkit, Material.AIR, (byte) 0);

                }
            }
        }
        if (!craft.isNotProcessing())
            craft.setProcessing(false);
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
            Block block = location.toBukkit(craft.getW()).getBlock();
            Material type = block.getType();
            if (type == Material.WALL_SIGN || type == Material.SIGN_POST) {
                Sign sign = (Sign) block.getState();
                if(!signs.containsKey(sign.getLines()))
                    signs.put(sign.getLines(), new ArrayList<>());
                signs.get(sign.getLines()).add(location);
            }
        }
        for(Map.Entry<String[], List<MovecraftLocation>> entry : signs.entrySet()){
            Bukkit.getServer().getPluginManager().callEvent(new SignTranslateEvent(craft, entry.getKey(), entry.getValue()));
            for(MovecraftLocation loc : entry.getValue()){
                Block block = loc.toBukkit(craft.getW()).getBlock();
                if (block.getType() != Material.WALL_SIGN && block.getType() != Material.SIGN_POST) {
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
