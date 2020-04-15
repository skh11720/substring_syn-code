package snu.kdd.substring_syn.algorithm.index.disk.objects;

import snu.kdd.substring_syn.algorithm.index.disk.PostingListAccessor;

public class BufferedPositionInvList extends AbstractBufferedInvList implements PositionInvList {

	public BufferedPositionInvList(PostingListAccessor acc) {
		super(acc);
	}
	
	@Override
	protected int entrySize() {
		return entrySize;
	}

	@Override
	public final int getPos() {
		return acc.getIBuf()[entrySize*listIdx+1];
	}

	@Override
	public int size() {
		return acc.numInts()/entrySize;
	}
}
