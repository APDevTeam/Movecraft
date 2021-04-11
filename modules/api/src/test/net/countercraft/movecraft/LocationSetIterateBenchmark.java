package net.countercraft.movecraft;

import net.countercraft.movecraft.util.collections.LocationSet;
import net.countercraft.movecraft.util.collections.LocationTrieSet;
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Threads(Threads.MAX)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
public class LocationSetIterateBenchmark {
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
        public Iterable<MovecraftLocation> bitMapHitBox = new BitmapHitBox(locations);
        public Iterable<MovecraftLocation> locationTrieSet = new LocationTrieSet(locations);
        public Iterable<MovecraftLocation> locationSet = new LocationSet(locations);
        public Iterable<MovecraftLocation> hashSet = new HashSet<>(locations);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void locationTrieSet(LocationState state, Blackhole blackhole){
        for(MovecraftLocation location : state.locationTrieSet){
            blackhole.consume(location);
        }
    }
    @Benchmark @BenchmarkMode(Mode.Throughput)
    public void locationSet(LocationState state, Blackhole blackhole){
        for(MovecraftLocation location : state.locationSet){
            blackhole.consume(location);
        }
    }

    @Benchmark @BenchmarkMode(Mode.Throughput)
    public void hashSet(LocationState state, Blackhole blackhole){
        for(MovecraftLocation location : state.hashSet){
            blackhole.consume(location);
        }
    }

    @Benchmark @BenchmarkMode(Mode.Throughput)
    public void bitmapHitBox(LocationState state, Blackhole blackhole){
        for(MovecraftLocation location : state.bitMapHitBox){
            blackhole.consume(location);
        }
    }

}
