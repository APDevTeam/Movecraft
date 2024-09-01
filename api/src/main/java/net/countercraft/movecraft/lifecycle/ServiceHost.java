package net.countercraft.movecraft.lifecycle;

import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ServiceHost {
    private final @NotNull List<Service> services;

    @Inject
    public ServiceHost(@NotNull List<Service> services) {
        this.services = services;
    }

    public void startAll(){
        services.forEach(Service::start);
    }

    public void stopAll(){
        services.forEach(Service::stop);
    }
}
