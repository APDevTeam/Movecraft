package net.countercraft.movecraft.processing.functions;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface public interface MonadicPredicate<T> {

    @Contract(pure = true)
    @NotNull Result validate(@NotNull T t);

    @Contract(pure = true)
    default @NotNull MonadicPredicate<T> or(@NotNull MonadicPredicate<T> other){
        return t -> {
            var result = this.validate(t);
            if(result.isSucess()){
                return result;
            }
            return other.validate(t);
        };
    }

    @Contract(pure = true)
    default @NotNull MonadicPredicate<T> and(@NotNull MonadicPredicate<T> other){
        return t -> {
            var result = this.validate(t);
            if(!result.isSucess()){
                return result;
            }
            return other.validate(t);
        };
    }

    @Contract(pure = true)
    default <U> @NotNull DyadicPredicate<U, T> expandFirst(){
        return (u,t) -> this.validate(t);
    }

    @Contract(pure = true)
    default <U> @NotNull DyadicPredicate<T, U> expandSecond(){
        return (t, u) -> this.validate(t);
    }

}
