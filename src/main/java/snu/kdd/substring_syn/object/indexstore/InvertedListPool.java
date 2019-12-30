package snu.kdd.substring_syn.object.indexstore;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.utils.Long2IntHashBasedBinaryHeap;

public class InvertedListPool {

	private static final int BUFFER_SIZE = 1 * 1024;
	private static final int ENTRY_SIZE = 4;
	
	private final Int2ObjectMap<IntList> map;
	private final Long2IntHashBasedBinaryHeap tic2tokMap;
	private final int capacity;
	private int size;
	private long tic;
	
	public InvertedListPool() {
		map = new Int2ObjectOpenHashMap<>();
		tic2tokMap = new Long2IntHashBasedBinaryHeap();
		capacity = BUFFER_SIZE/ENTRY_SIZE;
		size = 0;
		tic = 0;
	}
	
	public final int size() { return size; }

	public final int capacity() { return capacity; }

	public final IntSet getKeys() {
		return map.keySet();
	}
	
	public boolean containsKey( int key ) {
		return map.containsKey(key);
	}
	
	public IntList get( int key ) {
		if ( map.containsKey(key) ) {
			tic2tokMap.decreaseKeyOfValue(key, tic);
			++tic;
			return map.get(key);
		}
		else return null;
	}
	
	public void put( int key, IntList list ) {
		if ( map.containsKey(key) ) return;
		if ( size+list.size() > capacity ) getSpace(list.size());
		map.put(key, list);
		tic2tokMap.insert(tic, key);
		size += list.size();
	}
	
	private void getSpace( int required ) {
		while ( size > 0 && size+required > capacity ) {
			int key = chooseVictim();
			deallocate(key);
		}
	}
	
	private int chooseVictim() {
		return tic2tokMap.peek();
	}
	
	private void deallocate( int key ) {
		size -= map.get(key).size();
		tic2tokMap.poll();
		map.remove(key);
	}
}
