package snu.kdd.substring_syn.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import org.json.simple.JSONObject;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;

public class StatContainer {
	
	public static StatContainer global = null;

	private AbstractSearch alg;
	private final Object2ObjectMap<String, String> statMap;
//	private Object2ObjectArrayMap<String, String> optionalStatMap;
	
	private final Object2ObjectMap<String, Counter> counterBuffer;
	private final Object2ObjectMap<String, StopWatch> stopwatchBuffer;
	private final Object2ObjectMap<String, BasicStatCalculator> statBuffer;
	List<String> keyList;
	
	public StatContainer() {
		statMap = new Object2ObjectArrayMap<>();
		statMap.defaultReturnValue("null");
//		optionalStatMap = new Object2ObjectArrayMap<>();
		counterBuffer = new Object2ObjectOpenHashMap<>();
		stopwatchBuffer = new Object2ObjectOpenHashMap<>();
		statBuffer = new Object2ObjectOpenHashMap<>();
	}
	
	public void setAlgorithm( AbstractSearch alg ) {
		this.alg = alg;
//		putParam(alg.getParam());
		statMap.put(Stat.Alg_ID, alg.getID());
		statMap.put(Stat.Alg_Name, alg.getName());
		statMap.put(Stat.Alg_Version, alg.getVersion());
		statMap.put(Stat.Param, alg.getParam().toString());
	}
	
	public void setStat( String key, String value ) {
		statMap.put(key, value);
	}
	
	public String getStat( String key ) {
		return statMap.get(key);
	}
	
	public void finalizeAndOutput() {
		finalize();
		setDefault();
		print();
		outputSummary();
	}

	public void finalize() {
		for ( String key : counterBuffer.keySet() ) statMap.put(key, Long.toString(counterBuffer.get(key).get()));
		for ( String key : stopwatchBuffer.keySet() ) statMap.put(key, String.format("%.3f", stopwatchBuffer.get(key).get()/1e6));
		for ( String key : statBuffer.keySet() ) {
			statMap.put(key, String.format("%.3f", statBuffer.get(key).mean()));
			statMap.put(key+"_MIN", String.format("%.3f", statBuffer.get(key).min()));
			statMap.put(key+"_MAX", String.format("%.3f", statBuffer.get(key).max()));
			statMap.put(key+"_MEAN", String.format("%.3f", statBuffer.get(key).mean()));
			statMap.put(key+"_STD", String.format("%.3f", statBuffer.get(key).std()));
		}
		keyList = new ObjectArrayList<>( Stat.getList() );
		statMap.keySet().stream().sorted().forEach(key->{
			if ( !Stat.getSet().contains(key) ) keyList.add(key);
		});
	}
	
	private void setDefault() {
		for ( String key : keyList ) {
			if ( !statMap.containsKey(key) ) {
				if ( key.startsWith("Num") || key.startsWith("Len") || key.startsWith("Mem") ) statMap.put(key, "0");
				else if ( key.startsWith("Time") ) statMap.put(key, "0.0");
				else statMap.put(key, "null");
			}
		}
	}
	
	public void print() {
		Log.log.info("------------------------ Stat ------------------------");
		for ( String key : keyList ) {
			Log.log.info(String.format("%25s  :  %s", key, statMap.get(key)));
		}
	}
	
	public String outputSummaryString() {
		StringBuilder strbld = new StringBuilder();
		for ( String key : keyList ) {
			strbld.append(key+":"+statMap.get(key)+"\t");
		}
		return strbld.toString();
	}
	
	public void outputSummary() {
		PrintStream ps = null;
		try {
			ps = new PrintStream( new FileOutputStream("output/summary.txt", true) );
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		ps.println(outputSummaryString());
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
	
	public void addCount( String key, long count ) {
		if ( !counterBuffer.containsKey(key) ) counterBuffer.put(key, new Counter());
		counterBuffer.get(key).add(count);
	}

	public void stopCount( String key ) {
		Counter counter = counterBuffer.get(key);
		statMap.put(key, Long.toString(counter.get()));
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
	
	public void addSampleValue( String key, double value ) {
		if ( !statBuffer.containsKey(key) ) statBuffer.put(key, new BasicStatCalculator());
		statBuffer.get(key).append(value);
	}
	
	public void mergeStatContainer( StatContainer statContainer ) {
		for ( Entry<String, String> entry : statContainer.statMap.entrySet() ) {
			statMap.put(entry.getKey(), entry.getValue());
		}
	}
	
	
	
	private class Counter {
		long c = 0;
		
		public void increment() {
			++c;
		}
		
		public void add( long v ) {
			c += v;
		}
		
		public long get() {
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
			if (active) t += System.nanoTime() - dt;
			active = false;
		}
		
		public double get() {
			if (active) throw new RuntimeException();
			return t;
		}
	}
	
	private class BasicStatCalculator {
		int n = 0;
		double sum = 0;
		double sqsum = 0;
		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;
		
		public void append( double value ) {
			++n;
			sum += value;
			sqsum += value*value;
			max = Math.max(max, value);
			min = Math.min(min, value);
		}
		public double max() { return max; }
		public double min() { return min; }
		public double mean() { return sum/n; }
		public double std() { return Math.sqrt(sqsum/n - mean()*mean()); }
	}
}
