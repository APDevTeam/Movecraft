package net.countercraft.movecraft.processing.functions;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface public interface TetradicPredicate<T, U, V, W> {
    @Contract(pure = true) @NotNull Result validate(@NotNull T t, @NotNull U u, @NotNull V v, @NotNull W w);

    @Contract(pure = true)
    default TetradicPredicate<T, U, V, W> or(@NotNull TetradicPredicate<T, U, V, W> other){
        return (t,u,v,w) -> {
            var result = this.validate(t,u,v,w);
            if(result.isSucess()){
                return result;
            }
            return other.validate(t,u,v,w);
        };
    }

    @Contract(pure = true)
    default @NotNull TetradicPredicate<T, U, V, W> and(@NotNull TetradicPredicate<T, U, V, W> other){
        return (t,u,v,w) -> {
            var result = this.validate(t,u,v,w);
            if(!result.isSucess()){
                return result;
            }
            return other.validate(t,u,v,w);
        };
    }

    @Contract(pure = true)
    default @NotNull TriadicPredicate<U, V, W> fixFirst(T t){
        return (u, v, w) -> this.validate(t, u, v, w);
    }

    @Contract(pure = true)
    default @NotNull TriadicPredicate<T, V, W> fixSecond(U u){
        return (t, v, w) -> this.validate(t, u, v, w);
    }

    @Contract(pure = true)
    default @NotNull TriadicPredicate<T, U, W> fixThird(V v){
        return (t, u, w) -> this.validate(t, u, v, w);
    }

    @Contract(pure = true)
    default @NotNull TriadicPredicate<T, U, V> fixFourth(W w){
        return (t, u, v) -> this.validate(t, u, v, w);
    }
}
