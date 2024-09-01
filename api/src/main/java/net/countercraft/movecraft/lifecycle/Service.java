package net.countercraft.movecraft.lifecycle;

public interface Service {
    default void start(){}
    default void stop(){}
}
