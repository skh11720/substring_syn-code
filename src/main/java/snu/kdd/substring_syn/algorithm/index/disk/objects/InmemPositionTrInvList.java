package snu.kdd.substring_syn.algorithm.index.disk.objects;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class InmemPositionTrInvList extends AbstractInmemInvList implements PositionTrInvList {

	public InmemPositionTrInvList(int[] arr, int length) {
		super(arr, length);
	}
	
	public static InmemPositionTrInvList copy(PositionTrInvList o) {
		IntArrayList list = new IntArrayList();
		for ( o.init(); o.hasNext(); o.next() ) {
			list.add(o.getIdx());
			list.add(o.getLeft());
			list.add(o.getRight());
		}
		int[] arr = list.toIntArray();
		int length = arr.length/entrySize;
		return new InmemPositionTrInvList(arr, length);
	}
	
	@Override
	protected int entrySize() {
		return entrySize;
	}
	
	@Override
	public final int getLeft() {
		assert (listIdx < size);
		return arr[entrySize*listIdx+1];
	}

	@Override
	public final int getRight() {
		assert (listIdx < size);
		return arr[entrySize*listIdx+2];
	}
	
	@Override
	public int size() {
		return size;
	}
	
	@Override
	public String toString() {
		return Arrays.toString(Arrays.copyOfRange(arr, 0, entrySize*size));
	}
}
