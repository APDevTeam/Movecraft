package net.countercraft.movecraft.processing.functions;

import org.jetbrains.annotations.NotNull;

public final class Result {
    private static final Result SUCCESS = new Result(true);
    private static final Result FAILURE = new Result(false);

    @NotNull
    public static Result of(boolean success) {
        return success ? SUCCESS : FAILURE;
    }

    @NotNull
    public static Result succeed() {
        return SUCCESS;
    }

    @NotNull
    public static Result succeedWithMessage(@NotNull String message) {
        return new Result(true, message);
    }

    @NotNull
    public static Result fail() {
        return FAILURE;
    }

    @NotNull
    public static Result failWithMessage(@NotNull String message) {
        return new Result(false, message);
    }

    private final boolean success;
    @NotNull
    private final String message;


    private Result(boolean success) {
        this.success = success;
        message = "No result message provided! This is a bug and should be reported.";
    }

    private Result(boolean success, @NotNull String message) {
        this.success = success;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSucess() {
        return success;
    }
}
