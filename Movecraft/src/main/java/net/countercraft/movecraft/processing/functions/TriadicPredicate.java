package net.countercraft.movecraft.processing.functions;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface public interface TriadicPredicate<T, U, V> {

    @Contract(pure = true)
    @NotNull Result validate(@NotNull T t, @NotNull U u, @NotNull V v);

    @Contract(pure = true)
    default @NotNull TriadicPredicate<T, U, V> or(@NotNull TriadicPredicate<T, U, V> other){
        return (t, u, v) -> {
            var result = this.validate(t, u, v);
            if(result.isSucess()){
                return result;
            }
            return other.validate(t, u, v);
        };
    }

    @Contract(pure = true)
    default @NotNull TriadicPredicate<T, U, V> and(@NotNull TriadicPredicate<T, U, V> other){
        return (t, u, v) -> {
            var result = this.validate(t, u, v);
            if(!result.isSucess()){
                return result;
            }
            return other.validate(t, u, v);
        };
    }

    @Contract(pure = true)
    default @NotNull DyadicPredicate<U, V> fixFirst(T t){
        return (u, v) -> this.validate(t, u, v);
    }

    @Contract(pure = true)
    default @NotNull DyadicPredicate<T, V> fixSecond(U u){
        return (t,v) -> this.validate(t, u, v);
    }

    @Contract(pure = true)
    default @NotNull DyadicPredicate<T, U> fixThird(V v){
        return (t,u) -> this.validate(t, u, v);
    }

    @Contract(pure = true)
    default <W> @NotNull TetradicPredicate<W, T, U, V> expandFirst(){
        return (w, t, u, v) -> this.validate(t,u,v);
    }

    @Contract(pure = true)
    default <W> @NotNull TetradicPredicate<T, W, U, V> expandSecond(){
        return (t, w, u, v) -> this.validate(t, u, v);
    }

    @Contract(pure = true)
    default <W> @NotNull TetradicPredicate<T, U, W, V> expandThird(){
        return (t, u, w, v) -> this.validate(t, u, v);
    }

    @Contract(pure = true)
    default <W> @NotNull TetradicPredicate<T, U, V, W> expandFourth(){
        return (t, u, v, w) -> this.validate(t, u, v);
    }
}
