package snu.kdd.faerie;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.record.Record;

public class FaerieIndex {
	
	final ObjectList<FaerieIndexEntry> entryList;

	public FaerieIndex( Iterable<Record> records ) {
		entryList = new ObjectArrayList<FaerieIndexEntry>();
		for ( Record rec : records ) {
			entryList.add(new FaerieIndexEntry(rec.getTokenArray()));
		}
	}
}
