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
}
