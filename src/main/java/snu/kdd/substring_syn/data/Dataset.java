package snu.kdd.substring_syn.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class Dataset implements Iterable<Record> {
	public final String path;
	private final ObjectArrayList<Record> recordList;

	public Dataset( String dataPath, TokenIndex tokenIndex ) throws IOException {
		this.path = dataPath;
		this.recordList = new ObjectArrayList<>();

		BufferedReader br = new BufferedReader( new FileReader( dataPath ) );
		String line;
		for ( int i=0; ( line = br.readLine() ) != null; ++i ) {
			this.recordList.add( new Record( i, line, tokenIndex ) );
		}
		br.close();
	}

	public Dataset( ObjectArrayList<Record> record ) {
		this.path = null;
		this.recordList = record;
	}

	public Record getRecord( int id ) {
		return this.recordList.get( id );
	}

	public int size() {
		return this.recordList.size();
	}

	@Override
	public Iterator<Record> iterator() {
		return recordList.iterator();
	}
	
	public List<Record> recordList() {
		return recordList;
	}
}
