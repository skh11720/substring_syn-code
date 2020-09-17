package snu.kdd.substring_syn.utils;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterable;

public class MaxBoundTokenCounterDeprecated {
	
	private final Int2IntMap tokenMaxCountMap;
	private Int2IntOpenHashMap counter;
	private final int sumBounds;
	private int sum = 0;
	
	public MaxBoundTokenCounterDeprecated( IntIterable iter ) {
		counter = new Int2IntOpenHashMap();
		tokenMaxCountMap = Util.getCounter(iter);
		sumBounds = tokenMaxCountMap.values().stream().mapToInt(Integer::intValue).sum();
	}
	
	public void clear() {
		sum = 0;
		counter = new Int2IntOpenHashMap();
	}
	
	public boolean tryIncrement( int key, int token ) {
		if ( counter.get(key) < tokenMaxCountMap.get(token) ) {
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

	public int sumBounds() { return sumBounds; }
}
