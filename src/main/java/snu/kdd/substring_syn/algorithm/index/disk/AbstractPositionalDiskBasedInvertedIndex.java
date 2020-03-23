package snu.kdd.substring_syn.algorithm.index.disk;

import snu.kdd.substring_syn.algorithm.index.disk.objects.BytesMeasurableInterface;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;

public abstract class AbstractPositionalDiskBasedInvertedIndex<S extends BytesMeasurableInterface, T extends BytesMeasurableInterface>
extends AbstractDiskBasedInvertedIndex<S, T> {

	public AbstractPositionalDiskBasedInvertedIndex(Iterable<TransformableRecordInterface> recordList) {
		super(recordList);
	}
}
