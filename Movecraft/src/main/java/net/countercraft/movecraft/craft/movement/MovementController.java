package net.countercraft.movecraft.craft.movement;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.datatag.CraftDataTagKey;
import net.countercraft.movecraft.craft.datatag.CraftDataTagRegistry;
import net.countercraft.movecraft.processing.MovecraftWorld;
import org.bukkit.Effect;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class MovementController {
    private static final @NotNull MovecraftLocation ZERO = new MovecraftLocation(0,0,0);
    private static final @NotNull MovementData DEFAULT = new MovementData(AffineTransformation.NONE, null);
    private final @NotNull CraftDataTagKey<MovementData> _dataTagKey;

    public MovementController(Plugin plugin) {
        _dataTagKey = CraftDataTagRegistry.INSTANCE.registerTagKey(
            new NamespacedKey(plugin, "movement"),
            craft -> DEFAULT);
    }

    public void move(@NotNull Craft craft, @Nullable MovecraftLocation translation, @Nullable MovecraftRotation rotation, @Nullable MovecraftWorld changeWorld){
        craft.computeDataTag(_dataTagKey, (data) -> data.merge(new MovementData(
            AffineTransformation.of(translation).mult(AffineTransformation.of(rotation)),
            changeWorld)));
    }

    private MovementData reset(@NotNull Craft craft){
        return craft.setDataTag(_dataTagKey, DEFAULT);
    }

    private class MovementTask implements Supplier<Effect> {

        private final @NotNull Craft craft;

        public MovementTask(@NotNull Craft craft){
            this.craft = craft;
        }

        @Override
        public @Nullable Effect get() {
            // Reset the current movement tracker
            var movement = reset(craft);

            return null;
        }
    }

    // TODO: Implement int matrix or find library (most are for floating point)
    private record AffineTransformation(){
        public static @NotNull AffineTransformation NONE = null;
        public static @NotNull AffineTransformation of(MovecraftLocation translation){ return null; }
        public static @NotNull AffineTransformation of(MovecraftRotation rotation){ return null; }
        public @NotNull AffineTransformation mult(AffineTransformation other){ return null; }
        public @NotNull MovecraftLocation apply(MovecraftLocation location){ return location; }
    }

    private record MovementData(@NotNull AffineTransformation transformation, @Nullable MovecraftWorld world){
        @Contract("_ -> new")
        public @NotNull MovementData merge(@NotNull MovementData other){
            // TODO: Correct this
            return new MovementData(transformation.mult(other.transformation), this.world == null ? other.world : this.world);
        }
    }
}
