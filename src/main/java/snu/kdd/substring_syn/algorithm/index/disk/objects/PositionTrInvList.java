package snu.kdd.substring_syn.algorithm.index.disk.objects;

public interface PositionTrInvList extends BytesMeasurableInterface, IterativePostingListInterface {
	
	public int getLeft();
	public int getRight();
	public int size();
	final int entrySize = 3;
}
