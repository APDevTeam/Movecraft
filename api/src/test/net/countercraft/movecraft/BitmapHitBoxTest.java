package net.countercraft.movecraft;

import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BitmapHitBoxTest {
    private BitmapHitBox createTestHitbox(){
        BitmapHitBox out = new BitmapHitBox();
        for(int i = 0; i < 3; i++){
            for(int j = 0; j< 3; j++){
                for(int k = 0; k<3; k++){
                    out.add(new MovecraftLocation(i,j,k));
                }
            }
        }
        return out;
    }

    @Test
    public void testMin(){
        assertEquals(0, createTestHitbox().getMinX(), "X");
        assertEquals( 0, createTestHitbox().getMinY(), "Y");
        assertEquals(0, createTestHitbox().getMinZ(), "Z");
    }

    @Test
    public void testMax(){
        assertEquals(createTestHitbox().getMaxX(), 2);
        assertEquals(createTestHitbox().getMaxY(), 2);
        assertEquals( 2, createTestHitbox().getMaxZ());
    }

    @Test
    public void testLocalExtrema(){
        assertEquals(0,createTestHitbox().getMinYAt(0,0));
//        assertEquals(createTestHitbox().getLocalMaxY(0,0), 2);
    }

}
