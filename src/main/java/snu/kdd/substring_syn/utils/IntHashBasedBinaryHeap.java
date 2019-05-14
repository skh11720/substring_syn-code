package snu.kdd.substring_syn.utils;

import java.util.Comparator;
import java.util.stream.IntStream;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

/**
 * 
 * @author ghsong
 *
 */

public class IntHashBasedBinaryHeap extends IntBinaryHeap {
	
	protected Int2IntMap map;
	
	public IntHashBasedBinaryHeap() {
		super();
	}

	public IntHashBasedBinaryHeap( int[] arr ) {
		super(arr);
	}

	public IntHashBasedBinaryHeap( int[] arr, Comparator<Integer> comp ) {
		super(arr, comp);
	}
	
	public boolean containesKey( int key ) {
		return map.containsKey(key);
	}
	
	@Override
	public boolean isValidHeap() {
		if ( size != map.size() ) return false;
		for ( Int2IntMap.Entry entry : map.int2IntEntrySet() ) {
			if ( arr[entry.getIntValue()] != entry.getIntKey() ) return false;
		}
		return super.isValidHeap();
	}

	@Override
	public void insert( int key ) {
		map.put(key, size);
		super.insert(key);
	}
	
	public void delete( int key ) {
		if ( !map.containsKey(key) ) throw new RuntimeException("Key does not exist");
		int i = map.get(key);
		decreaseKey(i, getMinimumKey());
		poll();
	}

	@Override
	protected void deleteMin() {
		map.remove(arr[0]);
		super.deleteMin();
	}

	@Override
	protected void decreaseKeyKernel( int i, int v ) {
		map.remove(arr[i]);
		map.put(v, i);
		super.decreaseKeyKernel(i, v);
	}
	
	@Override
	public String toString() {
		StringBuilder strbld = new StringBuilder("[");
		for ( int i=0; i<(size+1)/2; ++i ) {
			strbld.append("  "+arr[i]+"("+map.get(arr[i])+")");
			if ( left(i) < size ) strbld.append(" -> "+arr[left(i)]+" ("+map.get(arr[left(i)])+")");
			if ( right(i) < size ) strbld.append(", "+arr[right(i)]+" ("+map.get(arr[right(i)])+")"+"\n");
		}
		strbld.append("] "+String.format("(%d/%d)", size, arr.length));
		return strbld.toString();
	}

	@Override
	protected void build() {
		map = new Int2IntOpenHashMap();
		for ( int i=0; i<size; ++i ) map.put(arr[i], i);
		super.build();
	}
	
	@Override
	protected void swap( int i, int j ) {
		if ( map.containsKey(arr[j]) ) map.put(arr[j], i);
		if ( map.containsKey(arr[i]) ) map.put(arr[i], j);
		super.swap(i, j);
	}


	public static void main(String[] args) {
		{
			int[] arr = {16,1,7,8,14,3,9,4,2,10};
			IntHashBasedBinaryHeap heap = new IntHashBasedBinaryHeap(arr, (x,y) -> Integer.compare(x,y));
			System.out.println(heap.map);
			System.out.println(heap);
			
			int[] brr = {24,34,11,23,5,13,6};
			for ( int key : brr ) {
				heap.insert(key);
				System.out.println(heap);
			}
			
			for ( int i=0; i<17; ++i ) {
				int minKey = heap.poll();
				System.out.println(minKey);
				System.out.println(heap);
			}
		}

		{
			int[] arr = {16,1,7,8,14,3,9,4,2,10};
			IntHashBasedBinaryHeap heap = new IntHashBasedBinaryHeap(arr, (x,y) -> Integer.compare(x,y));
			System.out.println(heap.map);
			System.out.println(heap);
			
			int[] brr = {24,34,11,23,5,13,6};
			for ( int key : brr ) {
				System.out.println("INSERT "+key);
				heap.insert(key);
				System.out.println(heap);
				if ( !heap.isValidHeap() ) throw new RuntimeException();
			}
			
			IntStream stream = IntStream.concat( IntStream.of(arr), IntStream.of(brr) );
			for ( int key : stream.toArray() ) {
				System.out.println("DELETE "+key);
				heap.delete(key);
				System.out.println(key);
				System.out.println(heap);
				if ( !heap.isValidHeap() ) throw new RuntimeException();
			}
		}
	}
}
