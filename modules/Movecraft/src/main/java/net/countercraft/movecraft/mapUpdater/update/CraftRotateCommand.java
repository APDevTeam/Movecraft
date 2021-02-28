package net.countercraft.movecraft.mapUpdater.update;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.Rotation;
import net.countercraft.movecraft.WorldHandler;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.countercraft.movecraft.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class CraftRotateCommand extends UpdateCommand {
    @NotNull
    private final Craft craft;
    @NotNull
    private final Rotation rotation;
    @NotNull
    private final MovecraftLocation originLocation;


    public CraftRotateCommand(@NotNull final Craft craft, @NotNull final MovecraftLocation originLocation, @NotNull final Rotation rotation) {
        this.craft = craft;
        this.rotation = rotation;
        this.originLocation = originLocation;
    }

    @Override
    public void doUpdate() {
        final Logger logger = Movecraft.getInstance().getLogger();
        if (craft.getHitBox().isEmpty()) {
            logger.warning("Attempted to move craft with empty HashHitBox!");
            CraftManager.getInstance().removeCraft(craft, CraftReleaseEvent.Reason.EMPTY);
            return;
        }
        long time = System.nanoTime();
        final Set<Material> passthroughBlocks = new HashSet<>(craft.getType().getPassthroughBlocks());
        if(craft.getSinking()){
            passthroughBlocks.add(Material.WATER);
            passthroughBlocks.addAll(Tag.LEAVES.getValues());
            passthroughBlocks.add(Material.TALL_GRASS);
            passthroughBlocks.add(Material.GRASS);
        }
        if (!passthroughBlocks.isEmpty()) {
            BitmapHitBox originalLocations = new BitmapHitBox();
            final Rotation counterRotation = rotation == Rotation.CLOCKWISE ? Rotation.ANTICLOCKWISE : Rotation.CLOCKWISE;
            for (MovecraftLocation movecraftLocation : craft.getHitBox()) {
                originalLocations.add(MathUtils.rotateVec(counterRotation, movecraftLocation.subtract(originLocation)).add(originLocation));
            }

            final HitBox to = craft.getHitBox().difference(originalLocations);

            for (MovecraftLocation location : to) {
                Block b = location.toBukkit(craft.getW()).getBlock();
                Material material = b.getType();
                byte data = b.getData();
                if (passthroughBlocks.contains(material)) {
                    craft.getPhaseBlocks().put(location.toBukkit(craft.getW()), new Pair<>(material, data));
                }
            }
            //The subtraction of the set of coordinates in the HitBox cube and the HitBox itself
            final BitmapHitBox invertedHitBox = new BitmapHitBox(craft.getHitBox().boundingHitBox()).difference(craft.getHitBox());
            //A set of locations that are confirmed to be "exterior" locations
            final BitmapHitBox exterior = new BitmapHitBox();
            final BitmapHitBox interior = new BitmapHitBox();

            //place phased blocks
            final Set<Location> overlap = new HashSet<>(craft.getPhaseBlocks().keySet());
            overlap.retainAll(craft.getHitBox().asSet().stream().map(l -> l.toBukkit(craft.getW())).collect(Collectors.toSet()));
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
            //Valid exterior starts as the 6 surface planes of the HitBox with the locations that lie in the HitBox removed
            final BitmapHitBox validExterior = new BitmapHitBox();
            for (HitBox hitBox : surfaces) {
                validExterior.addAll(new BitmapHitBox(hitBox).difference(craft.getHitBox()));
            }
            //Check to see which locations in the from set are actually outside of the craft
            for (MovecraftLocation location :validExterior ) {
                if (craft.getHitBox().contains(location) || exterior.contains(location)) {
                    continue;
                }
                //use a modified BFS for multiple origin elements
                BitmapHitBox visited = new BitmapHitBox();
                Queue<MovecraftLocation> queue = new LinkedList<>();
                queue.add(location);
                while (!queue.isEmpty()) {
                    MovecraftLocation node = queue.poll();
                    //If the node is already a valid member of the exterior of the HitBox, continued search is unitary.
                    for (MovecraftLocation neighbor : CollectionUtils.neighbors(invertedHitBox, node)) {
                        if (visited.contains(neighbor)) {
                            continue;
                        }
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
                exterior.addAll(visited);
            }
            interior.addAll(invertedHitBox.difference(exterior));

            final WorldHandler handler = Movecraft.getInstance().getWorldHandler();
            for (MovecraftLocation location : invertedHitBox.difference(exterior)) {
                Block b = location.toBukkit(craft.getW()).getBlock();
                Material material = b.getType();
                byte data = b.getData();
                if (!passthroughBlocks.contains(material)) {
                    continue;
                }
                craft.getPhaseBlocks().put(location.toBukkit(craft.getW()), new Pair<>(material, data));
            }

            //translate the craft

            handler.rotateCraft(craft, originLocation, rotation);
            //trigger sign events
            sendSignEvents();

            //place confirmed blocks if they have been un-phased
            for (MovecraftLocation location : exterior) {
                Location bukkit = location.toBukkit(craft.getW());
                if (!craft.getPhaseBlocks().containsKey(bukkit)) {
                    continue;
                }
                Pair<Material, Byte> phaseBlock = craft.getPhaseBlocks().remove(bukkit);
                handler.setBlockFast(bukkit, phaseBlock.getLeft(), phaseBlock.getRight());
                craft.getPhaseBlocks().remove(bukkit);
            }

            for(MovecraftLocation location : originalLocations.boundingHitBox()){
                Location bukkit = location.toBukkit(craft.getW());
                if(!craft.getHitBox().inBounds(location) && craft.getPhaseBlocks().containsKey(bukkit)){
                    Pair<Material, Byte> phaseBlock = craft.getPhaseBlocks().remove(bukkit);
                    handler.setBlockFast(bukkit, phaseBlock.getLeft(), phaseBlock.getRight());
                }
            }

            for (MovecraftLocation location : interior) {
                Location bukkit = location.toBukkit(craft.getW());
                Block b = bukkit.getBlock();
                Material material = b.getType();
                byte data = b.getData();
                if (passthroughBlocks.contains(material)) {
                    craft.getPhaseBlocks().put(bukkit, new Pair<>(material, data));
                    handler.setBlockFast(bukkit, Material.AIR, (byte) 0);

                }
            }
        }else{
            //translate the craft

            Movecraft.getInstance().getWorldHandler().rotateCraft(craft, originLocation, rotation);
            //trigger sign events
            sendSignEvents();
        }

        if (!craft.isNotProcessing())
            craft.setProcessing(false);
        time = System.nanoTime() - time;
        if (Settings.Debug)
            logger.info("Total time: " + (time / 1e6) + " milliseconds. Moving with cooldown of " + craft.getTickCooldown() + ". Speed of: " + String.format("%.2f", craft.getSpeed()));
    }

    private void sendSignEvents(){
        Object2ObjectMap<String[], List<MovecraftLocation>> signs = new Object2ObjectOpenCustomHashMap<>(new Hash.Strategy<String[]>() {
            @Override
            public int hashCode(String[] strings) {
                return Arrays.hashCode(strings);
            }

            @Override
            public boolean equals(String[] a, String[] b) {
                return Arrays.equals(a, b);
            }
        });
        Map<MovecraftLocation, Sign> signStates = new HashMap<>();

        for (MovecraftLocation location : craft.getHitBox()) {
            Block block = location.toBukkit(craft.getW()).getBlock();
            BlockState state = block.getState();
            if (state instanceof Sign) {
                Sign sign = (Sign) block.getState();
                if(!signs.containsKey(sign.getLines()))
                    signs.put(sign.getLines(), new ArrayList<>());
                signs.get(sign.getLines()).add(location);
                signStates.put(location, sign);
            }
        }
        for(Map.Entry<String[], List<MovecraftLocation>> entry : signs.entrySet()){
            SignTranslateEvent event = new SignTranslateEvent(craft, entry.getKey(), entry.getValue());
            Bukkit.getServer().getPluginManager().callEvent(event);
            if(!event.isUpdated()){
                continue;
            }
            for(MovecraftLocation location : entry.getValue()){
                Block block = location.toBukkit(craft.getW()).getBlock();
                BlockState state =  block.getState();
                if (!(state instanceof Sign)) {
                    continue;
                }
                Sign sign = signStates.get(location);
                for(int i = 0; i<4; i++){
                    sign.setLine(i, entry.getKey()[i]);
                }
                sign.update(false, false);
            }
        }
    }

    @NotNull
    public Craft getCraft() {
        return craft;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CraftRotateCommand)) {
            return false;
        }
        CraftRotateCommand other = (CraftRotateCommand) obj;
        return other.craft.equals(this.craft) &&
                other.rotation == this.rotation &&
                other.originLocation.equals(this.originLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(craft, rotation, originLocation);
    }
}
