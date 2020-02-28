package snu.kdd.substring_syn.algorithm.index.disk.objects;

import java.util.Arrays;

public class PositionInvList implements BytesMeasurableInterface, PostingListInterface {

	final int[] arr;
	public final int size;
	
	public PositionInvList(int[] arr, int size) {
		this.arr = arr;
		this.size = size;
	}
	
	public PositionInvList(PositionInvList o) {
		arr = Arrays.copyOf(o.arr, 2*o.size);
		size = o.size;
	}
	
	public final int getId(final int i) {
		assert (i < size);
		return arr[2*i];
	}

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
