package net.countercraft.movecraft;

import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MovecraftLocationTest {

    @Test
    public void testPacking(){
        for(int x = -255; x < 255; x++){
            for(int y = 0; y < 255; y++){
                for(int z = -255; z < 255; z++){
                    MovecraftLocation test = new MovecraftLocation(x,y,z);
                    assertEquals(test, MovecraftLocation.unpack(test.pack()));
                }
            }
        }
    }

    @Test
    public void testSpa(){
        MovecraftLocation location = new MovecraftLocation(1137, 96, -3068);
        assertEquals(MovecraftLocation.unpack(location.pack()), location);

    }
}
