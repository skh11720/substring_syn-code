package snu.kdd.faerie;

import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Records;

public abstract class AbstractFaerieSynIndex implements FaerieSynIndexInterface {

	final IntList eidList;
	
	public AbstractFaerieSynIndex(Iterable<Record> records) {
//		Stream<Record> recExpStream = StreamSupport.stream(records.spliterator(), false)
//				.flatMap(rec->StreamSupport.stream(expandIterator(rec).spliterator(), false));
//		Stream<FaerieSynIndexEntry> entryStream = recExpStream.map(recExp->new FaerieSynIndexEntry(recExp));
		eidList = buildEntryIdList(getRecExpStream(records)::iterator);
	}
	
	protected final Stream<Record> getRecExpStream(Iterable<Record> records) {
		return StreamSupport.stream(records.spliterator(), false)
				.flatMap(rec->StreamSupport.stream(expandIterator(rec).spliterator(), false));
	}
	
	protected final Stream<FaerieSynIndexEntry> getEntryStream(Iterable<Record> records) {
		return getRecExpStream(records).map(recExp->new FaerieSynIndexEntry(recExp));
	}

	protected final Iterable<Record> expandIterator(Record rec) {
		rec.preprocessAll();
		return Records.expands(rec);
	}

	protected final IntList buildEntryIdList(Iterable<Record> recExps) {
		IntList eidList = new IntArrayList();
		eidList.add(0);
		int eid = 0;
		int rid = 0;
		for ( Record recExp : recExps ) {
//			Log.log.trace("eid=%d, recExp.id=%d", eid, recExp.getID());
			if ( rid != recExp.getID() ) {
				eidList.add(eid);
				rid += 1;
			}
			eid += 1;
		}
		eidList.add(eid);
//		Log.log.trace("eidList.size=%d", eidList.size());
		return eidList;
	}

	@Override
	public final Iterable<FaerieSynIndexEntry> getRecordEntries(int id) {
		Stream<FaerieSynIndexEntry> entryStream = IntStream.range(eidList.getInt(id), eidList.getInt(id+1)).boxed().map(eid->getEntry(eid));
		return entryStream::iterator;
	}
}
