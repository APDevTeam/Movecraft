package net.countercraft.movecraft;

import net.countercraft.movecraft.utils.HashHitBox;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HashHitBoxTest {
    private HashHitBox createTestHitbox(){
        HashHitBox out = new HashHitBox();
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
        assertEquals(createTestHitbox().getMinX(), 0);
        assertEquals(createTestHitbox().getMinY(), 0);
        assertEquals(createTestHitbox().getMinZ(), 0);
    }

    @Test
    public void testMaX(){
        assertEquals(createTestHitbox().getMaxX(), 2);
        assertEquals(createTestHitbox().getMaxY(), 2);
        assertEquals(createTestHitbox().getMaxZ(), 2);
    }

    @Test
    public void testLocalExtrema(){
        assertEquals(createTestHitbox().getLocalMinY(0,0), 0);
        assertEquals(createTestHitbox().getLocalMaxY(0,0), 2);
    }

}
