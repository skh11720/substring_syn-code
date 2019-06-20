package snu.kdd.substring_syn.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map.Entry;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.Dataset;

public class StatContainer {

	private final Object2ObjectMap<String, String> statMap;
//	private Object2ObjectArrayMap<String, String> optionalStatMap;
	
	private final Object2ObjectMap<String, Counter> counterBuffer;
	private final Object2ObjectMap<String, StopWatch> stopwatchBuffer;
	List<String> keyList;
	
	private StatContainer() {
		statMap = new Object2ObjectArrayMap<>();
		statMap.defaultReturnValue("null");
//		optionalStatMap = new Object2ObjectArrayMap<>();
		counterBuffer = new Object2ObjectOpenHashMap<>();
		stopwatchBuffer = new Object2ObjectOpenHashMap<>();
	}
	
	public StatContainer( AbstractSearch alg, Dataset dataset ) {
		this();
//		putParam(alg.getParam());
		statMap.put(Stat.Alg_ID, alg.getID());
		statMap.put(Stat.Alg_Name, alg.getName());
		statMap.put(Stat.Alg_Version, alg.getVersion());
		statMap.put(Stat.Alg_Param, alg.getParam().toString());
		statMap.put(Stat.Dataset_Name, dataset.name);
		statMap.put(Stat.Dataset_numSearched, Integer.toString(dataset.searchedList.size()));
		statMap.put(Stat.Dataset_numIndexed, Integer.toString(dataset.indexedList.size()));
		statMap.put(Stat.Dataset_numRule, Integer.toString(dataset.ruleSet.size()));
	}
	
//	protected void putParam( Param param ) {
//		for ( Entry<String, String> entry : param.getEntries() ) {
//			statMap.put("Param_"+entry.getKey(), entry.getValue());
//		}
//	}
	
	public void finalizeAndOutput() {
		finalize();
		print();
		outputSummary();
	}

	protected void finalize() {
		for ( String key : counterBuffer.keySet() ) statMap.put(key, Integer.toString(counterBuffer.get(key).get()));
		for ( String key : stopwatchBuffer.keySet() ) statMap.put(key, String.format("%.3f", stopwatchBuffer.get(key).get()/1e6));
		keyList = new ObjectArrayList<>( Stat.getList() );
		for ( String key : statMap.keySet() ) {
			if ( !Stat.getSet().contains(key) ) keyList.add(key);
		}

	}
	
	public void print() {
		System.out.println("------------------------ Stat ------------------------");
		for ( String key : keyList ) {
			System.out.println(String.format("%25s  :  %s", key, statMap.get(key)));
		}
	}
	
	public void outputSummary() {
		PrintStream ps = null;
		try {
			ps = new PrintStream( new FileOutputStream("output/summary.txt", true) );
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		for ( String key : keyList ) {
			ps.print(key+":"+statMap.get(key)+"\t");
		}
		ps.println();
		ps.close();
	}


	
	// interfaces for counters

	public void increment( String key ) {
		if ( !counterBuffer.containsKey(key) ) counterBuffer.put(key, new Counter());
		counterBuffer.get(key).increment();
	}
	
	public void addCount( String key, int count ) {
		if ( !counterBuffer.containsKey(key) ) counterBuffer.put(key, new Counter());
		counterBuffer.get(key).add(count);
	}

	public void stopCount( String key ) {
		Counter counter = counterBuffer.get(key);
		statMap.put(key, Integer.toString(counter.get()));
		counterBuffer.remove(key);
	}
	
	
	
	// interfaces for stopwatches
	
	public void startWatch( String key ) {
		if ( !stopwatchBuffer.containsKey(key) ) stopwatchBuffer.put(key, new StopWatch());
		stopwatchBuffer.get(key).start();
	}
	
	public void stopWatch( String key ) {
		stopwatchBuffer.get(key).stop();
//		StopWatch watch = stopwatchBuffer.get(key);
//		double t = 0;
//		if ( statMap.containsKey(key) ) t = Double.parseDouble(statMap.get(key));
//		statMap.put(key, Double.toString(t+watch.stop()));
//		stopwatchBuffer.remove(key);
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
