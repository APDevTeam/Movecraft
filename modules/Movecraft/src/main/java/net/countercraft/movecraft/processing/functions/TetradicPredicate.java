package net.countercraft.movecraft.processing.functions;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface public interface TetradicPredicate<T, U, V, W> {
    @NotNull Result validate(@NotNull T t, @NotNull U u, @NotNull V v, @NotNull W w);

    default TetradicPredicate<T, U, V, W> or(TetradicPredicate<T, U, V, W> other){
        return (t,u,v,w) -> {
            var result = this.validate(t,u,v,w);
            if(result.isSucess()){
                return result;
            }
            return other.validate(t,u,v,w);
        };
    }

    default TetradicPredicate<T, U, V, W> and(TetradicPredicate<T, U, V, W> other){
        return (t,u,v,w) -> {
            var result = this.validate(t,u,v,w);
            if(!result.isSucess()){
                return result;
            }
            return other.validate(t,u,v,w);
        };
    }

    default TriadicPredicate<U, V, W> fixFirst(T t){
        return (u, v, w) -> this.validate(t, u, v, w);
    }

    default TriadicPredicate<T, V, W> fixSecond(U u){
        return (t, v, w) -> this.validate(t, u, v, w);
    }

    default TriadicPredicate<T, U, W> fixThird(V v){
        return (t, u, w) -> this.validate(t, u, v, w);
    }

    default TriadicPredicate<T, U, V> fixFourth(W w){
        return (t, u, v) -> this.validate(t, u, v, w);
    }
}
