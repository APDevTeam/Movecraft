package net.countercraft.movecraft;

import com.google.common.collect.Iterators;
import net.countercraft.movecraft.util.hitboxes.SolidHitBox;
import net.countercraft.movecraft.util.hitboxes.SetHitBox;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TreeHitBoxTest {
    private SetHitBox createTestHitbox(){
        SetHitBox out = new SetHitBox();
        for(int i = -3; i < 3; i++){
            for(int j = -3; j< 3; j++){
                for(int k = -3; k<3; k++){
                    out.add(new MovecraftLocation(i,j,k));
                }
            }
        }
        return out;
    }

    @Test
    public void testRemove() {
        SetHitBox hitBox = createTestHitbox();
        SetHitBox other = createTestHitbox();
        for(var location : hitBox){
            other.remove(location);
        }
        assertTrue(other.isEmpty());
    }

    @Test
    public void testRemoveThanIterate(){
        SetHitBox hitBox = createTestHitbox();
        SetHitBox other = createTestHitbox();
        int size = other.size();
        for(var location : hitBox){
            other.remove(location);
            size -= 1;
            assertEquals(size, Iterators.size(other.iterator()));
        }
        assertTrue(other.isEmpty());
    }

    @Test
    public void testIteratorBitPosition(){
        long l = 1;
        for(int i = 0; i < 64; i++){
            long shifted = l << i;
            MovecraftLocation unpacked = MovecraftLocation.unpack(shifted);
            SetHitBox box = new SetHitBox();
            box.add(unpacked);
            for(var iterLoc : box){
                assertEquals(unpacked, iterLoc);
            }
        }
    }

    @Test
    public void testSinglePointIterator(){
        for(var location : new SolidHitBox(new MovecraftLocation(-3,-3,-3), new MovecraftLocation(3,3,3))){
            SetHitBox box = new SetHitBox();
            box.add(location);
            assertEquals(location, box.iterator().next());
        }
    }

    @Test
    public void testIncrementalIterator(){
        Set<MovecraftLocation> visited = new HashSet<>();
        SetHitBox box = new SetHitBox();
        for(var location : new SolidHitBox(new MovecraftLocation(-3,-3,-3), new MovecraftLocation(3,3,3))){
            box.add(location);
            visited.add(location);
            Set<MovecraftLocation> verified = new HashSet<>();
            for (var probe : box){
                assertTrue(visited.contains(probe), String.format("Location %s is not contained in %s, however %s are", probe, visited, verified));
                verified.add(probe);
            }
        }
    }

    @Test
    public void testMin(){
        assertEquals(-3, createTestHitbox().getMinX(), "X");
        assertEquals( -3, createTestHitbox().getMinY(), "Y");
        assertEquals(-3, createTestHitbox().getMinZ(), "Z");
    }

    @Test
    public void testMax(){
        assertEquals(2, createTestHitbox().getMaxX());
        assertEquals(2, createTestHitbox().getMaxY());
        assertEquals( 2, createTestHitbox().getMaxZ());
    }

    @Test
    public void testDoubleAdd(){
        SetHitBox box = new SetHitBox();
        assertTrue(box.add(new MovecraftLocation(0, 1, 0)), "first");
        assertFalse(box.add(new MovecraftLocation(0, 1, 0)), "second");
        assertEquals(1, box.size());
    }

    @Test
    public void testContains(){
        SetHitBox box = new SetHitBox();
        box.add(new MovecraftLocation(0, 1, 0));
        assertTrue(box.contains(new MovecraftLocation(0,1,0)));
    }

    @Test
    public void testIterator(){
        int i = 0;
        for(var location : createTestHitbox()){
            i++;
            assertTrue(location.getX() >= -3);
            assertTrue(location.getY() >= -3);
            assertTrue(location.getZ() >= -3);
            assertTrue(location.getX() < 3);
            assertTrue(location.getY() < 3);
            assertTrue(location.getZ() < 3);
        }
        assertEquals(216, i);
    }

    @Test
    public void testContainsSelf(){
        var hitbox = createTestHitbox();
        for(var location : hitbox){
            assertTrue(hitbox.contains(location), String.format("HitBox should contain location %s", location));
        }
    }

    @Test  @Ignore
    public void testLargeHitBox(){
        SolidHitBox solid = new SolidHitBox(new MovecraftLocation(-100,-100,-100), new MovecraftLocation(100,100,100));
        SetHitBox hitBox = new SetHitBox();
        hitBox.addAll(solid);
        assertEquals(solid.size(), hitBox.size());
        assertEquals(solid.size(), Iterators.size(hitBox.iterator()));
        assertTrue(hitBox.containsAll(solid.asSet()));
    }

    @Test
    public void testCloneBounds(){
        SolidHitBox solid = new SolidHitBox(new MovecraftLocation(-3,-3,-3), new MovecraftLocation(3,3,3));
        SetHitBox box = new SetHitBox(solid);
        assertEquals(-3, box.getMinX(), "X");
        assertEquals( -3, box.getMinY(), "Y");
        assertEquals(-3, box.getMinZ(), "Z");
        assertEquals(3, box.getMaxX());
        assertEquals(3, box.getMaxY());
        assertEquals( 3, box.getMaxZ());
    }
}
