package snu.kdd.substring_syn.algorithm.index.disk.objects;

import snu.kdd.substring_syn.algorithm.index.disk.PostingListAccessor;

public abstract class AbstractBufferedInvList implements BytesMeasurableInterface, IterativePostingListInterface {
	
	protected final PostingListAccessor acc;
	public int listIdx = 0;
	public int listSize = 0;
	
	public AbstractBufferedInvList(PostingListAccessor acc) {
		this.acc = acc;
	}
	
	protected abstract int entrySize();
	
	@Override
	public void init() {
		listIdx = 0;
		acc.reset();
		listSize = acc.nextChunk()/entrySize();
	}

	@Override
	public boolean hasNext() {
		return acc.hasNextChunk() || listIdx < listSize;
	}
	
	@Override
	public void next() {
		listIdx += 1;
		if ( listIdx >= listSize ) {
			int remainder = acc.chunkSize() - entrySize()*listIdx;
			if ( acc.hasNextChunk() ) {
				listIdx = 0;
				listSize = acc.nextChunk(remainder)/entrySize();
			}
		}
	}
	
	@Override
	public final int getIdx() {
//		assert (entrySize()*listIdx < listSize);
		return acc.getIBuf()[entrySize()*listIdx];
	}

	@Override
	public int bytes() {
		return acc.bytes();
	}
}
