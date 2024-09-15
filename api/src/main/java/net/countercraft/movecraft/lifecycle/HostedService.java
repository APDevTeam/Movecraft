package net.countercraft.movecraft.lifecycle;

public interface HostedService {
    default void start(){}
    default void stop(){}
}
