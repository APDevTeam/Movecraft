package net.countercraft.movecraft;

import net.countercraft.movecraft.util.AtomicLocationSet;
import net.countercraft.movecraft.util.hitboxes.SolidHitBox;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AtomicLocationSetTest {

    private AtomicLocationSet createTestSet(){
        var hitBox = new SolidHitBox(new MovecraftLocation(-3,-3,-3), new MovecraftLocation(3,3,3));
        var locations = new AtomicLocationSet();
        for(var location : hitBox){
            locations.add(location);
        }
        return locations;
    }

    @Test
    public void testContainsSelf(){
        var locations = createTestSet();
        for(var location : new SolidHitBox(new MovecraftLocation(-3,-3,-3), new MovecraftLocation(3,3,3))){
            assertTrue(locations.contains(location), String.format("AtomicLocationSet should contain location %s", location));
        }
    }
}
