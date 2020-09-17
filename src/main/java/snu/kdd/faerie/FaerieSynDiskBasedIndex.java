package snu.kdd.faerie;

import java.math.BigInteger;

import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.object.indexstore.EntryStore;
import snu.kdd.substring_syn.utils.Log;

public class FaerieSynDiskBasedIndex extends AbstractFaerieSynIndex {

	final EntryStore<FaerieSynIndexEntry> store;
	
	public FaerieSynDiskBasedIndex(Iterable<TransformableRecordInterface> records, String name) {
		super(records);
		Log.log.trace("FaerieSynDiskBasedIndex.constructor");
		store = new EntryStore<>(getEntries(Records.expands(records)), name);
	}

	@Override
	public final FaerieSynIndexEntry getEntry(int idx) {
		return store.getEntry(idx);
	}

	@Override
	public final BigInteger diskSpaceUsage() {
		return BigInteger.valueOf(store.storeSize);
	}
}
