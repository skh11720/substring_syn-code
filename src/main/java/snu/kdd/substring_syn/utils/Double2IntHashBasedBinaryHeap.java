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

public class Double2IntHashBasedBinaryHeap extends Double2IntBinaryHeap {
	
	protected Int2IntMap val2idxMap;
	
	public Double2IntHashBasedBinaryHeap() {
		super();
	}

	public Double2IntHashBasedBinaryHeap( int initCapacity ) {
		super(initCapacity);
	}

	public Double2IntHashBasedBinaryHeap( int initCapacity, Comparator<Double> comp ) {
		super(initCapacity, comp);
	}

	public Double2IntHashBasedBinaryHeap(Comparator<Double> comp) {
		super(comp);
	}

	public Double2IntHashBasedBinaryHeap( double[] keys, int[] values ) {
		super(keys, values);
	}

	public Double2IntHashBasedBinaryHeap( double[] keys, int[] values, Comparator<Double> comp ) {
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
	public void insert( double key, int value ) {
		val2idxMap.put(value, size);
		super.insert(key, value);
	}
	
	public void deleteByValue( int value ) {
		if ( !val2idxMap.containsKey(value) ) throw new RuntimeException("Value does not exist");
		int i = val2idxMap.get(value);
		val2idxMap.remove(value);
		decreaseKey(i, getMaximumKey());
		poll();
	}

	public void deleteByValueIfExists( int value ) {
		if ( !val2idxMap.containsKey(value) ) return;
		int i = val2idxMap.get(value);
		val2idxMap.remove(value);
		decreaseKey(i, getMaximumKey());
		poll();
	}

	@Override
	protected void deleteMin() {
		val2idxMap.remove(values[0]);
		super.deleteMin();
	}

	public void increaseKeyOfValue( int v, double newKey ) {
		int i = val2idxMap.get(v);
		super.increaseKey(i, newKey);
	}
	
	public void decreaseKeyOfValue( int v, double newKey ) {
		int i = val2idxMap.get(v);
		super.decreaseKey(i, newKey);
	}
	
	public double getKeySum() {
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
			double[] keys = {16,1,7,8,14,3,9,4,2,10};
			int[] values = {100, 101, 102, 103, 104, 105, 106, 107, 108, 109};
			Double2IntHashBasedBinaryHeap heap = new Double2IntHashBasedBinaryHeap(keys, values, (x,y) -> Double.compare(x,y));
			System.out.println(heap.val2idxMap);
			System.out.println(heap);
			
			double[] keys2 = {24,34,11, 11, 11, 11, 11};
			int[] values2 = {200, 201, 202, 203, 204, 205, 206};
			for ( int i=0; i<keys2.length; ++i ) {
				double key = keys2[i];
				int value = values2[i];
				heap.insert(key, value);
				System.out.println(heap);
			}
			
			for ( int i=0; i<17; ++i ) {
				double minKey = heap.poll().v;
				System.out.println(minKey);
				System.out.println(heap);
			}
		}

		{
			double[] keys = {16,1,7,8,14,3,9,4,2,10};
			int[] values = {100, 101, 102, 103, 104, 105, 106, 107, 108, 109};
			Double2IntHashBasedBinaryHeap heap = new Double2IntHashBasedBinaryHeap(keys, values, (x,y) -> Double.compare(x,y));
			System.out.println(heap.val2idxMap);
			System.out.println(heap);
			
			double[] keys2 = {24,34,11, 11, 11, 11, 11};
			int[] values2 = {200, 201, 202, 203, 204, 205, 206};
			for ( int i=0; i<keys2.length; ++i ) {
				double key = keys2[i];
				int value = values2[i];
				System.out.println("INSERT "+key);
				heap.insert(key, value);
				System.out.println(heap.val2idxMap);
				System.out.println(heap);
				if ( !heap.isValidHeap() ) throw new RuntimeException();
			}
			
			IntStream stream = IntStream.concat( IntStream.of(values), IntStream.of(values2) );
			for ( int value : stream.toArray() ) {
				System.out.println("DELETE "+value);
				heap.deleteByValue(value);
				System.out.println(heap.val2idxMap);
				System.out.println(heap);
				if ( !heap.isValidHeap() ) throw new RuntimeException();
			}
		}
	}
}
