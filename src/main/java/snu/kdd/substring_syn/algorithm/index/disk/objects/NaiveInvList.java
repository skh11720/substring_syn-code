package snu.kdd.substring_syn.algorithm.index.disk.objects;

import java.util.Arrays;
import java.util.Iterator;

public class NaiveInvList implements BytesMeasurableInterface, PostingListInterface {

	final int[] arr;
	public final int length;
	
	public NaiveInvList(int[] arr, int length) {
		this.arr = Arrays.copyOf(arr, length);
		this.length = length;
	}
	
	public NaiveInvList(NaiveInvList o) {
		arr = Arrays.copyOf(o.arr, o.length);
		length = o.length;
	}
	
	public final int getId(final int i) {
		assert (i < length);
		return arr[i];
	}

	@Override
	public int bytes() {
		return length*Integer.BYTES;
	}

	@Override
	public int size() {
		return length;
	}
	
	public Iterator<Integer> iterator() {
		return new Iterator<Integer>() {
			
			int i = 0;
			
			@Override
			public Integer next() {
				return getId(i++);
			}
			
			@Override
			public boolean hasNext() {
				return i < length;
			}
		};
	}
}
