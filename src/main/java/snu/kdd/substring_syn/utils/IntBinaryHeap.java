package snu.kdd.substring_syn.utils;

import java.util.Comparator;

/**
 * 
 * @author ghsong
 *
 */

public class IntBinaryHeap {
	
	protected int[] keys;
	protected int[] values;
	protected int size = 0;
	protected Comparator<Integer> comp = null;
	
	public IntBinaryHeap() {
		this(10, Integer::compare);
	}
	
	public IntBinaryHeap( int initialCapacity ) {
		this(initialCapacity, Integer::compare);
	}

	public IntBinaryHeap( int initialCapacity, Comparator<Integer> comp ) {
		keys = new int[initialCapacity];
		values = new int[initialCapacity];
		this.comp = comp;
		size = 0;
		build();
	}
	
	public IntBinaryHeap( int[] keys, int[] values ) {
		this(keys, values, Integer::compare);
	}
	
	public IntBinaryHeap( int[] keys, int[] values, Comparator<Integer> comp ) {
		if ( keys.length != values.length ) throw new IllegalArgumentException();
		this.keys = keys;
		this.values = values;
		this.comp = comp;
		size = keys.length;
		build();
	}
	
	public boolean isEmpty() {
		return ( size == 0 );
	}
	
	public int size() {
		return size;
	}
	
	public int capacity() {
		return keys.length;
	}
	
	protected void heapify( int i ) {
		int l = left(i);
		int r = right(i);
		int smallest;
		if ( l < size && comp.compare(keys[i],  keys[l]) > 0 ) smallest =  l;
		else smallest = i;
		if ( r < size && comp.compare(keys[smallest],  keys[r]) > 0 ) smallest = r;
		if ( smallest != i ) {
			swap(i, smallest);
			heapify(smallest);
		}
	}
	
	public void decreaseKey( int i, int v ) {
		if ( v >  keys[i] ) throw new RuntimeException();
		decreaseKeyKernel(i, v);
	}

	protected void decreaseKeyKernel( int i, int v ) {
		keys[i] = v;
		while ( i > 0 && comp.compare(keys[parent(i)], keys[i]) > 0 ) {
			swap(i, parent(i));
			i = parent(i);
		}
	}
	
	public void insert( int key, int value ) {
		increaseSize();
		keys[size-1] = getMaximumKey();
		values[size-1] = value;
		decreaseKey(size-1, key);
	}
	
	protected void deleteMin() {
		swap(0, size-1);
		decreaseSize();
		heapify(0);
	}
	
	public int peek() {
		return keys[0];
	}
	
	public int poll() {
		if ( isEmpty() ) throw new RuntimeException();
		int minKey = keys[0];
		deleteMin();
		return minKey;
	}
	
	public boolean isValidHeap() {
		for ( int i=parent(size-1); i >=0; --i ) {
			int l = left(i);
			int r = right(i);
			if ( l < size && comp.compare(keys[i], keys[l]) > 0 ) return false;
			if ( r < size && comp.compare(keys[i], keys[r]) > 0 ) return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder strbld = new StringBuilder("[");
		for ( int i=0; i<(size+1)/2; ++i ) {
			strbld.append("  "+keys[i]+":"+values[i]);
			int l = left(i);
			int r = right(i);
			if ( l < size ) strbld.append(" -> "+keys[l]+":"+values[l]);
			if ( r < size ) strbld.append(", "+keys[r]+":"+values[r]+"\n");
		}
		strbld.append("] "+String.format("(%d/%d)", size, keys.length));
		return strbld.toString();
	}
	
	protected void checkUnderflow() {
		if ( isEmpty() ) throw new RuntimeException("Heap underflow occurred");
	}
	
	protected void build() {
		for ( int i=parent(size-1); i >=0; --i ) {
			heapify(i);
		}
	}
	
	protected int parent( int i ) {
		return (i-1)/2;
	}
	
	protected int left( int i ) {
		return 2*i+1;
	}
	
	protected int right( int i ) {
		return 2*i+2;
	}
	
	protected void swap( int i, int j ) {
		if ( i >= size || j >= size ) throw new RuntimeException();
		swapInArr(keys, i, j);
		swapInArr(values, i, j);
	}
	
	protected void swapInArr( int[] arr, int i, int j ) {
		int tmp = arr[i];
		arr[i] = arr[j];
		arr[j] = tmp;
	}
	
	protected void increaseSize() {
		if ( size == keys.length ) increaseCapacity();
		++size;
	}
	
	protected void decreaseSize() {
		if ( size < keys.length/3 ) decreaseCapacitiy();
		--size;
	}
	
	protected void increaseCapacity() {
		keys = getIncreasedArray(keys);
		values = getIncreasedArray(values);
	}
	
	protected int[] getIncreasedArray( int[] arr ) {
		int[] arr0 = new int[(arr.length+1)*3/2];
		for ( int i=0; i<size; ++i ) arr0[i] = arr[i];
		return arr0;
	}
	
	protected void decreaseCapacitiy() {
		keys = getDecreasedArray(keys);
		values = getDecreasedArray(values);
	}
	
	protected int[] getDecreasedArray( int[] arr ) {
		int[] arr0 = new int[arr.length*2/3];
		for ( int i=0; i<size; ++i ) arr0[i] = arr[i];
		return arr0;
	}
	
	protected int getMinimumKey() {
		if ( comp.compare(0, 1) < 0 ) return Integer.MIN_VALUE;
		else return Integer.MAX_VALUE;
	}

	protected int getMaximumKey() {
		if ( comp.compare(0, 1) < 0 ) return Integer.MAX_VALUE;
		else return Integer.MIN_VALUE;
	}

	
	public static void main(String[] args) {
		int[] keys = {16,1,7,8,14,3,9,4,2,10};
		int[] values = {100, 101, 102, 103, 104, 105, 106, 107, 108, 109};
		IntBinaryHeap heap = new IntBinaryHeap(keys, values, (x,y) -> Integer.compare(x,y));
		System.out.println(heap);
		if ( !heap.isValidHeap() ) throw new RuntimeException();
		
		int[] keys2 = {24,34,11, 11, 11, 11, 11};
		int[] values2 = {200, 201, 202, 203, 204, 205, 206};
		for ( int i=0; i<keys2.length; ++i ) {
			int key = keys2[i];
			int value = values2[i];
			heap.insert(key, value);
			System.out.println(heap);
			if ( !heap.isValidHeap() ) throw new RuntimeException();
		}
		
		for ( int i=0; i<17; ++i ) {
			int minKey = heap.poll();
			System.out.println(minKey);
			System.out.println(heap);
			if ( !heap.isValidHeap() ) throw new RuntimeException();
		}
	}
}
