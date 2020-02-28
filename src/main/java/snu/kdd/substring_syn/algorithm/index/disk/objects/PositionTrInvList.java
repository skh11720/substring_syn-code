package snu.kdd.substring_syn.algorithm.index.disk.objects;

import java.util.Arrays;

public class PositionTrInvList implements BytesMeasurableInterface, PostingListInterface {

	final int[] arr;
	public final int size;
	
	public PositionTrInvList(int[] arr, int size) {
		this.arr = arr;
		this.size = size;
	}
	
	public PositionTrInvList(PositionTrInvList o) {
		arr = Arrays.copyOf(o.arr, 3*o.size);
		size = o.size;
	}
	
	public final int getId(final int i) {
		assert (i < size);
		return arr[3*i];
	}

	public final int getLeft(final int i) {
		assert (i < size);
		return arr[3*i+1];
	}

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
}
