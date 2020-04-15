package snu.kdd.faerie;

import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Records;

public abstract class AbstractFaerieSynIndex implements FaerieSynIndexInterface {

	final IntList eidList;
	
	public AbstractFaerieSynIndex(Iterable<Record> records) {
		eidList = buildEntryIdList(records);
	}
	
	protected final Iterable<FaerieSynIndexEntry> getEntries(Iterable<Record> recExps) {
		return new Iterable<FaerieSynIndexEntry>() {
			
			@Override
			public Iterator<FaerieSynIndexEntry> iterator() {
				return new Iterator<FaerieSynIndexEntry>() {
					
					Iterator<Record> iter = recExps.iterator();
					
					@Override
					public FaerieSynIndexEntry next() {
						return new FaerieSynIndexEntry(iter.next());
					}
					
					@Override
					public boolean hasNext() {
						return iter.hasNext();
					}
				};
			}
		};
	}

	protected final IntList buildEntryIdList(Iterable<Record> records) {
		IntList eidList = new IntArrayList();
		eidList.add(0);
		int eid = 0;
		int rid = 0;
		for ( Record recExp : Records.expands(records) ) {
//			Log.log.trace("eid=%d, recExp.id=%d", eid, recExp.getID());
			if ( rid != recExp.getID() ) {
				eidList.add(eid);
				rid += 1;
			}
			eid += 1;
		}
		eidList.add(eid);
//		Log.log.trace("AbstractFaerieSynIndex.buildEntryIdList finished");
//		Log.log.trace("eidList.size=%d", eidList.size());
		return eidList;
	}

	@Override
	public final Iterable<FaerieSynIndexEntry> getRecordEntries(int id) {
		Stream<FaerieSynIndexEntry> entryStream = IntStream.range(eidList.getInt(id), eidList.getInt(id+1)).boxed().map(eid->getEntry(eid));
		return entryStream::iterator;
	}
}
