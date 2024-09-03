package net.countercraft.movecraft.lifecycle;

import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ServiceHost {
    private final @NotNull List<HostedService> hostedServices;

    @Inject
    public ServiceHost(@NotNull List<HostedService> hostedServices) {
        this.hostedServices = hostedServices;
    }

    public void startAll(){
        hostedServices.forEach(HostedService::start);
    }

    public void stopAll(){
        hostedServices.forEach(HostedService::stop);
    }
}
