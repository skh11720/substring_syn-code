package snu.kdd.substring_syn.algorithm.index.disk.objects;

import java.util.Arrays;
import java.util.Iterator;

public class NaiveInvList implements BytesMeasurableInterface, IterativePostingListInterface {

	private final int[] arr;
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
	
	@Override
	public void init() {
		listIdx = 0;
	}

	@Override
	public boolean hasNext() {
		return listIdx < size;
	}
	
	@Override
	public void next() {
		listIdx += 1;
	}
	
	@Override
	public int getIdx() {
		assert (listIdx < size);
		return arr[listIdx];
	}
	
	@Override
	public int bytes() {
		return size*Integer.BYTES;
	}

	public Iterator<Integer> iterator() {
		init();
		return new Iterator<Integer>() {
			
			@Override
			public Integer next() {
				int idx = getIdx();
				NaiveInvList.this.next();
				return idx;
			}
			
			@Override
			public boolean hasNext() {
				return NaiveInvList.this.hasNext();
			}
		};
	}
}
