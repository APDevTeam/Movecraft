package net.countercraft.movecraft;

import com.google.common.collect.Iterators;
import net.countercraft.movecraft.util.hitboxes.SolidHitBox;
import net.countercraft.movecraft.util.hitboxes.TreeHitBox;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TreeHitBoxTest {
    private TreeHitBox createTestHitbox(){
        TreeHitBox out = new TreeHitBox();
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
        TreeHitBox box = new TreeHitBox();
        assertTrue(box.add(new MovecraftLocation(0, 1, 0)), "first");
        assertFalse(box.add(new MovecraftLocation(0, 1, 0)), "second");
        assertEquals(1, box.size());
    }

    @Test
    public void testContains(){
        TreeHitBox box = new TreeHitBox();
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

    @Test @Ignore
    public void testLargeHitBox(){
        SolidHitBox solid = new SolidHitBox(new MovecraftLocation(-100,-100,-100), new MovecraftLocation(100,100,100));
        TreeHitBox hitBox = new TreeHitBox();
        hitBox.addAll(solid);
        assertEquals(solid.size(), hitBox.size());
        assertEquals(solid.size(), Iterators.size(hitBox.iterator()));
        assertTrue(hitBox.containsAll(solid.asSet()));
    }

    @Test
    public void testCloneBounds(){
        SolidHitBox solid = new SolidHitBox(new MovecraftLocation(-3,-3,-3), new MovecraftLocation(3,3,3));
        TreeHitBox box = new TreeHitBox(solid);
        assertEquals(-3, box.getMinX(), "X");
        assertEquals( -3, box.getMinY(), "Y");
        assertEquals(-3, box.getMinZ(), "Z");
        assertEquals(3, box.getMaxX());
        assertEquals(3, box.getMaxY());
        assertEquals( 3, box.getMaxZ());
    }
}
