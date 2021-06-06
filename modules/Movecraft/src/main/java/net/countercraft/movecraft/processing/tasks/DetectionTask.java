package net.countercraft.movecraft.processing.tasks;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.Result;
import net.countercraft.movecraft.processing.TaskPredicate;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.processing.tasks.detection.AllowedBlockValidator;
import net.countercraft.movecraft.processing.tasks.detection.AlreadyControlledValidator;
import net.countercraft.movecraft.processing.tasks.detection.AlreadyPilotingValidator;
import net.countercraft.movecraft.processing.tasks.detection.FlyBlockValidator;
import net.countercraft.movecraft.processing.tasks.detection.ForbiddenBlockValidator;
import net.countercraft.movecraft.processing.tasks.detection.ForbiddenSignStringValidator;
import net.countercraft.movecraft.processing.tasks.detection.NameSignValidator;
import net.countercraft.movecraft.processing.tasks.detection.PilotSignValidator;
import net.countercraft.movecraft.processing.tasks.detection.SizeValidator;
import net.countercraft.movecraft.processing.tasks.detection.SubcraftValidator;
import net.countercraft.movecraft.processing.tasks.detection.WaterContactValidator;
import net.countercraft.movecraft.util.AtomicLocationSet;
import net.countercraft.movecraft.util.CollectionUtils;
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.countercraft.movecraft.util.hitboxes.SolidHitBox;
import net.countercraft.movecraft.util.hitboxes.SetHitBox;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public class DetectionTask implements Runnable {
    private final static MovecraftLocation[] SHIFTS = {
            new MovecraftLocation(0, 1, 1),
            new MovecraftLocation(0, 0, 1),
            new MovecraftLocation(0, -1, 1),
            new MovecraftLocation(0, 1, 0),
            new MovecraftLocation(1, 1 ,0),
            new MovecraftLocation(1, 0 ,0),
            new MovecraftLocation(1, -1 ,0),
            new MovecraftLocation(0, 1, -1),
            new MovecraftLocation(0, 0, -1),
            new MovecraftLocation(0, -1, -1),
            new MovecraftLocation(0, -1, 0),
            new MovecraftLocation(-1, 1, 0),
            new MovecraftLocation(-1, 0, 0),
            new MovecraftLocation(-1, -1, 0)};
    private final Craft craft;
    private final MovecraftLocation startLocation;
    private final MovecraftWorld world;
    private final Player player;
    private final LongAdder size = new LongAdder();
    private final Set<MovecraftLocation> visited;
    private final ConcurrentLinkedDeque<MovecraftLocation> illegal = new ConcurrentLinkedDeque<>();
    private final ConcurrentMap<Material, Deque<MovecraftLocation>> materials = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<MovecraftLocation> legal = new ConcurrentLinkedDeque<>();
    private static final AllowedBlockValidator ALLOWED_BLOCK_VALIDATOR = new AllowedBlockValidator();
    private static final ForbiddenBlockValidator FORBIDDEN_BLOCK_VALIDATOR = new ForbiddenBlockValidator();
    private static final List<TaskPredicate<MovecraftLocation>> validators = List.of(
            new ForbiddenSignStringValidator(),
            new NameSignValidator(),
            new PilotSignValidator(),
            new SubcraftValidator(),
            new AlreadyControlledValidator());
    private static final List<TaskPredicate<Map<Material, Deque<MovecraftLocation>>>> completionValidators = List.of(
            new SizeValidator(),
            new WaterContactValidator(),
            new FlyBlockValidator(),
            new AlreadyPilotingValidator());

    public DetectionTask(@NotNull Craft craft, @NotNull MovecraftLocation startLocation, @NotNull MovecraftWorld world, @Nullable Player player) {
        this.craft = craft;
        this.startLocation = startLocation;
        this.world = world;
        this.player = player;
        visited = new AtomicLocationSet();
    }

    @Deprecated
    private void water(@NotNull Craft c){
        final int waterLine = WorldManager.INSTANCE.executeMain(c::getWaterLine);
        if (c.getType().blockedByWater() || c.getHitBox().getMinY() > waterLine) {
            return;
        }

        var badWorld = c.getWorld();
        //The subtraction of the set of coordinates in the HitBox cube and the HitBox itself
        final HitBox invertedHitBox = new BitmapHitBox(c.getHitBox().boundingHitBox()).difference(c.getHitBox());

        //A set of locations that are confirmed to be "exterior" locations
        final SetHitBox confirmed = new SetHitBox();
        final SetHitBox entireHitbox = new SetHitBox(c.getHitBox());

        //place phased blocks
        final Set<Location> overlap = new HashSet<>(c.getPhaseBlocks().keySet());
        overlap.retainAll(c.getHitBox().asSet().stream().map(l -> l.toBukkit(badWorld)).collect(Collectors.toSet()));
        final int minX = c.getHitBox().getMinX();
        final int maxX = c.getHitBox().getMaxX();
        final int minY = c.getHitBox().getMinY();
        final int maxY = overlap.isEmpty() ? c.getHitBox().getMaxY() : Collections.max(overlap, Comparator.comparingInt(Location::getBlockY)).getBlockY();
        final int minZ = c.getHitBox().getMinZ();
        final int maxZ = c.getHitBox().getMaxZ();
        final HitBox[] surfaces = {
                new SolidHitBox(new MovecraftLocation(minX, minY, minZ), new MovecraftLocation(minX, maxY, maxZ)),
                new SolidHitBox(new MovecraftLocation(minX, minY, minZ), new MovecraftLocation(maxX, maxY, minZ)),
                new SolidHitBox(new MovecraftLocation(maxX, minY, maxZ), new MovecraftLocation(minX, maxY, maxZ)),
                new SolidHitBox(new MovecraftLocation(maxX, minY, maxZ), new MovecraftLocation(maxX, maxY, minZ)),
                new SolidHitBox(new MovecraftLocation(minX, minY, minZ), new MovecraftLocation(maxX, minY, maxZ))};
        final SetHitBox validExterior = new SetHitBox();
        for (HitBox hitBox : surfaces) {
            validExterior.addAll(new BitmapHitBox(hitBox).difference(c.getHitBox()));
        }

        //Check to see which locations in the from set are actually outside of the craft
        //use a modified BFS for multiple origin elements
        SetHitBox visited = new SetHitBox();
        Queue<MovecraftLocation> queue = Lists.newLinkedList(validExterior);
        while (!queue.isEmpty()) {
            MovecraftLocation node = queue.poll();
            if (visited.contains(node))
                continue;
            visited.add(node);
            //If the node is already a valid member of the exterior of the HitBox, continued search is unitary.
            for (MovecraftLocation neighbor : CollectionUtils.neighbors(invertedHitBox, node)) {
                queue.add(neighbor);
            }
        }
        confirmed.addAll(visited);
        entireHitbox.addAll(invertedHitBox.difference(confirmed));

        var waterData = Bukkit.createBlockData(Material.WATER);
        for (MovecraftLocation location : entireHitbox) {
            if (location.getY() <= waterLine) {
                c.getPhaseBlocks().put(location.toBukkit(badWorld), waterData);
            }
        }
    }

    @Override
    public void run() {
        var start = System.nanoTime();
        frontier();
        if(!illegal.isEmpty()) {
            return;
        }
        var result = completionValidators.stream().reduce(TaskPredicate::and).orElse((a,b,c,d) -> Result.fail()).validate(materials, craft.getType(), world, player);
        if(!result.isSucess()){
            player.sendMessage(result.getMessage());
            return;
        }
        craft.setHitBox(new BitmapHitBox(legal));
        craft.setNotificationPlayer(player);
        craft.setOrigBlockCount(craft.getHitBox().size());
        water(craft); //TODO: Remove
        final CraftDetectEvent event = new CraftDetectEvent(craft);

        WorldManager.INSTANCE.executeMain(()-> Bukkit.getPluginManager().callEvent(event));
        if (event.isCancelled()) {
            craft.getAudience().sendMessage(Component.text(event.getFailMessage()));
            return;
        }
        craft.getAudience().sendMessage(Component.text(String.format("%s Size: %s", I18nSupport.getInternationalisedString("Detection - Successfully piloted craft"), craft.getHitBox().size())));
        Movecraft.getInstance().getLogger().info(String.format(
                I18nSupport.getInternationalisedString("Detection - Success - Log Output"),
                player == null ? "null" : player.getName(), craft.getType().getCraftName(), craft.getHitBox().size(),
                craft.getHitBox().getMinX(), craft.getHitBox().getMinZ()));
        CraftManager.getInstance().addCraft(craft);
    }

    private void frontier(){
        ConcurrentLinkedQueue<MovecraftLocation> currentFrontier = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<MovecraftLocation> nextFrontier = new ConcurrentLinkedQueue<>();
        currentFrontier.add(startLocation);
        currentFrontier.addAll(Arrays.stream(SHIFTS).map(startLocation::add).collect(Collectors.toList()));
        for(var location : currentFrontier){
            visited.add(location);
        }
        int threads = Runtime.getRuntime().availableProcessors();
        while (!currentFrontier.isEmpty() && size.intValue() < craft.getType().getMaxSize()) {
            List<Callable<Object>> tasks = new ArrayList<>();
            for(int j = 0; j < threads ; j++) {
                tasks.add(Executors.callable(new DetectAction(currentFrontier, nextFrontier)));
            }
            ForkJoinPool.commonPool().invokeAll(tasks);
            currentFrontier = nextFrontier;
            nextFrontier = new ConcurrentLinkedQueue<>();
        }
    }

    @Override
    public String toString(){
        return String.format("DetectionTask{%s}", this.craft);
    }

    private class DetectAction implements Runnable{
        private final ConcurrentLinkedQueue<MovecraftLocation> currentFrontier;
        private final ConcurrentLinkedQueue<MovecraftLocation> nextFrontier;

        private DetectAction(ConcurrentLinkedQueue<MovecraftLocation> currentFrontier, ConcurrentLinkedQueue<MovecraftLocation> nextFrontier) {
            this.currentFrontier = currentFrontier;
            this.nextFrontier = nextFrontier;
        }

        @Override
        public void run() {
            MovecraftLocation probe;
            while((probe = currentFrontier.poll())!=null) {
                if(!ALLOWED_BLOCK_VALIDATOR.validate(probe, craft.getType(), world, player).isSucess()){
                    continue;
                }
                TaskPredicate<MovecraftLocation> chain = FORBIDDEN_BLOCK_VALIDATOR;
                for (var validator : validators) {
                    chain = chain.and(validator);
                }
                var result = chain.validate(probe, craft.getType(), world, player);
                if(result.isSucess()){
                    legal.add(probe);
                    size.increment();
                    materials.computeIfAbsent(world.getMaterial(probe), Functions.forSupplier(ConcurrentLinkedDeque::new)).add(probe);
                    for(int i = 0; i< SHIFTS.length; i++){
                        var shifted = probe.add(SHIFTS[i]);
                        if(visited.add(shifted)) {
                            nextFrontier.add(shifted);
                        }
                    }
                } else {
                    illegal.add(probe);
                    player.sendMessage(result.getMessage());
                }
            }

        }
    }
}
