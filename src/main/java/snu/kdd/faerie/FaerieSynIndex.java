package snu.kdd.faerie;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.object.indexstore.EntryStore;

public class FaerieSynIndex {
	
	final EntryStore<FaerieSynIndexEntry> store;

	public FaerieSynIndex(Iterable<Record> records) {
		Stream<FaerieSynIndexEntry> stream = StreamSupport.stream(records.spliterator(), false).map(rec->new FaerieSynIndexEntry(rec));
		store = new EntryStore<FaerieSynIndexEntry>(stream::iterator, "FaerieSynIndex_EntryStore");
	}
	
	public FaerieSynIndexEntry getEntry(int id) {
		return store.getEntry(id);
	}
}
