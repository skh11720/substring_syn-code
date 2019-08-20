package snu.kdd.substring_syn.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Log;

public class InMemDataset extends Dataset {
	
	private final List<Record> searchedList;
	private final List<Record> indexedList;
	
	protected InMemDataset( String name, String rulePath, String searchedPath, String indexedPath, String outputPath ) throws IOException {
		super(name, rulePath, searchedPath, indexedPath, outputPath);
		searchedList = loadRecordList(searchedPath);
		indexedList = loadRecordList(indexedPath);
	}
	
	public Iterable<Record> getSearchedList() {
		return searchedList;
	}

	public Iterable<Record> getIndexedList() {
		return indexedList;
	}

	private List<Record> loadRecordList( String dataPath ) throws IOException {
		List<Record> recordList = new ObjectArrayList<>();
		BufferedReader br = new BufferedReader( new FileReader( dataPath ) );
		String line;
		for ( int i=0; ( line = br.readLine() ) != null; ++i ) {
			recordList.add( new Record( i, line ) );
		}
		br.close();
		Log.log.info("loadRecordList(%s): %d records", dataPath, recordList.size());
		return recordList;
	}
	
}
