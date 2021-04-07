package net.countercraft.movecraft.util;

import java.util.DoubleSummaryStatistics;
import java.util.function.DoubleConsumer;

public class TimingData implements DoubleConsumer {
    private int count = 0;
    private int endIndex = 0;
    private final double[] data;
    private final DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
    public TimingData(){
        this(5);
    }

    private TimingData(int size){
        data = new double[size];
    }

    @Override
    public void accept(double value) {
        count++;
        data[endIndex] = value;
        endIndex = (endIndex + 1) % data.length;
        stats.accept(value);
    }

    /**
     * Gets the average across the most recent values defined by size (default 64)
     * @return an average of the most recently supplied values
     */
    public double getRecentAverage(){
        if(this.count == 0){
            return 0;
        }
        int bound = Math.min(data.length, count);
        int out = 0;
        for(int i = 0; i < bound; i++){
            out += data[i];
        }
        return out/(double) bound;
    }

    public double getAverage(){
        return stats.getAverage();
    }

    public int getCount(){
        return count;
    }
}
