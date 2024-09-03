package net.countercraft.movecraft.lifecycle;

import org.bukkit.plugin.Plugin;
import org.int4.dirk.api.Injector;
import org.int4.dirk.di.Injectors;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.logging.Logger;

public final class PluginBuilder {
    private final Injector injector;

    private PluginBuilder() {
        injector = Injectors.manual();
    }

    @Contract("_->new")
    public static @NotNull PluginBuilder createFor(@NotNull Plugin plugin){
        var builder = new PluginBuilder();
        builder.injector.registerInstance(plugin.getLogger());
        builder.injector.registerInstance(plugin);

        return builder;
    }

    @Contract("->new")
    public static @NotNull PluginBuilder createForTest(){
        var builder = new PluginBuilder();
        builder.injector.registerInstance(Logger.getLogger("movecraft-unit-test"));

        return builder;
    }

    @SuppressWarnings("UnusedReturnValue")
    @Contract("_->this")
    public @NotNull PluginBuilder register(Type type){
        injector.register(type);

        return this;
    }

    @Contract("_->this")
    public @NotNull PluginBuilder register(Collection<Type> types){
        injector.register(types);

        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    @Contract("_,_->this")
    public @NotNull PluginBuilder registerInstance(Object instance, Annotation... qualifiers){
        injector.registerInstance(instance, qualifiers);

        return this;
    }

    @Contract("->new")
    public @NotNull Application build(){
        // Lifecycle management
        injector.register(WorkerHost.class);
        injector.register(ListenerHostedService.class);

        return new Application(injector.getInstance(ServiceHost.class), new ServiceProvider(injector));
    }

    public record Application(ServiceHost host, ServiceProvider container){}

    public static class ServiceProvider {
        private final @NotNull Injector injector;

        private ServiceProvider(@NotNull Injector injector) {
            this.injector = injector;
        }

        public <T> T getService(@NotNull Class<T> cls){
            return injector.getInstance(cls);
        }
    }
}
