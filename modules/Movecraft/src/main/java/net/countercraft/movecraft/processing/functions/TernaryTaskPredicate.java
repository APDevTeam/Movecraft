package net.countercraft.movecraft.processing.functions;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface public interface TernaryTaskPredicate<T, U, V> {

    Result validate(@NotNull T t, @NotNull U u, @NotNull V v);

    default TernaryTaskPredicate<T, U, V> or(TernaryTaskPredicate<T, U, V> other){
        return (t, u, v) -> {
            var result = this.validate(t, u, v);
            if(result.isSucess()){
                return result;
            }
            return other.validate(t, u, v);
        };
    }

    default TernaryTaskPredicate<T, U, V> and(TernaryTaskPredicate<T, U, V> other){
        return (t, u, v) -> {
            var result = this.validate(t, u, v);
            if(!result.isSucess()){
                return result;
            }
            return other.validate(t, u, v);
        };
    }
}
