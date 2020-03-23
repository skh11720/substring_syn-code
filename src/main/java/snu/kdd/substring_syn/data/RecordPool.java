package snu.kdd.substring_syn.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.utils.Long2IntHashBasedBinaryHeap;
import snu.kdd.substring_syn.utils.StatContainer;

public class RecordPool<T extends RecordInterface> {

	public static int BUFFER_SIZE = (int)(1e4);
	
	private final Int2ObjectMap<T> map;
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
	
	public T get( int idx ) {
		if ( map.containsKey(idx) ) {
			tic2tokMap.decreaseKeyOfValue(idx, tic);
			++tic;
			return map.get(idx);
		}
		else return null;
	}
	
	public void put( int idx, T rec ) {
		if ( map.containsKey(idx) ) return;
		if ( size+rec.size() > capacity ) getSpace(rec.size());
		map.put(idx, rec);
		tic2tokMap.insert(tic, idx);
		size += rec.size();
	}
	
	private void getSpace( int required ) {
		StatContainer.global.startWatch("RecordPool.getSpace");
		while ( size > 0 && size+required > capacity ) {
			int token = chooseVictim();
			deallocate(token);
		}
		StatContainer.global.stopWatch("RecordPool.getSpace");
	}
	
	private int chooseVictim() {
		return tic2tokMap.peek();
	}
	
	private void deallocate( int token ) {
		StatContainer.global.startWatch("RecordPool.deallocate");
		size -= map.get(token).size();
		tic2tokMap.poll();
		map.remove(token);
		StatContainer.global.stopWatch("RecordPool.deallocate");
	}

}
