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
    private static final @NotNull MovementData DEFAULT = new MovementData(ZERO, MovecraftRotation.NONE, null);
    private final @NotNull CraftDataTagKey<MovementData> _dataTagKey;

    public MovementController(Plugin plugin) {
        _dataTagKey = CraftDataTagRegistry.INSTANCE.registerTagKey(
            new NamespacedKey(plugin, "movement"),
            craft -> DEFAULT);
    }

    public void move(@NotNull Craft craft, @Nullable MovecraftLocation translation, @Nullable MovecraftRotation rotation, @Nullable MovecraftWorld changeWorld){
        craft.computeDataTag(_dataTagKey, (data) -> data.merge(new MovementData(
            translation == null ? ZERO : translation,
            rotation == null ? MovecraftRotation.NONE : rotation,
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

    private record MovementData(@NotNull MovecraftLocation translation, @NotNull MovecraftRotation rotation, @Nullable MovecraftWorld world){

        private static MovecraftRotation add(MovecraftRotation a, MovecraftRotation b){
            if(a == MovecraftRotation.NONE || a==b){
                return b;
            }
            if(b == MovecraftRotation.NONE){
                return a;
            }

            return MovecraftRotation.NONE;
        }

        @Contract("_ -> new")
        public @NotNull MovementData merge(@NotNull MovementData other){
            // TODO: Correct this
            return new MovementData(translation.add(other.translation), add(rotation, other.rotation), this.world == null ? other.world : this.world);
        }
    }
}
