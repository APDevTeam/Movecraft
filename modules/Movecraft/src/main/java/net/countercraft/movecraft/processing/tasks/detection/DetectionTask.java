package net.countercraft.movecraft.processing.tasks.detection;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.CruiseOnPilotCraft;
import net.countercraft.movecraft.craft.SubCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.processing.effects.Effect;
import net.countercraft.movecraft.processing.functions.DetectionPredicate;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.processing.tasks.detection.validators.AllowedBlockValidator;
import net.countercraft.movecraft.processing.tasks.detection.validators.AlreadyControlledValidator;
import net.countercraft.movecraft.processing.tasks.detection.validators.AlreadyPilotingValidator;
import net.countercraft.movecraft.processing.tasks.detection.validators.FlyBlockValidator;
import net.countercraft.movecraft.processing.tasks.detection.validators.ForbiddenBlockValidator;
import net.countercraft.movecraft.processing.tasks.detection.validators.ForbiddenSignStringValidator;
import net.countercraft.movecraft.processing.tasks.detection.validators.NameSignValidator;
import net.countercraft.movecraft.processing.tasks.detection.validators.PilotSignValidator;
import net.countercraft.movecraft.processing.tasks.detection.validators.SizeValidator;
import net.countercraft.movecraft.processing.tasks.detection.validators.SubcraftValidator;
import net.countercraft.movecraft.processing.tasks.detection.validators.WaterContactValidator;
import net.countercraft.movecraft.util.AtomicLocationSet;
import net.countercraft.movecraft.util.CollectionUtils;
import net.countercraft.movecraft.util.Tags;
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.countercraft.movecraft.util.hitboxes.SetHitBox;
import net.countercraft.movecraft.util.hitboxes.SolidHitBox;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DetectionTask implements Supplier<Effect> {
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
    private final ConcurrentMap<Material, Deque<MovecraftLocation>> visitedMaterials = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<MovecraftLocation> legal = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<MovecraftLocation> fluid = new ConcurrentLinkedDeque<>();
    private static final AllowedBlockValidator ALLOWED_BLOCK_VALIDATOR = new AllowedBlockValidator();
    private static final ForbiddenBlockValidator FORBIDDEN_BLOCK_VALIDATOR = new ForbiddenBlockValidator();
    private static final SubcraftValidator SUBCRAFT_VALIDATOR = new SubcraftValidator();
    private static final AlreadyControlledValidator ALREADY_CONTROLLED_VALIDATOR = new AlreadyControlledValidator();
    private static final AlreadyPilotingValidator ALREADY_PILOTING_VALIDATOR = new AlreadyPilotingValidator();
    private static final List<DetectionPredicate<MovecraftLocation>> validators = List.of(
            new ForbiddenSignStringValidator(),
            new NameSignValidator(),
            new PilotSignValidator()
    );
    private static final List<DetectionPredicate<Map<Material, Deque<MovecraftLocation>>>> completionValidators = List.of(
            new SizeValidator(),
            new FlyBlockValidator()
    );
    private static final List<DetectionPredicate<Map<Material, Deque<MovecraftLocation>>>> visitedValidators = List.of(
            new WaterContactValidator()
    );

    public DetectionTask(@NotNull Craft craft, @NotNull MovecraftLocation startLocation, @NotNull MovecraftWorld world, @Nullable Player player) {
        this.craft = craft;
        this.startLocation = startLocation;
        this.world = world;
        this.player = player;
        visited = new AtomicLocationSet();
    }

    @Deprecated
    private Effect water() {
        final int waterLine = WorldManager.INSTANCE.executeMain(craft::getWaterLine);
        if (craft.getType().getBoolProperty(CraftType.BLOCKED_BY_WATER) || craft.getHitBox().getMinY() > waterLine)
            return null;

        var badWorld = WorldManager.INSTANCE.executeMain(craft::getWorld);
        //The subtraction of the set of coordinates in the HitBox cube and the HitBox itself
        final HitBox invertedHitBox = new BitmapHitBox(craft.getHitBox().boundingHitBox()).difference(craft.getHitBox());

        //A set of locations that are confirmed to be "exterior" locations
        final SetHitBox confirmed = new SetHitBox();
        final SetHitBox entireHitbox = new SetHitBox(craft.getHitBox());

        //place phased blocks
        final Set<Location> overlap = new HashSet<>(craft.getPhaseBlocks().keySet());
        overlap.retainAll(craft.getHitBox().asSet().stream().map(l -> l.toBukkit(badWorld)).collect(Collectors.toSet()));
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
        final SetHitBox validExterior = new SetHitBox();
        for (HitBox hitBox : surfaces) {
            validExterior.addAll(new BitmapHitBox(hitBox).difference(craft.getHitBox()));
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
        return () -> {
            for (MovecraftLocation location : entireHitbox) {
                if (location.getY() <= waterLine) {
                    craft.getPhaseBlocks().put(location.toBukkit(badWorld), waterData);
                }
            }
        };

    }

    private void subcraft() {
        if(!(craft instanceof SubCraft) && !(craft instanceof CruiseOnPilotCraft))
            return; // Return no-op for non-subcraft and non-cruise on pilots

        Craft parent = null;
        for(MovecraftLocation loc : craft.getHitBox()) {
            for(var otherCraft : CraftManager.getInstance()) {
                if(!otherCraft.getMovecraftWorld().equals(world))
                    continue;

                if(otherCraft.getHitBox().contains(loc))
                    parent = otherCraft;
            }
        }
        if(parent == null)
            throw new IllegalStateException("Subcrafts should always have a parent");

        if(craft instanceof SubCraft)
            ((SubCraft) craft).setParent(parent);

        // Subtract the subcraft from the hitbox of the parent.
        var parentHitBox = parent.getHitBox();
        parentHitBox = parentHitBox.difference(craft.getHitBox());
        parent.setHitBox(parentHitBox);
    }

    @Override
    public @Nullable Effect get() {
        frontier();
        if(!illegal.isEmpty())
            return null;

        var result = completionValidators.stream().reduce(DetectionPredicate::and).orElse((a, b, c, d) -> Result.fail()).validate(materials, craft.getType(), world, player);
        // If the craft is not a subcraft, must not be a subcraft, and is not a cruise on pilot, run the already piloting validator
        if(!(craft instanceof SubCraft) && !craft.getType().getBoolProperty(CraftType.MUST_BE_SUBCRAFT) && !craft.getType().getBoolProperty(CraftType.CRUISE_ON_PILOT))
            result = result.isSucess() ? ALREADY_PILOTING_VALIDATOR.validate(materials, craft.getType(), world, player) : result;

        result = result.isSucess() ? visitedValidators.stream().reduce(DetectionPredicate::and).orElse((a, b, c, d) -> Result.fail()).validate(visitedMaterials, craft.getType(), world, player) : result;
        if(!result.isSucess()) {
            Result finalResult = result;
            return () -> craft.getAudience().sendMessage(Component.text(finalResult.getMessage()));
        }

        craft.setHitBox(new BitmapHitBox(legal));
        craft.setFluidLocations(new BitmapHitBox(fluid));
        craft.setNotificationPlayer(player);
        craft.setOrigBlockCount(craft.getHitBox().size());

        subcraft();
        var effect = water(); //TODO: Remove

        final CraftDetectEvent event = new CraftDetectEvent(craft);

        WorldManager.INSTANCE.executeMain(()-> Bukkit.getPluginManager().callEvent(event));
        if (event.isCancelled()) {
            return () -> craft.getAudience().sendMessage(Component.text(event.getFailMessage()));
        }
        return ((Effect)() -> {
            craft.getAudience().sendMessage(Component.text(String.format("%s Size: %s", I18nSupport.getInternationalisedString("Detection - Successfully piloted craft"), craft.getHitBox().size())));
            Movecraft.getInstance().getLogger().info(String.format(
                    I18nSupport.getInternationalisedString("Detection - Success - Log Output"),
                    player == null ? "null" : player.getName(), craft.getType().getStringProperty(CraftType.NAME), craft.getHitBox().size(),
                    craft.getHitBox().getMinX(), craft.getHitBox().getMinZ()));
            CraftManager.getInstance().addCraft(craft);
        }).andThen(effect);
    }

    private void frontier(){
        ConcurrentLinkedQueue<MovecraftLocation> currentFrontier = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<MovecraftLocation> nextFrontier = new ConcurrentLinkedQueue<>();
        currentFrontier.add(startLocation);
        currentFrontier.addAll(Arrays.stream(SHIFTS).map(startLocation::add).collect(Collectors.toList()));
        for(var location : currentFrontier) {
            visited.add(location);
        }
        int threads = Runtime.getRuntime().availableProcessors();
        while (!currentFrontier.isEmpty() && size.intValue() < craft.getType().getIntProperty(CraftType.MAX_SIZE)) {
            List<ForkJoinTask<?>> tasks = new ArrayList<>();
            for(int j = 0; j < threads ; j++) {
                tasks.add(ForkJoinPool.commonPool().submit(new DetectAction(currentFrontier, nextFrontier)));
            }
            for(var task : tasks){
                task.join();
                if(task.getException() != null){
                    task.getException().printStackTrace();
                }
            }
            currentFrontier = nextFrontier;
            nextFrontier = new ConcurrentLinkedQueue<>();
        }
    }

    @Override
    public String toString(){
        return String.format("DetectionTask{%s}", this.craft);
    }

    private class DetectAction implements Runnable {
        private final ConcurrentLinkedQueue<MovecraftLocation> currentFrontier;
        private final ConcurrentLinkedQueue<MovecraftLocation> nextFrontier;

        private DetectAction(ConcurrentLinkedQueue<MovecraftLocation> currentFrontier, ConcurrentLinkedQueue<MovecraftLocation> nextFrontier) {
            this.currentFrontier = currentFrontier;
            this.nextFrontier = nextFrontier;
        }

        @Override
        public void run() {
            MovecraftLocation probe;
            while((probe = currentFrontier.poll()) != null) {
                visitedMaterials.computeIfAbsent(world.getMaterial(probe), Functions.forSupplier(ConcurrentLinkedDeque::new)).add(probe);
                if(!ALLOWED_BLOCK_VALIDATOR.validate(probe, craft.getType(), world, player).isSucess())
                    continue;

                DetectionPredicate<MovecraftLocation> chain = FORBIDDEN_BLOCK_VALIDATOR;
                for (var validator : validators) {
                    chain = chain.and(validator);
                }
                // If a craft is piloted as a subcraft, run the subcraft validator
                if(craft instanceof SubCraft)
                    chain = chain.and(SUBCRAFT_VALIDATOR);
                // If the craft is not a subcraft or cruise on pilot, run the already controlled validator
                else if(!craft.getType().getBoolProperty(CraftType.CRUISE_ON_PILOT))
                    chain = chain.and(ALREADY_CONTROLLED_VALIDATOR);

                var result = chain.validate(probe, craft.getType(), world, player);
                if(result.isSucess()) {
                    legal.add(probe);
                    if (Tags.FLUID.contains(world.getMaterial(probe)))
                        fluid.add(probe);

                    size.increment();
                    materials.computeIfAbsent(world.getMaterial(probe), Functions.forSupplier(ConcurrentLinkedDeque::new)).add(probe);
                    for(MovecraftLocation shift : SHIFTS) {
                        var shifted = probe.add(shift);
                        if(visited.add(shifted))
                            nextFrontier.add(shifted);
                    }
                }
                else {
                    illegal.add(probe);
                    craft.getAudience().sendMessage(Component.text(result.getMessage()));
                }
            }
        }
    }
}
