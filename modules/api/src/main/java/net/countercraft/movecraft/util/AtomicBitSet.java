package net.countercraft.movecraft.util;


import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;

public class AtomicBitSet {
    private static final VarHandle BACKING = MethodHandles.arrayElementVarHandle(Cell[].class);
    private static final VarHandle VALUE;

    static {
        try {
            VALUE = MethodHandles.lookup().findVarHandle(Cell.class, "value", long.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static final int FIELD_LENGTH = 64;
    private final Cell[] backing;

    public AtomicBitSet(int maxSize){
        this.backing = new Cell[(int) Math.ceil(maxSize/(double)FIELD_LENGTH)];
    }

    /**
     * Sets the bit at the specified value to true, returning the previous value.
     * @param index index the bit index
     * @return the previous value of the bit with the specified index
     */
    public boolean add(int index){
        while(true){
            var cell = getCell(index);
            long bitField = (long) VALUE.getAcquire(cell);
            if(VALUE.weakCompareAndSetRelease(cell, bitField,
                    bitField | (1L << (index & (FIELD_LENGTH - 1))))) {
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
        // Use acquire to ensure a happens before relation in external contexts
        return ((((long) VALUE.getAcquire(getCell(index))) >>> (index & (FIELD_LENGTH - 1))) & 1L) == 1;
    }

    private Cell getCell(int index){
        Cell cellField = (Cell) BACKING.getAcquire(backing, index/FIELD_LENGTH);
        if (cellField != null) {
            return cellField;
        }
        var initialized = new Cell();
        cellField = (Cell) BACKING.compareAndExchangeRelease(backing, index/FIELD_LENGTH, null, initialized);
        if(cellField == null){
            return initialized;
        }
        return cellField;
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

    private static class Cell{
        @jdk.internal.vm.annotation.Contended long value;
    }
}
