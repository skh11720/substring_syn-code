package snu.kdd.substring_syn.algorithm.index.disk;

import snu.kdd.substring_syn.data.record.Record;

public abstract class AbstractPositionalDiskBasedInvertedIndex<S, T>
extends AbstractDiskBasedInvertedIndex<S, T> 
implements DiskBasedPositionalIndexInterface {

	public AbstractPositionalDiskBasedInvertedIndex(Iterable<Record> recordList) {
		super(recordList);
	}
}
