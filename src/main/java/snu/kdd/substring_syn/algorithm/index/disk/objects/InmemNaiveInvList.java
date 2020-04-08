package snu.kdd.substring_syn.algorithm.index.disk.objects;

import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class InmemNaiveInvList extends NaiveInvList {

	private final int[] arr;
	public final int size;
	public int listIdx = 0;
	
	public InmemNaiveInvList(int[] arr, int length) {
		this.arr = arr;
		this.size = length;
//		Log.log.trace("NaiveInvList: length=%d, arr=%s", this.length, Arrays.toString(this.arr));
	}
	
	public InmemNaiveInvList(NaiveInvList o) {
		IntArrayList list = new IntArrayList();
		for ( o.init(); o.hasNext(); o.next() ) list.add(o.getIdx());
		arr = list.toIntArray();
		size = arr.length;
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
				InmemNaiveInvList.this.next();
				return idx;
			}
			
			@Override
			public boolean hasNext() {
				return InmemNaiveInvList.this.hasNext();
			}
		};
	}
}
