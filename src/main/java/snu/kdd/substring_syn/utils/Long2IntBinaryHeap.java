package snu.kdd.substring_syn.utils;

import java.util.Comparator;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import snu.kdd.substring_syn.data.IntLong;


public class Long2IntBinaryHeap {
	protected long[] keys;
	protected int[] values;
	protected int size = 0;
	protected Comparator<Long> comp = null;
	
	public Long2IntBinaryHeap() {
		this(10, Long::compare);
	}

	public Long2IntBinaryHeap(Comparator<Long> comp ) {
		this(10, comp);
	}
	
	public Long2IntBinaryHeap( int initialCapacity ) {
		this(initialCapacity, Long::compare);
	}

	public Long2IntBinaryHeap( int initialCapacity, Comparator<Long> comp ) {
		keys = new long[initialCapacity];
		values = new int[initialCapacity];
		this.comp = comp;
		size = 0;
		build();
	}
	
	public Long2IntBinaryHeap( long[] keys, int[] values ) {
		this(keys, values, Long::compare);
	}
	
	public Long2IntBinaryHeap( long[] keys, int[] values, Comparator<Long> comp ) {
		if ( keys.length != values.length ) throw new IllegalArgumentException();
		this.keys = keys;
		this.values = values;
		this.comp = comp;
		size = keys.length;
		build();
	}
	
	public LongList getKeys() {
		return new LongArrayList(keys, 0, size);
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

	public void increaseKey( int i, long newKey ) {
		if ( comp.compare(newKey, keys[i]) > 0 ) throw new RuntimeException();
		keys[i] = newKey;
		while ( i > 0 && comp.compare(keys[parent(i)], keys[i]) > 0 ) {
			swap(i, parent(i));
			i = parent(i);
		}
	}
	
	public void decreaseKey( int i, long newKey ) {
		if ( comp.compare(newKey, keys[i]) < 0 ) throw new RuntimeException();
		keys[i] = newKey;
		heapify(i);
	}
	
	public void insert( long key, int value ) {
		increaseSize();
		keys[size-1] = getMaximumKey();
		values[size-1] = value;
		increaseKey(size-1, key);
	}
	
	protected void deleteMin() {
		swap(0, size-1);
		decreaseSize();
		heapify(0);
	}
	
	public int peek() {
		if ( isEmpty() ) throw new RuntimeException();
		return values[0];
	}
	
	public IntLong poll() {
		if ( isEmpty() ) throw new RuntimeException();
		long minKey = keys[0];
		int value = values[0];
		deleteMin();
		return new IntLong(value, minKey);
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
		swapInKeyArr(keys, i, j);
		swapInValueArr(values, i, j);
	}

	protected void swapInKeyArr( long[] arr, int i, int j ) {
		long tmp = arr[i];
		arr[i] = arr[j];
		arr[j] = tmp;
	}
	
	protected void swapInValueArr( int[] arr, int i, int j ) {
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
		keys = getIncreasedKeyArr(keys);
		values = getIncreasedValueArr(values);
	}
	
	protected long[] getIncreasedKeyArr( long[] arr ) {
		long[] arr0 = new long[(arr.length+1)*3/2];
		for ( int i=0; i<size; ++i ) arr0[i] = arr[i];
		return arr0;
	}
	
	protected int[] getIncreasedValueArr( int[] arr ) {
		int[] arr0 = new int[(arr.length+1)*3/2];
		for ( int i=0; i<size; ++i ) arr0[i] = arr[i];
		return arr0;
	}
	
	protected void decreaseCapacitiy() {
		keys = getDecreasedKeyArr(keys);
		values = getDecreasedValueArr(values);
	}
	
	protected long[] getDecreasedKeyArr( long[] arr ) {
		long[] arr0 = new long[arr.length*2/3];
		for ( int i=0; i<size; ++i ) arr0[i] = arr[i];
		return arr0;
	}

	protected int[] getDecreasedValueArr( int[] arr ) {
		int[] arr0 = new int[arr.length*2/3];
		for ( int i=0; i<size; ++i ) arr0[i] = arr[i];
		return arr0;
	}
	
	protected long getMaximumKey() {
		if ( comp.compare(0L, 1L) < 0 ) return Long.MAX_VALUE;
		else return Long.MIN_VALUE;
	}

	protected long getMinimumKey() {
		if ( comp.compare(0L, 1L) < 0 ) return Long.MIN_VALUE;
		else return Long.MAX_VALUE;
	}

	
	public static void main(String[] args) {
		long[] keys = {16,1,7,8,14,3,9,4,2,10};
		int[] values = {100, 101, 102, 103, 104, 105, 106, 107, 108, 109};
		Long2IntBinaryHeap heap = new Long2IntBinaryHeap(keys, values, (x,y) -> Long.compare(x,y));
		System.out.println(heap);
		if ( !heap.isValidHeap() ) throw new RuntimeException();
		
		long [] keys2 = {24,34,11, 11, 11, 11, 11};
		int[] values2 = {200, 201, 202, 203, 204, 205, 206};
		for ( int i=0; i<keys2.length; ++i ) {
			long key = keys2[i];
			int value = values2[i];
			heap.insert(key, value);
			System.out.println(heap);
			if ( !heap.isValidHeap() ) throw new RuntimeException();
		}
		
		for ( int i=0; i<17; ++i ) {
			long minKey = heap.poll().v;
			System.out.println(minKey);
			System.out.println(heap);
			if ( !heap.isValidHeap() ) throw new RuntimeException();
		}
	}
}
