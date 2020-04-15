package snu.kdd.substring_syn.algorithm.index.disk.objects;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class InmemPositionInvList extends AbstractInmemInvList implements PositionInvList {

	public InmemPositionInvList(int[] arr, int length) {
		super(arr, length);
	}
	
	public static InmemPositionInvList copy(PositionInvList o) {
		IntArrayList list = new IntArrayList();
		for ( o.init(); o.hasNext(); o.next() ) {
			list.add(o.getIdx());
			list.add(o.getPos());
		}
		int[] arr = list.toIntArray();
		int length = arr.length/entrySize;
		return new InmemPositionInvList(arr, length);
	}
	
	@Override
	protected int entrySize() {
		return entrySize;
	}

	@Override
	public final int getPos() {
		assert (listIdx < size);
		return arr[entrySize*listIdx+1];
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
