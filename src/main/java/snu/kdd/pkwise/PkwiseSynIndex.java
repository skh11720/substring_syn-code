package snu.kdd.pkwise;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map.Entry;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Subrecord;

public class PkwiseSynIndex {
	
	private final WindowDataset dataset;
	private final Int2ObjectMap<ObjectList<WindowInterval>> witvMap;
	private final Int2ObjectMap<ObjectList<WindowInterval>> twitvMap;

	public PkwiseSynIndex( PkwiseSynSearch alg, WindowDataset dataset, int qlen, double theta ) {
		this.dataset = dataset;
		witvMap = PkwiseIndexBuilder.buildTok2WitvMap(alg, dataset, qlen, theta);
		twitvMap = PkwiseIndexBuilder.buildTok2TwitvMap(dataset, qlen, theta);
	}
	
	public final Iterable<RecordInterface> getCandWindowQuerySide( Record query ) {
		IterableConcatenator<RecordInterface> iterableList = new IterableConcatenator<>();
		for ( int token : query.getCandTokenSet() ) iterableList.addIterable(getWitvIterable(token));
		return iterableList.iterable();
	}

	public final Iterable<RecordInterface> getCandWindowTextSide( Record query ) {
		IterableConcatenator<RecordInterface> iterableList = new IterableConcatenator<>();
		for ( int token : query.getDistinctTokens() )  iterableList.addIterable(getTwitvIterable(token));
		return iterableList.iterable();
	}
	
	public final Iterable<RecordInterface> getWitvIterable( int token ) {
		return new Iterable<RecordInterface>() {
			
			@Override
			public Iterator<RecordInterface> iterator() {
				return getWitvIterator(token);
			}
		};
	}
	
	public final Iterable<RecordInterface> getTwitvIterable( int token ) {
		return new Iterable<RecordInterface>() {
			
			@Override
			public Iterator<RecordInterface> iterator() {
				return getTwitvIterator(token);
			}
		};
	}
	
	public final Iterator<RecordInterface> getWitvIterator( int token ) {
		return new WitvIterator(token);
	}

	public final Iterator<RecordInterface> getTwitvIterator( int token ) {
		return new TwitvIterator(token);
	}
	
	public final Int2ObjectMap<ObjectList<WindowInterval>> getWitvMap() {
		return witvMap;
	}
	
	public final Int2ObjectMap<ObjectList<WindowInterval>> getTwitvMap() {
		return twitvMap;
	}
	
	public final void writeToFile() {
		try {
			PrintStream ps = null;
			ps = new PrintStream("tmp/PkwiseSynIndex.witvMap.txt");
			for ( Entry<Integer, ObjectList<WindowInterval>> e : getWitvMap().entrySet() ) ps.println(Record.tokenIndex.getToken(e.getKey())+"\t"+e);
			ps.close();
			ps = new PrintStream("tmp/PkwiseSynIndex.twitvMap.txt");
			for ( Entry<Integer, ObjectList<WindowInterval>> e : getTwitvMap().entrySet() ) ps.println(Record.tokenIndex.getToken(e.getKey())+"\t"+e);
			ps.close();
		}
		catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	class AbstractWitvIterator implements Iterator<RecordInterface> {
		
		final int token;
		ObjectList<WindowInterval> list;
		int iidx = -1;
		int widx = -1;
		int w;
		Record rec = null;
		
		public AbstractWitvIterator( int token ) {
			this.token = token;
		}

		@Override
		public boolean hasNext() {
			return (rec != null);
		}

		@Override
		public Subrecord next() {
			Record rec0 = rec;
			int sidx0 = widx;
			int eidx0 = widx+w;
			findNext();
			return new Subrecord(rec0, sidx0, eidx0);
		}
		
		protected void findNext() {
			widx += 1;
			if ( iidx == -1 || widx >= list.get(iidx).eidx ) {
				iidx += 1;
				if ( iidx >= list.size() ) rec = null;
				else {
					WindowInterval witv = list.get(iidx);
					widx = witv.sidx;
					w = witv.w;
					if ( rec == null || rec.getID() != witv.rid ) rec = dataset.getRecord(witv.rid);
				}
			}
		}
	}
	
	class WitvIterator extends AbstractWitvIterator {
		public WitvIterator( int token ) {
			super(token);
			list = witvMap.get(token);
			if ( list != null ) findNext();
		}
	}
	
	class TwitvIterator extends AbstractWitvIterator {
		public TwitvIterator( int token ) {
			super(token);
			list = twitvMap.get(token);
			if ( list != null ) findNext();
		}
	}
}
