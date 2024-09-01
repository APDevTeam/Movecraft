package net.countercraft.movecraft.lifecycle;

public interface Worker {
    default int getDelay(){
        return 1;
    }

    boolean isAsync();

    int getPeriod();

    void run();
}
