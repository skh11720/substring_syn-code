package snu.kdd.substring_syn.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.json.simple.JSONObject;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.Dataset;

public class StatContainer {

	private AbstractSearch alg;
	private final Object2ObjectMap<String, String> statMap;
//	private Object2ObjectArrayMap<String, String> optionalStatMap;
	
	private final Object2ObjectMap<String, Counter> counterBuffer;
	private final Object2ObjectMap<String, StopWatch> stopwatchBuffer;
	List<String> keyList;
	
	public StatContainer() {
		statMap = new Object2ObjectArrayMap<>();
		statMap.defaultReturnValue("null");
//		optionalStatMap = new Object2ObjectArrayMap<>();
		counterBuffer = new Object2ObjectOpenHashMap<>();
		stopwatchBuffer = new Object2ObjectOpenHashMap<>();
	}
	
	public StatContainer( AbstractSearch alg, Dataset dataset ) {
		this();
		this.alg = alg;
//		putParam(alg.getParam());
		statMap.put(Stat.Alg_ID, alg.getID());
		statMap.put(Stat.Alg_Name, alg.getName());
		statMap.put(Stat.Alg_Version, alg.getVersion());
		statMap.put(Stat.Param, alg.getParam().toString());
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
	
	public String getStat( String key ) {
		return statMap.get(key);
	}
	
	public void finalizeAndOutput() {
		finalize();
		print();
		outputSummary();
	}

	public void finalize() {
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
	
	@SuppressWarnings("unchecked")
	public String outputJson() {
		JSONObject json = new JSONObject();
		Date date = new Date();
		json.put("Date", date.toString());
		
		JSONObject json_dataset = new JSONObject();
		json_dataset.put("Name", statMap.get(Stat.Dataset_Name));
		json_dataset.put("numSearched", statMap.get(Stat.Dataset_numSearched));
		json_dataset.put("numIndexed", statMap.get(Stat.Dataset_numIndexed));
		json_dataset.put("numRule", statMap.get(Stat.Dataset_numRule));
		json.put("Dataset", json_dataset);
		
		JSONObject json_alg = new JSONObject();
		json_alg.put("Name", statMap.get(Stat.Alg_Name));
		json_alg.put("Version", statMap.get(Stat.Alg_Version));
		json.put("Algorithm", json_alg);
		
		json.put("Param", alg.getParam().toJson());
		
		JSONObject json_output = new JSONObject();
		for ( String key : keyList ) json_output.put(key, statMap.get(key));
		json.put("Output", json_output);
		
		try {
			PrintWriter pw = new PrintWriter("json/"+statMap.get(Stat.Alg_Name)+"_"+(new SimpleDateFormat("yyyyMMdd_HHmmss_z")).format(date) + ".txt");
			pw.write(json.toJSONString());
			pw.close();
		} catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
		return "";
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
