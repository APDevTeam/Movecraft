package net.countercraft.movecraft.processing.functions;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface public interface DyadicPredicate<T, U> {

    @Contract(pure = true)
    @NotNull Result validate(@NotNull T t, @NotNull U u);

    @Contract(pure = true)
    default @NotNull DyadicPredicate<T, U> or(@NotNull DyadicPredicate<T, U> other){
        return (t,u) -> {
            var result = this.validate(t, u);
            if(result.isSucess()){
                return result;
            }
            return other.validate(t,u);
        };
    }

    @Contract(pure = true)
    default @NotNull DyadicPredicate<T, U> and(@NotNull DyadicPredicate<T, U> other){
        return (t,u) -> {
            var result = this.validate(t, u);
            if(!result.isSucess()){
                return result;
            }
            return other.validate(t, u);
        };
    }

    @Contract(pure = true)
    default @NotNull MonadicPredicate<T> fixSecond(U u){
        return (t) -> this.validate(t, u);
    }

    @Contract(pure = true)
    default @NotNull MonadicPredicate<U> fixFirst(T t){
        return (u) -> this.validate(t, u);
    }

    @Contract(pure = true)
    default <V> @NotNull TriadicPredicate<V, T, U> expandFirst(){
        return (v, t, u) -> this.validate(t,u);
    }

    @Contract(pure = true)
    default <V> @NotNull TriadicPredicate<T, V, U> expandSecond(){
        return (t, v, u) -> this.validate(t,u);
    }

    @Contract(pure = true)
    default <V> @NotNull TriadicPredicate<T, U, V> expandThird(){
        return (t, u, v) -> this.validate(t,u);
    }

}
