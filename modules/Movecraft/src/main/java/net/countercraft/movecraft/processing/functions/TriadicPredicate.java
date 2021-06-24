package net.countercraft.movecraft.processing.functions;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface public interface TriadicPredicate<T, U, V> {

    Result validate(@NotNull T t, @NotNull U u, @NotNull V v);

    default TriadicPredicate<T, U, V> or(TriadicPredicate<T, U, V> other){
        return (t, u, v) -> {
            var result = this.validate(t, u, v);
            if(result.isSucess()){
                return result;
            }
            return other.validate(t, u, v);
        };
    }

    default TriadicPredicate<T, U, V> and(TriadicPredicate<T, U, V> other){
        return (t, u, v) -> {
            var result = this.validate(t, u, v);
            if(!result.isSucess()){
                return result;
            }
            return other.validate(t, u, v);
        };
    }

    default DyadicPredicate<U, V> fixFirst(T t){
        return (u, v) -> this.validate(t, u, v);
    }

    default DyadicPredicate<T, V> fixSecond(U u){
        return (t,v) -> this.validate(t, u, v);
    }

    default DyadicPredicate<T, U> fixThird(V v){
        return (t,u) -> this.validate(t, u, v);
    }

    default <W> TetradicPredicate<W, T, U, V> expandFirst(){
        return (w, t, u, v) -> this.validate(t,u,v);
    }

    default <W> TetradicPredicate<T, W, U, V> expandSecond(){
        return (t, w, u, v) -> this.validate(t, u, v);
    }
    default <W> TetradicPredicate<T, U, W, V> expandThird(){
        return (t, u, w, v) -> this.validate(t, u, v);
    }

    default <W> TetradicPredicate<T, U, V, W> expandFourth(){
        return (t, u, v, w) -> this.validate(t, u, v);
    }
}
