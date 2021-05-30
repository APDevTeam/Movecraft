package net.countercraft.movecraft.util;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class CompletableFutureTask<T> extends CompletableFuture<T> implements Runnable  {

    private final Supplier<T> function;

    public CompletableFutureTask(Supplier<T> function){
        this.function = function;
    }

    @Override
    public void run(){
        this.complete(function.get());
    }


}
