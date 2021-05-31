package net.countercraft.movecraft.processing;

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

}
