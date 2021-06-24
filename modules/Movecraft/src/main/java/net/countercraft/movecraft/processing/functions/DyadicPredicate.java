package net.countercraft.movecraft.processing.functions;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface public interface DyadicPredicate<T, U> {

    @NotNull Result validate(@NotNull T t, @NotNull U u);

    default DyadicPredicate<T, U> or(DyadicPredicate<T, U> other){
        return (t,u) -> {
            var result = this.validate(t, u);
            if(result.isSucess()){
                return result;
            }
            return other.validate(t,u);
        };
    }

    default DyadicPredicate<T, U> and(DyadicPredicate<T, U> other){
        return (t,u) -> {
            var result = this.validate(t, u);
            if(!result.isSucess()){
                return result;
            }
            return other.validate(t, u);
        };
    }

    default MonadicPredicate<T> fixSecond(U u){
        return (t) -> this.validate(t, u);
    }

    default MonadicPredicate<U> fixFirst(T t){
        return (u) -> this.validate(t, u);
    }

    default <V> TriadicPredicate<V, T, U> expandFirst(){
        return (v, t, u) -> this.validate(t,u);
    }

    default <V> TriadicPredicate<T, V, U> expandSecond(){
        return (t, v, u) -> this.validate(t,u);
    }
    default <V> TriadicPredicate<T, U, V> expandThird(){
        return (t, u, v) -> this.validate(t,u);
    }

}
