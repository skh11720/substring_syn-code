package snu.kdd.faerie;

import java.math.BigInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.object.indexstore.EntryStore;

public class FaerieSynDiskBasedIndex implements FaerieSynIndexInterface {
	
	final EntryStore<FaerieSynIndexEntry> store;

	public FaerieSynDiskBasedIndex(Iterable<Record> records) {
		this(records, "FaerieSynDiskBasedIndex_EntryStore");
	}

	public FaerieSynDiskBasedIndex(Iterable<Record> records, String name) {
		Stream<FaerieSynIndexEntry> stream = StreamSupport.stream(records.spliterator(), false).map(rec->new FaerieSynIndexEntry(rec));
		store = new EntryStore<FaerieSynIndexEntry>(stream::iterator, name);
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
