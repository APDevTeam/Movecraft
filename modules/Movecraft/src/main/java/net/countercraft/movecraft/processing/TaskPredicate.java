package net.countercraft.movecraft.processing;

import net.countercraft.movecraft.craft.CraftType;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a data carrying predicate, which can return a message depending on the success state of its evaluation
 * @param <T>
 */
@FunctionalInterface public interface TaskPredicate<T> {
    Result validate(@NotNull T t, @NotNull CraftType type, @NotNull MovecraftWorld world, @Nullable CommandSender player);

    default TaskPredicate<T> or(TaskPredicate<T> other){
        return (t, type, world, player) -> {
            var result = this.validate(t, type, world, player);
            if(result.isSucess()){
                return result;
            }
            return other.validate(t,type,world,player);
        };
    }

    default TaskPredicate<T> and(TaskPredicate<T> other){
        return (t, type, world, player) -> {
            var result = this.validate(t, type, world, player);
            if(!result.isSucess()){
                return result;
            }
            return other.validate(t,type,world,player);
        };
    }

    final class Result{
        private static final Result SUCCESS = new Result(true);
        private static final Result FAILURE = new Result(false);

        @NotNull
        public static Result of(boolean success){
            return success ? SUCCESS : FAILURE;
        }

        @NotNull
        public static Result succeed(){
            return SUCCESS;
        }

        @NotNull
        public static Result succeedWithMessage(@NotNull String message){
            return new Result(true, message);
        }

        @NotNull
        public static Result fail(){
            return FAILURE;
        }

        @NotNull
        public static Result failWithMessage(@NotNull String message){
            return new Result(false, message);
        }

        private final boolean success;
        @NotNull private final String message;


        private Result(boolean success){
            this.success = success;
            message = "No result message provided! This is a bug and should be reported.";
        }

        private Result(boolean success, @NotNull String message) {
            this.success = success;
            this.message = message;
        }

        public String getMessage(){
            return message;
        }

        public boolean isSucess(){
            return success;
        }
    }
}
