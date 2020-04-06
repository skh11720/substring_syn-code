package snu.kdd.substring_syn.algorithm.index.disk.objects;

import java.util.Arrays;

public class PositionTrInvList implements BytesMeasurableInterface, PostingListInterface {

	final int[] arr;
	public final int size;
	public int listIdx = 0;
	
	public PositionTrInvList(int[] arr, int size) {
		this.arr = arr;
		this.size = size;
	}
	
	public PositionTrInvList(PositionTrInvList o) {
		arr = Arrays.copyOf(o.arr, 3*o.size);
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
		return arr[3*listIdx];
	}

	public final int getLeft() {
		assert (listIdx < size);
		return arr[3*listIdx+1];
	}

	public final int getRight() {
		assert (listIdx < size);
		return arr[3*listIdx+2];
	}
	
	@Deprecated
	public final int getIdx(final int i) {
		assert (i < size);
		return arr[3*i];
	}

	@Deprecated
	public final int getLeft(final int i) {
		assert (i < size);
		return arr[3*i+1];
	}

	@Deprecated
	public final int getRight(final int i) {
		assert (i < size);
		return arr[3*i+2];
	}
	
	@Override
	public int bytes() {
		return 3*size*Integer.BYTES;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public String toString() {
		return Arrays.toString(Arrays.copyOfRange(arr, 0, 3*size));
	}
}
