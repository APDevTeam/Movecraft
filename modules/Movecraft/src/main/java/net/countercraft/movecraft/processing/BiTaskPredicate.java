package net.countercraft.movecraft.processing;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface public interface BiTaskPredicate<T, U> {

    Result validate(@NotNull T t, @NotNull U u);

    default BiTaskPredicate<T, U> or(BiTaskPredicate<T, U> other){
        return (t,u) -> {
            var result = this.validate(t, u);
            if(result.isSucess()){
                return result;
            }
            return other.validate(t,u);
        };
    }

    default BiTaskPredicate<T, U> and(BiTaskPredicate<T, U> other){
        return (t,u) -> {
            var result = this.validate(t, u);
            if(!result.isSucess()){
                return result;
            }
            return other.validate(t, u);
        };
    }

    default UnaryTaskPredicate<T> fixSecond(U u){
        return (t) -> this.validate(t, u);
    }

    default UnaryTaskPredicate<U> fixFirst(T t){
        return (u) -> this.validate(t, u);
    }

}
