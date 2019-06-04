package snu.kdd.substring_syn.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.Dataset;

public class StatContainer {

	private final Object2ObjectOpenHashMap<String, String> statMap;
//	private Object2ObjectArrayMap<String, String> optionalStatMap;
	
	private final Object2ObjectOpenHashMap<String, Counter> counterBuffer;
	private final Object2ObjectOpenHashMap<String, StopWatch> stopwatchBuffer;
	
	private StatContainer() {
		statMap = new Object2ObjectOpenHashMap<>();
		statMap.defaultReturnValue("null");
//		optionalStatMap = new Object2ObjectArrayMap<>();
		counterBuffer = new Object2ObjectOpenHashMap<>();
		stopwatchBuffer = new Object2ObjectOpenHashMap<>();
	}
	
	public StatContainer( AbstractSearch alg, Dataset dataset ) {
		this();
		statMap.put(Stat.Alg_Name, alg.getName());
		statMap.put(Stat.Alg_Version, alg.getVersion());
		statMap.put(Stat.Dataset_Name, dataset.name);
		statMap.put(Stat.Dataset_numSearched, Integer.toString(dataset.searchedList.size()));
		statMap.put(Stat.Dataset_numIndexed, Integer.toString(dataset.indexedList.size()));
		statMap.put(Stat.Dataset_numRule, Integer.toString(dataset.ruleSet.size()));
	}
	
	public void finalizeAndOutput() {
		finalize();
		print();
		outputSummary();
	}

	public void finalize() {
		for ( String field : counterBuffer.keySet() ) statMap.put(field, Integer.toString(counterBuffer.get(field).get()));
		for ( String field : stopwatchBuffer.keySet() ) statMap.put(field, String.format("%.3f", stopwatchBuffer.get(field).get()/1e6));

	}
	
	public void print() {
		System.out.println("------------------------ Stat ------------------------");
		for ( String field : Stat.getList() ) {
			System.out.println(String.format("%25s  :  %s", field, statMap.get(field)));
		}
	}
	
	public void outputSummary() {
		PrintStream ps = null;
		try {
			ps = new PrintStream( new FileOutputStream("output/summary.txt", true) );
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		for ( String field : Stat.getList() ) {
			ps.print(field+":"+statMap.get(field)+"\t");
		}
		ps.println();
		ps.close();
	}


	
	// interfaces for counters

	public void increment( String field ) {
		if ( !counterBuffer.containsKey(field) ) counterBuffer.put(field, new Counter());
		counterBuffer.get(field).increment();
	}
	
	public void addCount( String field, int count ) {
		if ( !counterBuffer.containsKey(field) ) counterBuffer.put(field, new Counter());
		counterBuffer.get(field).add(count);
	}

	public void stopCount( String field ) {
		Counter counter = counterBuffer.get(field);
		statMap.put(field, Integer.toString(counter.get()));
		counterBuffer.remove(field);
	}
	
	
	
	// interfaces for stopwatches
	
	public void startWatch( String field ) {
		if ( !stopwatchBuffer.containsKey(field) ) stopwatchBuffer.put(field, new StopWatch());
		stopwatchBuffer.get(field).start();
	}
	
	public void stopWatch( String field ) {
		stopwatchBuffer.get(field).stop();
//		StopWatch watch = stopwatchBuffer.get(field);
//		double t = 0;
//		if ( statMap.containsKey(field) ) t = Double.parseDouble(statMap.get(field));
//		statMap.put(field, Double.toString(t+watch.stop()));
//		stopwatchBuffer.remove(field);
	}
	
	
	
	private class Counter {
		int c = 0;
		
		public void increment() {
			++c;
		}
		
		public void add( int v ) {
			c += v;
		}
		
		public int get() {
			return c;
		}
	}
	
	private class StopWatch {
		long t = 0;
		long dt;
		boolean active = false;
		
		public void start() {
			active = true;
			dt = System.nanoTime();
		}
		
		public void stop() {
			t += System.nanoTime() - dt;
			active = false;
		}
		
		public double get() {
			if (active) throw new RuntimeException();
			return t;
		}
	}
}
