package net.countercraft.movecraft;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.countercraft.movecraft.utils.HitBox;
import net.countercraft.movecraft.utils.SolidHitBox;
import org.junit.Test;


public class HitBoxTest {

    @Test
    public void testSolidHitBox(){
        final MovecraftLocation minimum = new MovecraftLocation(0,0,0);
        final MovecraftLocation maximum = new MovecraftLocation(3,3,3);
        final HitBox hitBox = new SolidHitBox(minimum, maximum);
        assertEquals(hitBox.size(),64);
    }

}
