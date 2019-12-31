package snu.kdd.faerie;

import java.math.BigInteger;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.object.indexstore.EntryStore;

public class FaerieSynDiskBasedIndex extends AbstractFaerieSynIndex {

	final EntryStore<FaerieSynIndexEntry> store;
	
	public FaerieSynDiskBasedIndex(Iterable<Record> records, String name) {
		super(records);
		store = new EntryStore<>(getEntries(Records.expands(records)), name);
	}

	@Override
	public final FaerieSynIndexEntry getEntry(int id) {
		return store.getEntry(id);
	}

	@Override
	public final BigInteger diskSpaceUsage() {
		return BigInteger.valueOf(store.storeSize);
	}
}
