package snu.kdd.faerie;

import java.math.BigInteger;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;

public class FaerieSynMemBasedIndex extends AbstractFaerieSynIndex {
	
	final ObjectList<FaerieSynIndexEntry> store;

	public FaerieSynMemBasedIndex(Iterable<TransformableRecordInterface> records) {
		super(records);
		store = new ObjectArrayList<>(getEntries(Records.expands(records)).iterator());
	}

	@Override
	public final FaerieSynIndexEntry getEntry(int idx) {
		return store.get(idx);
	}

	@Override
	public final BigInteger diskSpaceUsage() {
		return BigInteger.ZERO;
	}
}
