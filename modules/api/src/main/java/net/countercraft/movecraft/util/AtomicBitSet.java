package net.countercraft.movecraft.util;


import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;

public class AtomicBitSet {
    private static final VarHandle BACKING = MethodHandles.arrayElementVarHandle(long[].class);
    private static final int FIELD_LENGTH = 64;
    private final long[] backing;

    public AtomicBitSet(int maxSize){
        this.backing = new long[(int) Math.ceil(maxSize/(double)FIELD_LENGTH)];
    }

    /**
     * Sets the bit at the specified value to true, returning the previous value.
     * @param index index the bit index
     * @return the previous value of the bit with the specified index
     */
    public boolean add(int index){
        long prior = (long) BACKING.getAndBitwiseOr(backing, index/FIELD_LENGTH, (1L << (index & (FIELD_LENGTH - 1))));
        return ((prior >>> (index & (FIELD_LENGTH - 1))) & 1L) == 1;
    }

    /**
     * Returns the value of the bit at the specified index. The value is false if unset.
     * @param index the bit index
     * @return the value of the bit with the specified index
     */
    public boolean get(int index){
        // Use acquire to ensure a happens before relation in external contexts
        return (((long) BACKING.getAcquire(backing, index/FIELD_LENGTH) >>> (index & (FIELD_LENGTH - 1))) & 1L) == 1;
    }

    @Override
    public String toString(){
        List<String> values = new ArrayList<>();
        for(int i = 0; i<backing.length; i++){
            for(int j = 0; j < FIELD_LENGTH; j++){
                if(this.get(i * FIELD_LENGTH + j)){
                    values.add("" + j + i * FIELD_LENGTH);
                }
            }
        }
        return "AtomicBitSet{" + String.join(",", values) + "}";
    }
}
