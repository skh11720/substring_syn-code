package snu.kdd.substring_syn.algorithm.index.disk.objects;

import snu.kdd.substring_syn.algorithm.index.disk.PostingListAccessor;

public class BufferedNaiveInvList extends AbstractBufferedInvList implements NaiveInvList {

	public BufferedNaiveInvList(PostingListAccessor acc) {
		super(acc);
	}

	@Override
	protected int entrySize() {
		return 1;
	}
}
