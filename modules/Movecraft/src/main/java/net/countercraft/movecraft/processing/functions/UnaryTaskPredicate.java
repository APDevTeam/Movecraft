package net.countercraft.movecraft.processing.functions;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface public interface UnaryTaskPredicate<T> {

    Result validate(@NotNull T t);

    default UnaryTaskPredicate<T> or(UnaryTaskPredicate<T> other){
        return t -> {
            var result = this.validate(t);
            if(result.isSucess()){
                return result;
            }
            return other.validate(t);
        };
    }

    default UnaryTaskPredicate<T> and(UnaryTaskPredicate<T> other){
        return t -> {
            var result = this.validate(t);
            if(!result.isSucess()){
                return result;
            }
            return other.validate(t);
        };
    }

    default <U> BiTaskPredicate<U, T> expandFirst(){
        return (u,t) -> this.validate(t);
    }
    default <U> BiTaskPredicate<T, U> expandSecond(){
        return (t, u) -> this.validate(t);
    }

}
