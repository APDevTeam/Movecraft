package net.countercraft.movecraft.mapUpdater.update;

import com.google.common.collect.Sets;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.api.config.Settings;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.WorldHandler;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.SignTranslateEvent;
<<<<<<< HEAD
=======
import net.countercraft.movecraft.utils.CollectionUtils;
import net.countercraft.movecraft.utils.HashHitBox;
import net.countercraft.movecraft.utils.HitBox;
import net.countercraft.movecraft.utils.MutableHitBox;
import net.countercraft.movecraft.utils.SolidHitBox;
>>>>>>> upstream/master
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

public class CraftTranslateCommand extends UpdateCommand {
    @NotNull private final Craft craft;
    @NotNull private final MovecraftLocation displacement;

    public CraftTranslateCommand(@NotNull Craft craft, @NotNull MovecraftLocation displacement){
        this.craft = craft;
        this.displacement = displacement;
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
        MutableHitBox originalLocations = new HashHitBox();
        for(MovecraftLocation movecraftLocation : craft.getHitBox()) {
            originalLocations.add((movecraftLocation).subtract(displacement));
        }

        final HitBox to = CollectionUtils.filter(craft.getHitBox(), originalLocations);

        for(MovecraftLocation location: to){
            Material material = location.toBukkit(craft.getW()).getBlock().getType();
            if(craft.getType().getPassthroughBlocks().contains(material)){
                craft.getPhaseBlocks().put(location,material);
            }
        }

        //place phased blocks
        final int minX = craft.getHitBox().getMinX();
        final int maxX = craft.getHitBox().getMaxX();
        final int minY = craft.getHitBox().getMinY();
        final int maxY = craft.getHitBox().getMaxY();
        final int minZ = craft.getHitBox().getMinZ();
        final int maxZ = craft.getHitBox().getMaxZ();

        final HitBox[] surfaces = {
                new SolidHitBox(new MovecraftLocation(minX,minY,minZ), new MovecraftLocation(minX,maxY,maxZ)),
                new SolidHitBox(new MovecraftLocation(minX,minY,minZ), new MovecraftLocation(maxX,minY,maxZ)),
                new SolidHitBox(new MovecraftLocation(minX,minY,minZ), new MovecraftLocation(maxX,maxY,minZ)),
                new SolidHitBox(new MovecraftLocation(maxX,maxY,maxZ), new MovecraftLocation(minX,maxY,maxZ)),
                new SolidHitBox(new MovecraftLocation(maxX,maxY,maxZ), new MovecraftLocation(maxX,minY,maxZ)),
                new SolidHitBox(new MovecraftLocation(maxX,maxY,maxZ), new MovecraftLocation(maxX,maxY,minZ))};
        //Valid exterior starts as the 6 surface planes of the HitBox with the locations that lie in the HitBox removed
        final Set<MovecraftLocation> validExterior = new HashSet<>();
        for(HitBox hitBox : surfaces){
            validExterior.addAll(CollectionUtils.filter(hitBox, craft.getHitBox()).asSet());
        }
        //The subtraction of the set of coordinates in the HitBox cube and the HitBox itself
        final HitBox invertedHitBox = CollectionUtils.filter(craft.getHitBox().boundingHitBox(), craft.getHitBox());
        //A set of locations that are confirmed to be "exterior" locations
        final Set<MovecraftLocation> confirmed = new HashSet<>();
        final Set<MovecraftLocation> failed = new HashSet<>();
        //Check to see which locations in the from set are actually outside of the craft
        for(MovecraftLocation location : Sets.union(originalLocations.boundingHitBox().asSet(), craft.getHitBox().boundingHitBox().asSet())){
            if(craft.getHitBox().contains(location)){
                continue;
            }

            if(!craft.getHitBox().inBounds(location)){
                confirmed.add(location);
                continue;
            }
            //use a modified BFS for multiple origin elements
            Set<MovecraftLocation> visited = new HashSet<>();
            Queue<MovecraftLocation> queue = new LinkedList<>();
            boolean interior = true;
            queue.add(location);
            while (!queue.isEmpty()){
                MovecraftLocation node = queue.poll();
                //If the node is already a valid member of the exterior of the HitBox, continued search is unitary.
                if(validExterior.contains(node)){
                    confirmed.add(location);
                    validExterior.addAll(visited);
                    interior = false;
                    break;
                }
                for(MovecraftLocation neighbor : CollectionUtils.neighbors(invertedHitBox, node)){
                    if(visited.contains(neighbor)){
                        continue;
                    }
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
            if(interior)
                failed.add(location);
        }

        final WorldHandler handler = Movecraft.getInstance().getWorldHandler();
        for(MovecraftLocation location : CollectionUtils.filter(invertedHitBox, confirmed)){
            Material material = location.toBukkit(craft.getW()).getBlock().getType();
            if(!craft.getType().getPassthroughBlocks().contains(material)){
                continue;
            }
            craft.getPhaseBlocks().put(location,material);
        }

        //translate the craft

        handler.translateCraft(craft,displacement);
        //trigger sign events
        for(MovecraftLocation location : craft.getHitBox()){
            Block block = location.toBukkit(craft.getW()).getBlock();
            if(block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST){
                Sign sign = (Sign) block.getState();
                Bukkit.getServer().getPluginManager().callEvent(new SignTranslateEvent(block, craft, sign.getLines()));
                sign.update();
            }
        }

        //place confirmed blocks if they have been un-phased
        for(MovecraftLocation location : confirmed){
            if(!craft.getPhaseBlocks().containsKey(location)){
                continue;
            }
            handler.setBlockFast(location.toBukkit(craft.getW()),craft.getPhaseBlocks().get(location), (byte)0);
            craft.getPhaseBlocks().remove(location);
        }

        for(MovecraftLocation location: failed){
            final Material material = location.toBukkit(craft.getW()).getBlock().getType();
            if (craft.getType().getPassthroughBlocks().contains(material )){
                craft.getPhaseBlocks().put(location,material);
                handler.setBlockFast(location.toBukkit(craft.getW()), Material.AIR, (byte)0);

            }
        }

        /*logger.info("Move");
        for(Map.Entry<MovecraftLocation, Material> entry : craft.getPhaseBlocks().entrySet()){
            logger.info("Entry: " + entry.getKey() + " " + entry.getValue());
        }*/

        /*for(MovecraftLocation location : CollectionUtils.filter(invertedHitBox, confirmed)){
            Material material = location.toBukkit(craft.getW()).getBlock().getType();
            if(!craft.getPhaseBlocks().containsKey(location) || material.equals(Material.AIR)){
                continue;
            }
            handler.setBlockFast(location.toBukkit(craft.getW()), Material.WOOL, (byte)0);
        }*/

        time = System.nanoTime() - time;
        if(Settings.Debug)
            logger.info("Total time: " + (time / 1e9) + " seconds. Moving with cooldown of " + craft.getTickCooldown() + ". Speed of: " + String.format("%.2f", craft.getSpeed()));
        craft.addMoveTime(time/1e9f);
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
