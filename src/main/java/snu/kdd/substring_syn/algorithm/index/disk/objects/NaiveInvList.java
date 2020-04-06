package snu.kdd.substring_syn.algorithm.index.disk.objects;

import java.util.Arrays;
import java.util.Iterator;

public class NaiveInvList implements BytesMeasurableInterface, PostingListInterface {

	final int[] arr;
	public final int size;
	public int listIdx = 0;
	
	public NaiveInvList(int[] arr, int length) {
		this.arr = arr;
		this.size = length;
//		Log.log.trace("NaiveInvList: length=%d, arr=%s", this.length, Arrays.toString(this.arr));
	}
	
	public NaiveInvList(NaiveInvList o) {
		arr = Arrays.copyOf(o.arr, o.size);
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
		return arr[listIdx];
	}
	
	@Deprecated
	public final int getIdx(final int i) {
		assert (i < size);
		return arr[i];
	}

	@Override
	public int bytes() {
		return size*Integer.BYTES;
	}

	@Override
	public int size() {
		return size;
	}
	
	public Iterator<Integer> iterator() {
		return new Iterator<Integer>() {
			
			int i = 0;
			
			@Override
			public Integer next() {
				return getIdx(i++);
			}
			
			@Override
			public boolean hasNext() {
				return i < size;
			}
		};
	}
}
