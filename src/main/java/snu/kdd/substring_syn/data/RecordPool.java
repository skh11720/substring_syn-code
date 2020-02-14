package snu.kdd.substring_syn.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Long2IntHashBasedBinaryHeap;

public class RecordPool {

	public static int BUFFER_SIZE = (int)(1e8);
	
	private final Int2ObjectMap<Record> map;
	private final Long2IntHashBasedBinaryHeap tic2tokMap;
	private final int capacity;
	private int size;
	private long tic;
	
	public RecordPool() {
		this(BUFFER_SIZE);
	}
	
	public RecordPool(int capacity) {
		map = new Int2ObjectOpenHashMap<>();
		tic2tokMap = new Long2IntHashBasedBinaryHeap();
		this.capacity = capacity;
		size = 0;
		tic = 0;
	}
	
	public final int size() { return size; }

	public final int capacity() { return capacity; }

	public final IntSet getKeys() {
		return map.keySet();
	}
	
	public boolean containsKey( int token ) {
		return map.containsKey(token);
	}
	
	public Record get( int id ) {
		if ( map.containsKey(id) ) {
			tic2tokMap.decreaseKeyOfValue(id, tic);
			++tic;
			return map.get(id);
		}
		else return null;
	}
	
	public void put( int id, Record rec ) {
		if ( map.containsKey(id) ) return;
		if ( size+rec.size() > capacity ) getSpace(rec.size());
		map.put(id, rec);
		tic2tokMap.insert(tic, id);
		size += rec.size();
	}
	
	private void getSpace( int required ) {
		while ( size > 0 && size+required > capacity ) {
			int token = chooseVictim();
			deallocate(token);
		}
	}
	
	private int chooseVictim() {
		return tic2tokMap.peek();
	}
	
	private void deallocate( int token ) {
		size -= map.get(token).size();
		tic2tokMap.poll();
		map.remove(token);
	}

}
