package net.countercraft.movecraft;

import org.junit.Ignore;
import org.junit.Test;
import org.openjdk.jmh.Main;

import java.io.IOException;
import java.util.logging.Logger;

public class TestBenchmark {
    @Test @Ignore
    public void benchmark(){
        try {
            String[] args = {};
            Logger.getAnonymousLogger().info("Testing output");
            Main.main(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
