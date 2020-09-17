package snu.kdd.substring_syn.utils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/**
 * 
 * @author ghsong
 *
 */

public class Long2IntHashBasedBinaryHeap extends Long2IntBinaryHeap {
	
	protected Int2IntMap val2idxMap;
	
	public Long2IntHashBasedBinaryHeap() {
		super();
	}

	public Long2IntHashBasedBinaryHeap( int initCapacity ) {
		super(initCapacity);
	}

	public Long2IntHashBasedBinaryHeap( int initCapacity, Comparator<Long> comp ) {
		super(initCapacity, comp);
	}

	public Long2IntHashBasedBinaryHeap(Comparator<Long> comp) {
		super(comp);
	}

	public Long2IntHashBasedBinaryHeap( long[] keys, int[] values ) {
		super(keys, values);
	}

	public Long2IntHashBasedBinaryHeap( long[] keys, int[] values, Comparator<Long> comp ) {
		super(keys, values, comp);
	}
	
	public boolean containesValue( int value ) {
		return val2idxMap.containsKey(value);
	}
	
	@Override
	public boolean isValidHeap() {
		if ( size != val2idxMap.size() ) return false;
		for ( Int2IntMap.Entry entry : val2idxMap.int2IntEntrySet() ) {
			if ( values[entry.getIntValue()] != entry.getIntKey() ) return false;
		}
		return super.isValidHeap();
	}

	@Override
	public void insert( long key, int value ) {
		val2idxMap.put(value, size);
		super.insert(key, value);
	}
	
	public void deleteByValue( int value ) {
		if ( !val2idxMap.containsKey(value) ) throw new RuntimeException("Value does not exist");
		int i = val2idxMap.get(value);
		val2idxMap.remove(value);
		increaseKey(i, getMinimumKey());
		poll();
	}

	public void deleteByValueIfExists( int value ) {
		if ( !val2idxMap.containsKey(value) ) return;
		int i = val2idxMap.get(value);
		val2idxMap.remove(value);
		increaseKey(i, getMinimumKey());
		poll();
	}

	@Override
	protected void deleteMin() {
		val2idxMap.remove(values[0]);
		super.deleteMin();
	}

	public void increaseKeyOfValue( int v, long newKey ) {
		int i = val2idxMap.get(v);
		super.increaseKey(i, newKey);
	}
	
	public void decreaseKeyOfValue( int v, long newKey ) {
		int i = val2idxMap.get(v);
		super.decreaseKey(i, newKey);
	}
	
	public long getKeySum() {
		return Arrays.stream(keys, 0, size).sum();
	}

	@Override
	public String toString() {
		StringBuilder strbld = new StringBuilder("[");
		for ( int i=0; i<(size+1)/2; ++i ) {
			strbld.append("  "+keys[i]+":"+values[i]+" ("+val2idxMap.get(values[i])+")");
			int l = left(i);
			int r = right(i);
			if ( l < size ) strbld.append(" -> "+keys[l]+":"+values[l]+" ("+val2idxMap.get(values[l])+")");
			if ( r < size ) strbld.append(", "+keys[r]+":"+values[r]+" ("+val2idxMap.get(values[r])+")"+"\n");
		}
		strbld.append("] "+String.format("(%d/%d)", size, keys.length));
		return strbld.toString();
	}

	@Override
	protected void build() {
		val2idxMap = new Int2IntOpenHashMap();
		for ( int i=0; i<size; ++i ) val2idxMap.put(values[i], i);
		super.build();
	}
	
	@Override
	protected void swap( int i, int j ) {
		if ( val2idxMap.containsKey(values[j]) ) val2idxMap.put(values[j], i);
		if ( val2idxMap.containsKey(values[i]) ) val2idxMap.put(values[i], j);
		super.swap(i, j);
	}


	public static void main(String[] args) {
		{
			long[] keys = {16,1,7,8,14,3,9,4,2,10};
			int[] values = {100, 101, 102, 103, 104, 105, 106, 107, 108, 109};
			Long2IntHashBasedBinaryHeap heap = new Long2IntHashBasedBinaryHeap(keys, values, (x,y) -> Long.compare(x,y));
			System.out.println(heap.val2idxMap);
			System.out.println(heap);
			
			long[] keys2 = {24,34,11, 11, 11, 11, 11};
			int[] values2 = {200, 201, 202, 203, 204, 205, 206};
			for ( int i=0; i<keys2.length; ++i ) {
				long key = keys2[i];
				int value = values2[i];
				heap.insert(key, value);
				System.out.println(heap);
			}
			
			for ( int i=0; i<17; ++i ) {
				long minKey = heap.poll().v;
				System.out.println(minKey);
				System.out.println(heap);
			}
		}

		{
			long[] keys = {16,1,7,8,14,3,9,4,2,10};
			int[] values = {100, 101, 102, 103, 104, 105, 106, 107, 108, 109};
			Long2IntHashBasedBinaryHeap heap = new Long2IntHashBasedBinaryHeap(keys, values, (x,y) -> Long.compare(x,y));
			System.out.println(heap.val2idxMap);
			System.out.println(heap);
			
			long[] keys2 = {24,34,11, 11, 11, 11, 11};
			int[] values2 = {200, 201, 202, 203, 204, 205, 206};
			for ( int i=0; i<keys2.length; ++i ) {
				long key = keys2[i];
				int value = values2[i];
				System.out.println("INSERT KEY, VALUE "+key+", "+value);
				heap.insert(key, value);
				System.out.println(heap.val2idxMap);
				System.out.println(heap);
				if ( !heap.isValidHeap() ) throw new RuntimeException();
			}
			
			IntStream stream = IntStream.concat( IntStream.of(values), IntStream.of(values2) );
			for ( int value : stream.toArray() ) {
				System.out.println("DELETE VALUE "+value);
				heap.deleteByValue(value);
				System.out.println(heap.val2idxMap);
				System.out.println(heap);
				if ( !heap.isValidHeap() ) throw new RuntimeException();
			}
		}

		{
			long[] keys = {16,1,7,8,14,3,9,4,2,10};
			int[] values = {100, 101, 102, 103, 104, 105, 106, 107, 108, 109};
			Long2IntHashBasedBinaryHeap heap = new Long2IntHashBasedBinaryHeap(keys, values, (x,y) -> Long.compare(x,y));
			System.out.println(heap.val2idxMap);
			System.out.println(heap);
			
			for ( int i=0; i<keys.length; ++i ) {
				long key = keys[i];
				int value = values[i];
				System.out.println("DECREASE KEY OF VALUE "+value+", "+key+" BY 300");
				heap.decreaseKeyOfValue(value, key+300);
				System.out.println(heap.val2idxMap);
				System.out.println(heap);
				if ( !heap.isValidHeap() ) throw new RuntimeException();
			}
		}
	}
}
