package net.countercraft.movecraft.processing.functions;

import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.processing.MovecraftWorld;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a data carrying predicate, which can return a message depending on the success state of its evaluation
 * @param <T>
 */
@FunctionalInterface public interface DetectionPredicate<T> extends TetradicPredicate<T, CraftType, MovecraftWorld, Player>{

    @Override
    @Contract(pure = true)
    @NotNull Result validate(@NotNull T t, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable Player player);

    @Contract(pure = true)
    default @NotNull DetectionPredicate<T> or(@NotNull DetectionPredicate<T> other){
        return (t, type, world, player) -> {
            var result = this.validate(t, type, world, player);
            if(result.isSucess()){
                return result;
            }
            return other.validate(t,type,world,player);
        };
    }

    @Contract(pure = true)
    default @NotNull DetectionPredicate<T> and(@NotNull DetectionPredicate<T> other){
        return (t, type, world, player) -> {
            var result = this.validate(t, type, world, player);
            if(!result.isSucess()){
                return result;
            }
            return other.validate(t,type,world,player);
        };
    }

}
