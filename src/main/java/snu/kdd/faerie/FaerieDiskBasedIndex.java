package snu.kdd.faerie;

import java.math.BigInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.object.indexstore.EntryStore;

public class FaerieDiskBasedIndex implements FaerieIndexInterface {
	
	final EntryStore<FaerieIndexEntry> store;

	public FaerieDiskBasedIndex(Iterable<Record> records) {
		this(records, "FaerieDiskBasedInex_EntryStore");
	}

	public FaerieDiskBasedIndex(Iterable<Record> records, String name) {
		Stream<FaerieIndexEntry> stream = StreamSupport.stream(records.spliterator(), false).map(rec->new FaerieIndexEntry(rec));
		store = new EntryStore<FaerieIndexEntry>(stream::iterator, name);
	}
	
	@Override
	public final FaerieIndexEntry getEntry(int idx) {
		return store.getEntry(idx);
	}
	
	@Override
	public final BigInteger diskSpaceUsage() {
		return BigInteger.valueOf(store.storeSize);
	}
}
