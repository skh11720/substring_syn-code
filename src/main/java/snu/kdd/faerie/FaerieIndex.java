package snu.kdd.faerie;

import java.math.BigInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.object.indexstore.EntryStore;

public class FaerieIndex {
	
	final EntryStore<FaerieIndexEntry> store;

	public FaerieIndex(Iterable<Record> records) {
		this(records, "FaerieInex_EntryStore");
	}

	public FaerieIndex(Iterable<Record> records, String name) {
		Stream<FaerieIndexEntry> stream = StreamSupport.stream(records.spliterator(), false).map(rec->new FaerieIndexEntry(rec));
		store = new EntryStore<FaerieIndexEntry>(stream::iterator, name);
	}
	
	public final FaerieIndexEntry getEntry(int id) {
		return store.getEntry(id);
	}
	
	public final BigInteger diskSpaceUsage() {
		return store.diskSpaceUsage();
	}
}
