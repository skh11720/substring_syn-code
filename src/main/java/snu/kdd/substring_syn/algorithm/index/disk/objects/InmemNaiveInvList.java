package snu.kdd.substring_syn.algorithm.index.disk.objects;

import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class InmemNaiveInvList extends AbstractInmemInvList implements NaiveInvList {

	public InmemNaiveInvList(int[] arr, int length) {
		super(arr, length);
	}
	
	public static InmemNaiveInvList copy(NaiveInvList o) {
		IntArrayList list = new IntArrayList();
		for ( o.init(); o.hasNext(); o.next() ) list.add(o.getIdx());
		int[] arr = list.toIntArray();
		int length = arr.length;
		return new InmemNaiveInvList(arr, length);
	}
	
	@Override
	protected int entrySize() {
		return 1;
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
