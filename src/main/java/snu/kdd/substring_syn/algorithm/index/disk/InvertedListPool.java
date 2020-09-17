package snu.kdd.substring_syn.algorithm.index.disk;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.algorithm.index.disk.objects.BytesMeasurableInterface;
import snu.kdd.substring_syn.utils.Long2IntHashBasedBinaryHeap;

public class InvertedListPool<E extends BytesMeasurableInterface> {

	private static final int BUFFER_SIZE = 10 * 1024 * 1024;
	
	private final Int2ObjectMap<E> map;
	private final Long2IntHashBasedBinaryHeap tic2tokMap;
	private final int capacity;
	private int size;
	private long tic;
	
	public InvertedListPool() {
		map = new Int2ObjectOpenHashMap<>();
		tic2tokMap = new Long2IntHashBasedBinaryHeap();
		capacity = BUFFER_SIZE;
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
	
	public E get( int token ) {
		if ( map.containsKey(token) ) {
			tic2tokMap.decreaseKeyOfValue(token, tic);
			++tic;
			return map.get(token);
		}
		else return null;
	}
	
	public void put( int token, E list ) {
		if ( map.containsKey(token) ) return;
		if ( size+list.bytes() > capacity ) getSpace(list.bytes());
		map.put(token, list);
		tic2tokMap.insert(tic, token);
		size += list.bytes();
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
		size -= map.get(token).bytes();
		tic2tokMap.poll();
		map.remove(token);
	}
}
