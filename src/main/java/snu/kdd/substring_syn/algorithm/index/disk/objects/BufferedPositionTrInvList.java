package snu.kdd.substring_syn.algorithm.index.disk.objects;

import snu.kdd.substring_syn.algorithm.index.disk.PostingListAccessor;

public class BufferedPositionTrInvList extends AbstractBufferedInvList implements PositionTrInvList {

	public BufferedPositionTrInvList(PostingListAccessor acc) {
		super(acc);
	}

	@Override
	protected int entrySize() {
		return entrySize;
	}

	@Override
	public int getLeft() {
		return acc.getIBuf()[entrySize*listIdx+1];
	}

	@Override
	public int getRight() {
		return acc.getIBuf()[entrySize*listIdx+2];
	}

	@Override
	public int size() {
		return acc.numInts()/entrySize;
	}
}
