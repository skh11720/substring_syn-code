package snu.kdd.substring_syn.utils;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterable;

public class MaxBoundTokenCounter {
	
	private final Int2IntMap tokenMaxCountMap;
	private final Int2IntOpenHashMap counter;
	private int sum = 0;
	
	public MaxBoundTokenCounter( IntIterable iter ) {
		counter = new Int2IntOpenHashMap();
		tokenMaxCountMap = Util.getCounter(iter);
	}
	
	public void clear() {
		sum = 0;
		if ( counter.size() >= 1e6 ) counter.clear();
		else {
			for ( int key : counter.keySet() ) counter.put(key, 0);
		}
	}
	
	public boolean tryIncrement( int key ) {
		if ( counter.get(key) < tokenMaxCountMap.get(key) ) {
			increment(key);
			return true;
		}
		else return false;
	}

	private void increment( int key ) {
		counter.addTo(key, 1);
		sum += 1;
	}
	
	public int get( int key ) { return counter.get(key); }

	public int getMax( int key ) { return tokenMaxCountMap.get(key); }
	
	public int sum() { return sum; }
}
