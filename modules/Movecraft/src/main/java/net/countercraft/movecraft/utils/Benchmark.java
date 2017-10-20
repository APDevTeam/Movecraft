package net.countercraft.movecraft.utils;

//import com.sun.servicetag.SystemEnvironment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class Benchmarker {
	public HashMap<String, Long> breaks = new HashMap<String, Long>();
	public long lastTime = 0;
	
	public Benchmarker() {
		lastTime = System.currentTimeMillis();
	}
	
	public boolean addBreak(String name) {
		if(breaks.containsKey(name)) {
			return false;
		}
		
		long nowTime = System.currentTimeMillis();
		long breakTime = nowTime - lastTime;
		lastTime = nowTime;
		
		breaks.put(name, breakTime);
		return true;
	}
	
	public void addBreak() {		
		String breakName = "break " + breaks.size();
		
		addBreak(breakName);
	}
	
	public void echoToConsole() {
		for(Object breakPoints : breaks.keySet().toArray()) {
			String breakNames = (String) breakPoints;
			System.out.println(breakNames + "=" + breaks.get(breakNames));
		}
	}
	
	public void writeToFile(String fileName) {
		if (fileName == "") {
			fileName = "MoveCraft-BenchMark.txt";
		}
		
		File benchmarkFile = new File(NavyCraft.instance.getDataFolder(), fileName);
		if (!benchmarkFile.exists()) {
			return;
		}
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(benchmarkFile));

			for(Object breakPoints : breaks.keySet().toArray()) {
				String breakNames = (String) breakPoints;
				bw.write(breakNames + "=" + breaks.get(breakNames) + System.getProperty("line.separator"));
			}
			bw.close();
		}
		catch (IOException ex) {
		}	
	}
	
	public void echoSysInfo() {
		 /* Total number of processors or cores available to the JVM */
	    System.out.println("Available processors (cores): " + 
	        Runtime.getRuntime().availableProcessors());

	    /* Total amount of free memory available to the JVM */
	    System.out.println("Free memory (bytes): " + 
	        Runtime.getRuntime().freeMemory());

	    /* This will return Long.MAX_VALUE if there is no preset limit */
	    long maxMemory = Runtime.getRuntime().maxMemory();
	    /* Maximum amount of memory the JVM will attempt to use */
	    System.out.println("Maximum memory (bytes): " + 
	        (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));

	    /* Total memory currently in use by the JVM */
	    System.out.println("Total memory (bytes): " + 
	        Runtime.getRuntime().totalMemory());
	    
	    System.getProperties().list(System.out);
	    
	    //SystemEnvironment se = SystemEnvironment.getSystemEnvironment();
	    
	    try {
			Runtime.getRuntime().exec("notepad.exe");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
