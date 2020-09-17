package snu.kdd.faerie;

import java.util.Iterator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;

public abstract class AbstractFaerieSynIndex implements FaerieSynIndexInterface {

	final IntList eidxList;
	
	public AbstractFaerieSynIndex(Iterable<TransformableRecordInterface> records) {
		eidxList = buildEntryIdList(records);
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

	protected final IntList buildEntryIdList(Iterable<TransformableRecordInterface> records) {
		IntList eidxList = new IntArrayList();
		eidxList.add(0);
		int eidx = 0;
		int ridx = 0;
		for ( Record recExp : Records.expands(records) ) {
//			Log.log.trace("eid=%d, recExp.id=%d", eid, recExp.getID());
			if ( ridx != recExp.getIdx() ) {
				eidxList.add(eidx);
				ridx += 1;
			}
			eidx += 1;
		}
		eidxList.add(eidx);
//		Log.log.trace("AbstractFaerieSynIndex.buildEntryIdList finished");
//		Log.log.trace("eidList.size=%d", eidList.size());
		return eidxList;
	}

	@Override
	public final Iterable<FaerieSynIndexEntry> getRecordEntries(int idx) {
		Stream<FaerieSynIndexEntry> entryStream = IntStream.range(eidxList.getInt(idx), eidxList.getInt(idx+1)).boxed().map(eidx->getEntry(eidx));
		return entryStream::iterator;
	}
}
