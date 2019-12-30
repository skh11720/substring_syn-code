package snu.kdd.faerie;

import java.math.BigInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.record.Record;

public class FaerieSynMemBasedIndex implements FaerieSynIndexInterface {
	
	final ObjectList<FaerieSynIndexEntry> store;

	public FaerieSynMemBasedIndex(Iterable<Record> records) {
		this(records, "FaerieSynInex_EntryStore");
	}

	public FaerieSynMemBasedIndex(Iterable<Record> records, String name) {
		Stream<FaerieSynIndexEntry> stream = StreamSupport.stream(records.spliterator(), false).map(rec->new FaerieSynIndexEntry(rec));
		store = new ObjectArrayList<>(stream.iterator());
	}
	
	@Override
	public final FaerieSynIndexEntry getEntry(int id) {
		return store.get(id);
	}

	@Override
	public final BigInteger diskSpaceUsage() {
		return BigInteger.ZERO;
	}
}
