package net.countercraft.movecraft;

import net.countercraft.movecraft.utils.HitBox;
import net.countercraft.movecraft.utils.SolidHitBox;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class SolidHitBoxTest {

    final private MovecraftLocation minimum = new MovecraftLocation(0,0,0);
    final private MovecraftLocation maximum = new MovecraftLocation(3,3,3);
    final private HitBox hitBox = new SolidHitBox(minimum, maximum);

    @Test
    public void testBounds(){
        assertEquals(minimum.getX(), hitBox.getMinX());
        assertEquals(minimum.getY(), hitBox.getMinY());
        assertEquals(minimum.getZ(), hitBox.getMinZ());
        assertEquals(maximum.getX(), hitBox.getMaxX());
        assertEquals(maximum.getY(), hitBox.getMaxY());
        assertEquals(maximum.getZ(), hitBox.getMaxZ());
    }

    @Test
    public void testSize(){
        assertEquals(64, hitBox.size());
        assertThat(hitBox, iterableWithSize(64));
    }


    @Test
    public void testContents(){
        final List<MovecraftLocation> locations = new ArrayList<>(64);
        for(int x = minimum.getX(); x <= maximum.getX(); x++){
            for(int y = minimum.getY(); y <= maximum.getY(); y++){
                for(int z = minimum.getX(); z <= maximum.getY(); z++){
                    locations.add(new MovecraftLocation(x,y,z));
                }
            }
        }
        assertThat(locations,containsInAnyOrder(hitBox.asSet().toArray()));
    }

    @Test
    public void testBoundingHitBox(){
        assertIterableEquals(hitBox, hitBox.boundingHitBox());
    }

    @Test
    public void testContainsAll(){
        final List<MovecraftLocation> locations = new ArrayList<>(64);
        for(int x = minimum.getX(); x <= maximum.getX(); x++){
            for(int y = minimum.getY(); y <= maximum.getY(); y++){
                for(int z = minimum.getX(); z <= maximum.getY(); z++){
                    locations.add(new MovecraftLocation(x,y,z));
                }
            }
        }
        assertTrue(hitBox.containsAll(locations));
    }



}
