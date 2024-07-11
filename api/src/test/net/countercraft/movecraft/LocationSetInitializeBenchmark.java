package net.countercraft.movecraft;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.countercraft.movecraft.util.collections.LocationSet;
import net.countercraft.movecraft.util.collections.LocationTrieSet;
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;
import net.countercraft.movecraft.util.hitboxes.SetHitBox;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("deprecation")
@Threads(4)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 2, time = 1)
public class LocationSetInitializeBenchmark {
    @State(Scope.Thread)
    public static class LocationState {
        private ArrayList<MovecraftLocation> createTestLocations(){
            ArrayList<MovecraftLocation> out = new ArrayList<>();
            for(int i = 0; i < 50; i++){
                for(int j = 0; j< 50; j++){
                    for(int k = 0; k<50; k++){
                        out.add(new MovecraftLocation(i,j,k));
                    }
                }
            }
            return out;
        }
        public List<MovecraftLocation> locations = createTestLocations();
    }

    @Benchmark @BenchmarkMode(Mode.Throughput)
    public Set<MovecraftLocation> locationTrieSet(LocationState state){
        return new LocationTrieSet(state.locations);
    }
    @Benchmark @BenchmarkMode(Mode.Throughput)
    public Set<MovecraftLocation> locationSet(LocationState state){
        return new LocationSet(state.locations);
    }

    @Benchmark @BenchmarkMode(Mode.Throughput)
    public Set<MovecraftLocation> hashSet(LocationState state){
        return new HashSet<>(state.locations);
    }

    @Benchmark @BenchmarkMode(Mode.Throughput)
    public BitmapHitBox bitmapHitBox(LocationState state){
        return new BitmapHitBox(state.locations);
    }

    @Benchmark @BenchmarkMode(Mode.Throughput)
    public SetHitBox treeHitBox(LocationState state){
        return new SetHitBox(state.locations);
    }

    @Benchmark @BenchmarkMode(Mode.Throughput)
    public LongOpenHashSet longOpenHashSet(LocationState state){
        var out = new LongOpenHashSet();
        for(var location : state.locations){
            out.add(location.pack());
        }
        return out;
    }

}
