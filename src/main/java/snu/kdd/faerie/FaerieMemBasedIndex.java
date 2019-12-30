package snu.kdd.faerie;

import java.math.BigInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.record.Record;

public class FaerieMemBasedIndex implements FaerieIndexInterface {
	
	final ObjectList<FaerieIndexEntry> store;

	public FaerieMemBasedIndex(Iterable<Record> records) {
		this(records, "FaerieDiskBasedIndex_EntryStore");
	}

	public FaerieMemBasedIndex(Iterable<Record> records, String name) {
		Stream<FaerieIndexEntry> stream = StreamSupport.stream(records.spliterator(), false).map(rec->new FaerieIndexEntry(rec));
		store = new ObjectArrayList<>(stream.iterator());
	}
	
	@Override
	public final FaerieIndexEntry getEntry(int id) {
		return store.get(id);
	}
	
	@Override
	public final BigInteger diskSpaceUsage() {
		return BigInteger.ZERO;
	}
}
