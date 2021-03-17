package net.countercraft.movecraft.processing;

public abstract class WorldTask implements Runnable{

    protected final MovecraftWorld world;

    public WorldTask(MovecraftWorld world){

        this.world = world;
    }

    @Override
    public final void run(){
        compute(world);
        WorldManager.INSTANCE.poison();
    }

    public abstract void compute(MovecraftWorld world);

}
