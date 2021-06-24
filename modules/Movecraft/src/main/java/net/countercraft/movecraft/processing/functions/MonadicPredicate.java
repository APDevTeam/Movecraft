package net.countercraft.movecraft.processing.functions;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface public interface MonadicPredicate<T> {

    @NotNull Result validate(@NotNull T t);

    default MonadicPredicate<T> or(MonadicPredicate<T> other){
        return t -> {
            var result = this.validate(t);
            if(result.isSucess()){
                return result;
            }
            return other.validate(t);
        };
    }

    default MonadicPredicate<T> and(MonadicPredicate<T> other){
        return t -> {
            var result = this.validate(t);
            if(!result.isSucess()){
                return result;
            }
            return other.validate(t);
        };
    }

    default <U> DyadicPredicate<U, T> expandFirst(){
        return (u,t) -> this.validate(t);
    }
    default <U> DyadicPredicate<T, U> expandSecond(){
        return (t, u) -> this.validate(t);
    }

}
