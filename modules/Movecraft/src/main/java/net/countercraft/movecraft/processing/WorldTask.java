package net.countercraft.movecraft.processing;

@Deprecated
public abstract class WorldTask implements Runnable{

    public WorldTask(){    }

    @Override
    public final void run(){
        compute();
    }

    public abstract void compute();

}
