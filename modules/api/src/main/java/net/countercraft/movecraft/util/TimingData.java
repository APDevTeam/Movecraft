package net.countercraft.movecraft.util;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.function.DoubleConsumer;

public class TimingData implements DoubleConsumer {
    private int count = 0;
    private int endIndex = 0;
    private final double[] data;
    private final DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
    public TimingData(){
        this(20);
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
     * averages the 20 most recent values, dropping the maximum 2 if at capacity
     * @return an average of the most recently supplied values
     */
    public double getRecentAverage(){
        // We use exact floating point comparisons because we are specifically ignoring the initial 0 values of our array
        return Arrays.stream(data).sorted().filter((d) -> d != 0.0).limit(data.length-2).average().orElse(0);
    }

    public double getAverage(){
        return stats.getAverage();
    }

    public int getCount(){
        return count;
    }
}
