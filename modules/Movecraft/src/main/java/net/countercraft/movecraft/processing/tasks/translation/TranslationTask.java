package net.countercraft.movecraft.processing.tasks.translation;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.SinkingCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftCollisionEvent;
import net.countercraft.movecraft.events.CraftPreTranslateEvent;
import net.countercraft.movecraft.events.CraftTranslateEvent;
import net.countercraft.movecraft.events.FuelBurnEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.CachedMovecraftWorld;
import net.countercraft.movecraft.processing.MovecraftWorld;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.processing.effects.Effect;
import net.countercraft.movecraft.processing.functions.MonadicPredicate;
import net.countercraft.movecraft.processing.functions.Result;
import net.countercraft.movecraft.processing.functions.TetradicPredicate;
import net.countercraft.movecraft.processing.tasks.translation.effects.TeleportationEffect;
import net.countercraft.movecraft.processing.tasks.translation.validators.HoverValidator;
import net.countercraft.movecraft.processing.tasks.translation.validators.MaxHeightValidator;
import net.countercraft.movecraft.processing.tasks.translation.validators.MinHeightValidator;
import net.countercraft.movecraft.processing.tasks.translation.validators.WorldBorderValidator;
import net.countercraft.movecraft.util.Tags;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.countercraft.movecraft.util.hitboxes.SetHitBox;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TranslationTask implements Supplier<Effect> {

    private static final List<MonadicPredicate<Craft>> preTranslationValidators = new ArrayList<>();
    static {
        preTranslationValidators.add((craft -> craft.getHitBox().isEmpty() ? Result.failWithMessage("Empty hitbox") : Result.succeed()));
        preTranslationValidators.add((craft -> craft.getDisabled() && !(craft instanceof SinkingCraft)
                ? Result.failWithMessage(
                        I18nSupport.getInternationalisedString("Translation - Failed Craft Is Disabled"))
                : Result.succeed()));
    }
    private static final List<TetradicPredicate<MovecraftLocation, MovecraftWorld, HitBox, CraftType>> translationValidators = new ArrayList<>();
    static {
        translationValidators.add(new MinHeightValidator());
        translationValidators.add(new MaxHeightValidator());
        translationValidators.add(new HoverValidator());
        translationValidators.add(new WorldBorderValidator());
    }

    private MovecraftRotation rotation;
    private final Craft craft;
    private MovecraftWorld destinationWorld;
    private MovecraftLocation translation;

    public TranslationTask(@NotNull Craft craft, @NotNull MovecraftLocation translation, @NotNull MovecraftWorld destinationWorld, @NotNull MovecraftRotation rotation) {
        this.rotation = rotation;
        this.craft = craft;
        this.translation = translation;
        this.destinationWorld = destinationWorld;
    }

    @Override
    public Effect get() {
        var preTranslationResult = preTranslationValidators.stream().reduce(MonadicPredicate::and).orElseThrow().validate(craft);
        if(!preTranslationResult.isSucess()){
            return () -> craft.getAudience().sendMessage(Component.text(preTranslationResult.getMessage()));
        }
        var preTranslateEvent = WorldManager.INSTANCE.executeMain(()->{
            var event = new CraftPreTranslateEvent(craft, translation.getX(), translation.getY(), translation.getZ(), craft.getWorld());
            Bukkit.getServer().getPluginManager().callEvent(event);
            return event;
        });
        if (preTranslateEvent.isCancelled()) {
            return ()-> craft.getAudience().sendMessage(Component.text(preTranslateEvent.getFailMessage()));
        }
        translation = new MovecraftLocation(preTranslateEvent.getDx(), preTranslateEvent.getDy(), preTranslateEvent.getDz());
        destinationWorld = CachedMovecraftWorld.of(preTranslateEvent.getWorld());
        //TODO: Portal movement
        //TODO: Gravity
        var destinationLocations = new SetHitBox();
        var collisions = new SetHitBox();
        var phaseLocations = new SetHitBox();
        var harvestLocations = new SetHitBox();
        var fuelSources = new ArrayList<FurnaceInventory>();
        for(var originLocation : craft.getHitBox()){
            var originMaterial = craft.getMovecraftWorld().getMaterial(originLocation);
            // Remove air from hitboxes
            if(originMaterial.isAir())
                continue;
            if(Tags.FURNACES.contains(originMaterial)) {
                var state = craft.getMovecraftWorld().getState(originLocation);
                if(state instanceof FurnaceInventory)
                    fuelSources.add((FurnaceInventory) state);
            }

            var destination = originLocation.add(translation);

            destinationLocations.add(destination);
            // previous locations cannot collide
            if(craft.getMovecraftWorld().equals(destinationWorld) && craft.getHitBox().contains(destination)){
                continue;
            }
            var destinationMaterial = destinationWorld.getMaterial(destination);
            if(craft.getType().getMaterialSetProperty(CraftType.PASSTHROUGH_BLOCKS).contains(destinationMaterial)){
                phaseLocations.add(destination);
                continue;
            }
            if(craft.getType().getMaterialSetProperty(CraftType.HARVEST_BLOCKS).contains(destinationMaterial) &&
                    craft.getType().getMaterialSetProperty(CraftType.HARVESTER_BLADE_BLOCKS).contains(originMaterial)){
                harvestLocations.add(destination);
                continue;
            }
            collisions.add(destination);
        }
        double fuelBurnRate = (double) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_FUEL_BURN_RATE, preTranslateEvent.getWorld());
        Effect fuelBurnEffect;
        if (craft.getBurningFuel() >= fuelBurnRate) {
            //call event
            final FuelBurnEvent event = new FuelBurnEvent(craft, craft.getBurningFuel(), fuelBurnRate);
            submitEvent(event);
            fuelBurnEffect = () -> craft.setBurningFuel(event.getBurningFuel() - event.getFuelBurnRate());
        } else {
            var fuelSource = findFuelHolders(craft.getType(), fuelSources);
            if(fuelSource == null){
                return () -> craft.getAudience().sendMessage(I18nSupport.getInternationalisedComponent("Translation - Failed Craft out of fuel"));
            }
            callFuelEvent(craft, findFuelStack(craft.getType(), fuelSource));
            //TODO: Take Fuel
            fuelBurnEffect = () -> Bukkit.getLogger().info("This is where we'd take ur fuel, if we had some");
        }
        var translationResult = translationValidators.stream().reduce(TetradicPredicate::and).orElseThrow().validate(translation, destinationWorld, destinationLocations, craft.getType());
        if(!translationResult.isSucess()){
            return () -> craft.getAudience().sendMessage(Component.text(translationResult.getMessage()));
        }



        // Direct float comparison due to check for statically initialized value
        callCollisionEvent(craft, collisions, preTranslateEvent.getWorld());
        if(craft.getType().getFloatProperty(CraftType.COLLISION_EXPLOSION) <= 0F && !collisions.isEmpty()){
            //TODO: collision highlights
            return () -> craft.getAudience().sendMessage(Component.text(String.format(I18nSupport.getInternationalisedString("Translation - Failed Craft is obstructed") + " @ %d,%d,%d,%s", 0, 0, 0, "not_implemented")));
        }
        Effect fluidBoxEffect = fluidBox(craft, translation);
        var  translateEvent = callTranslateEvent(craft, destinationLocations, preTranslateEvent.getWorld());
        //TODO: Sinking?
        //TODO: Collision explosion
        //TODO: phase blocks
        Effect movementEffect = moveCraft();
        //TODO: un-phase blocks
        Effect teleportEffect = new TeleportationEffect(craft, translation, translateEvent.getWorld());
        return fuelBurnEffect
                .andThen(fluidBoxEffect)
                .andThen(movementEffect)
                .andThen(teleportEffect);
    }

    @Contract("_ -> param1")
    private static <T extends Event> T submitEvent(@NotNull T event ){
        WorldManager.INSTANCE.executeMain(() -> Bukkit.getServer().getPluginManager().callEvent(event));
        return event;
    }

    private static @NotNull Effect moveCraft(){
        return null;
    }

    private static @NotNull Effect fluidBox(Craft craft, MovecraftLocation translation){
        var newFluids = new SetHitBox();
        for(var location : craft.getFluidLocations()){
            newFluids.add(location.add(translation));
        }
        return () -> craft.setFluidLocations(newFluids);
    }

    private static @NotNull CraftCollisionEvent callCollisionEvent(@NotNull Craft craft, @NotNull HitBox collided, @NotNull World destinationWorld){
        return submitEvent(new CraftCollisionEvent(craft, collided, destinationWorld));
    }

    private static @NotNull CraftTranslateEvent callTranslateEvent(@NotNull Craft craft, @NotNull HitBox destinationHitBox, @NotNull World destinationWorld){
        return submitEvent(new CraftTranslateEvent(craft, craft.getHitBox(), destinationHitBox, destinationWorld));
    }

    private static @Nullable FurnaceInventory findFuelHolders(CraftType type, List<FurnaceInventory> inventories){
        for(var inventory : inventories){
            var stack = findFuelStack(type, inventory);
            if(stack != null){
                return inventory;
            }
        }
        return null;
    }

    private static @Nullable ItemStack findFuelStack(@NotNull CraftType type, @NotNull FurnaceInventory inventory){
        var v = type.getObjectProperty(CraftType.FUEL_TYPES);
        if(!(v instanceof Map<?, ?>))
            throw new IllegalStateException("FUEL_TYPES must be of type Map");
        var fuelTypes = (Map<?, ?>) v;
        for(var e : fuelTypes.entrySet()) {
            if(!(e.getKey() instanceof Material))
                throw new IllegalStateException("Keys in FUEL_TYPES must be of type Material");
            if(!(e.getValue() instanceof Double))
                throw new IllegalStateException("Values in FUEL_TYPES must be of type Double");
        }

        for(var item : inventory) {
            if(item == null || !fuelTypes.containsKey(item.getType())){
                continue;
            }
            return item;
        }
        return null;
    }

    private static @NotNull FuelBurnEvent callFuelEvent(@NotNull Craft craft, @NotNull ItemStack burningFuel) {
        var v = craft.getType().getObjectProperty(CraftType.FUEL_TYPES);
        if(!(v instanceof Map<?, ?>))
            throw new IllegalStateException("FUEL_TYPES must be of type Map");
        var fuelTypes = (Map<?, ?>) v;
        for(var e : fuelTypes.entrySet()) {
            if(!(e.getKey() instanceof Material))
                throw new IllegalStateException("Keys in FUEL_TYPES must be of type Material");
            if(!(e.getValue() instanceof Double))
                throw new IllegalStateException("Values in FUEL_TYPES must be of type Double");
        }

        double fuelBurnRate = (double) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_FUEL_BURN_RATE, craft.getWorld());
        return submitEvent(new FuelBurnEvent(craft, (double) fuelTypes.get(burningFuel.getType()), fuelBurnRate));
    }
}
