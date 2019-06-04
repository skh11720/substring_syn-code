package snu.kdd.substring_syn.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
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
		counterBuffer = new Object2ObjectOpenHashMap<>();;
		stopwatchBuffer = new Object2ObjectOpenHashMap<>();;
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
		Set<String> counterFieldList = new ObjectOpenHashSet<>( counterBuffer.keySet() );
		for ( String field : counterFieldList ) stopCount(field);
		Set<String> stopwatchFieldList = new ObjectOpenHashSet<>( stopwatchBuffer.keySet() );
		for ( String field : stopwatchFieldList ) stopWatch(field);
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
		stopwatchBuffer.put(field, new StopWatch());
	}
	
	public void stopWatch( String field ) {
		StopWatch watch = stopwatchBuffer.get(field);
		double t = 0;
		if ( statMap.containsKey(field) ) t = Double.parseDouble(statMap.get(field));
		statMap.put(field, Double.toString(t+watch.stop()));
		stopwatchBuffer.remove(field);
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
		final long t = System.nanoTime();;
		
		public double stop() {
			return (System.nanoTime() - t)/1e6;
		}
	}
}
