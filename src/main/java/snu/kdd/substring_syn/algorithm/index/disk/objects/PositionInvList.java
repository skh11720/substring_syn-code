package snu.kdd.substring_syn.algorithm.index.disk.objects;

import java.util.Arrays;

public class PositionInvList implements BytesMeasurableInterface, PostingListInterface {

	final int[] arr;
	public final int size;
	public int listIdx = 0;
	
	public PositionInvList(int[] arr, int size) {
		this.arr = arr;
		this.size = size;
	}
	
	public PositionInvList(PositionInvList o) {
		arr = Arrays.copyOf(o.arr, 2*o.size);
		size = o.size;
	}

	public void init() {
		listIdx = 0;
	}
	
	public boolean hasNext() {
		return listIdx < size;
	}
	
	public void next() {
		listIdx += 1;
	}
	
	public final int getIdx() {
		assert (listIdx < size);
		return arr[2*listIdx];
	}

	public final int getPos() {
		assert (listIdx < size);
		return arr[2*listIdx+1];
	}

	@Deprecated
	public final int getIdx(final int i) {
		assert (i < size);
		return arr[2*i];
	}

	@Deprecated
	public final int getPos(final int i) {
		assert (i < size);
		return arr[2*i+1];
	}
	
	@Override
	public int bytes() {
		return 2*size*Integer.BYTES;
	}

	@Override
	public int size() {
		return size;
	}
	
	@Override
	public String toString() {
		return Arrays.toString(Arrays.copyOfRange(arr, 0, 2*size));
	}
}
