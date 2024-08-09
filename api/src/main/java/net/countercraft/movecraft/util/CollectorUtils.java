package net.countercraft.movecraft.util;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.util.hitboxes.BitmapHitBox;

import java.util.stream.Collector;

public class CollectorUtils {
    public static Collector<MovecraftLocation, ?, BitmapHitBox> toHitBox(){
        return Collector.of(BitmapHitBox::new, BitmapHitBox::add, BitmapHitBox::union, t->t);
    }
}
