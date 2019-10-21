package snu.kdd.substring_syn.data;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Log;

public class IntQGramStore {

	public static final String path = "./tmp/IntQGramStore";
	private final DB db;
	private final List<IntQGram> list;
	
	
	public IntQGramStore( Iterable<IntQGram> iqgramList ) {
		try {
			FileUtils.forceDelete(new File(path+".db"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		db = DBMaker.fileDB(path+".db").make();
		list = db.<IntQGram>indexTreeList("IntQGramList", Serializer.JAVA).create();
		materializeIntQGrams(iqgramList);
	}
	
	private void materializeIntQGrams( Iterable<IntQGram> iqgramList ) {
		int n = 0;
		for ( IntQGram iqgram : iqgramList ) {
			list.add(iqgram);
			n += 1;
			if ( (n%10_000) == 0 ) Log.log.info("materializeIntQGrams: list.size="+n);
		}
	}
	
	public IntQGram getIntQGram( int id ) {
		return list.get(id);
	}
	
	public Iterable<Record> getIntQGrams() {
		return new Iterable<Record>() {
			
			@Override
			public Iterator<Record> iterator() {
				return new IntQGramIterator();
			}
		};
	}
	
	public final int getNumIntQGrams() {
		return list.size();
	}
	
	class IntQGramIterator implements Iterator<Record> {
		
		Iterator<IntQGram> iter = list.iterator();
		
		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public Record next() {
			return iter.next().toRecord();
		}
	}
}
