package snu.kdd.substring_syn.algorithm.index.disk.objects;

import snu.kdd.substring_syn.algorithm.index.disk.PostingListAccessor;

public class BufferedNaiveInvList implements BytesMeasurableInterface, IterativePostingListInterface {

	final PostingListAccessor acc;
	public int listIdx = 0;
	public int listSize = 0;
	
	public BufferedNaiveInvList(PostingListAccessor acc) {
		this.acc = acc;
	}
	
	@Override
	public void init() {
		listIdx = 0;
		acc.reset();
		listSize = acc.nextChunk();
	}

	@Override
	public boolean hasNext() {
		return acc.hasNextChunk() || listIdx < listSize;
	}
	
	@Override
	public void next() {
		listIdx += 1;
		if ( listIdx >= listSize ) {
			if ( acc.hasNextChunk() ) {
				listIdx = 0;
				listSize = acc.nextChunk();
			}
		}
	}
	
	@Override
	public final int getIdx() {
		assert (listIdx < listSize);
		return acc.getIBuf()[listIdx];
	}

	@Override
	public int bytes() {
		return acc.bytes();
	}
}
