package snu.kdd.substring_syn.algorithm.index.disk.objects;

public abstract class AbstractInmemInvList implements BytesMeasurableInterface, IterativePostingListInterface {

	protected final int[] arr;
	public final int size;
	public int listIdx = 0;
	
	public AbstractInmemInvList(int[] arr, int length) {
		this.arr = arr;
		this.size = length;
	}
	
	protected abstract int entrySize();
	
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
		return arr[entrySize()*listIdx];
	}
	
	@Override
	public int bytes() {
		return size*Integer.BYTES;
	}
}
