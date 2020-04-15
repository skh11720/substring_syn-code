package snu.kdd.substring_syn.algorithm.index.disk.objects;

public interface PositionInvList extends BytesMeasurableInterface, IterativePostingListInterface {
	
	public int getPos();
	public int size();
	final int entrySize = 2;
}
