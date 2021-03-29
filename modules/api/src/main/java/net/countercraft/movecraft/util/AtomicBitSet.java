package net.countercraft.movecraft.util;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLongArray;

public class AtomicBitSet {
    private static final int FIELD_LENGTH = 64;
    private final AtomicLongArray backing;

    public AtomicBitSet(int maxSize){
        this.backing = new AtomicLongArray((int) Math.ceil(maxSize/(double)FIELD_LENGTH));
    }

    /**
     * Sets the bit at the specified value to true, returning the previous value.
     * @param index index the bit index
     * @return the previous value of the bit with the specified index
     */
    public boolean add(int index){
        while(true){
            long bitField = backing.getAcquire(index/FIELD_LENGTH);
            if(backing.weakCompareAndSetRelease(index/FIELD_LENGTH, bitField,
                    bitField | (1L << (index & (FIELD_LENGTH - 1))))){
                return ((bitField >>> (index & (FIELD_LENGTH - 1))) & 1L) == 1;
            }
        }
    }

    /**
     * Returns the value of the bit at the specified index. The value is false if unset.
     * @param index the bit index
     * @return the value of the bit with the specified index
     */
    public boolean get(int index){
        return ((backing.get(index/FIELD_LENGTH) >>> (index & (FIELD_LENGTH - 1))) & 1L) == 1;
    }

    @Override
    public String toString(){

        List<String> values = new ArrayList<>();
        for(int i = 0; i<backing.length(); i++){
            for(long j = backing.get(i), k = 0; j != 0; j >>>= 1, k++){
                if((j & 1) != 0){
                    values.add("" + k + i* 64L);
                }
            }
        }
        return "AtomicBitSet{" + String.join(",", values) + "}";
    }
}
