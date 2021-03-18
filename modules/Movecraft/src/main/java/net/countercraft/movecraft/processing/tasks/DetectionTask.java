package net.countercraft.movecraft.processing.tasks;

import com.google.common.base.Functions;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.WorldTask;
import net.countercraft.movecraft.processing.tasks.detection.AllowedBlockValidator;
import net.countercraft.movecraft.processing.tasks.detection.DetectionValidator;
import net.countercraft.movecraft.processing.tasks.detection.ForbiddenBlockValidator;
import net.countercraft.movecraft.processing.tasks.detection.ForbiddenSignStringValidator;
import net.countercraft.movecraft.processing.tasks.detection.Modifier;
import net.countercraft.movecraft.processing.tasks.detection.NameSignValidator;
import net.countercraft.movecraft.processing.tasks.detection.PilotSignValidator;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DetectionTask extends WorldTask {
    private final static MovecraftLocation[] SHIFTS = {
            new MovecraftLocation(0, 0, 1),
            new MovecraftLocation(0, 1, 0),
            new MovecraftLocation(1, 0 ,0),
            new MovecraftLocation(0, 0, -1),
            new MovecraftLocation(0, -1, 0),
            new MovecraftLocation(-1, 0, 0)};
    private static final Object PRESENT = new Object();
    private final Craft craft;
    private final MovecraftLocation startLocation;
    private final Player player;
    private final AtomicInteger size = new AtomicInteger(0);
    private final ConcurrentMap<MovecraftLocation, Object> visited = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<MovecraftLocation> illegal = new ConcurrentLinkedDeque<>();
    private final ConcurrentMap<Material, ConcurrentLinkedDeque<MovecraftLocation>> materials = new ConcurrentHashMap<>();
    private static final List<DetectionValidator> validators = List.of(
            new AllowedBlockValidator(),
            new ForbiddenBlockValidator(),
            new ForbiddenSignStringValidator(),
            new NameSignValidator(),
            new PilotSignValidator());

    public DetectionTask(Craft craft, @NotNull MovecraftLocation startLocation, MovecraftWorld world, Player player) {
        super(world);
        this.craft = craft;
        this.startLocation = startLocation;
        this.player = player;
    }

    @Override
    public void compute(MovecraftWorld world) {
        long start = System.nanoTime();
        new DetectAction(startLocation).invoke();
        //todo: validate materials
//        if(illegal.isEmpty()){
//            return;
//        }
//        if(player == null) return;
//        for(var location : illegal){
//            BlockHighlight.highlightBlockAt(location.toBukkit(craft.getWorld()), player);
//        }
    }

    @Override
    public String toString(){
        return "Detection task of " + this.craft;
    }

    private class DetectAction extends RecursiveAction{
        private final MovecraftLocation probe;

        private DetectAction(MovecraftLocation probe) {
            this.probe = probe;
        }

        @Override
        protected void compute() {
            // by using putIfAbsent, we guarantee that there will only ever be one successful computation on each
            // location. This is as opposed to using containsKey and than using put after, which is not atomic. and
            // thus a race condition.
            if(visited.putIfAbsent(probe, PRESENT) != null){
                return;
            }
            Modifier status = Modifier.NONE;
            for(var validator : validators){
                status = status.merge(validator.validate(probe, craft.getType(), world, player));
            }
            switch (status){
                case FAIL:
                    illegal.add(probe);
                case NONE:
                    break;
                case PERMIT:
                    materials.computeIfAbsent(world.getMaterial(probe), Functions.forSupplier(ConcurrentLinkedDeque::new)).add(probe);
                    invokeAll(Arrays.stream(SHIFTS)
                            .map(location -> location.add(probe))
                            .map(DetectAction::new)
                            .collect(Collectors.toList()));
            }
        }
    }
}
