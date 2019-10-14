package snu.kdd.pkwise;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map.Entry;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Util;

public class PkwiseIndex {
	
	private final double theta;
	private final WindowDataset dataset;
	private final Int2ObjectMap<ObjectList<WindowInterval>> witvMap;

	public PkwiseIndex( PkwiseSearch alg, WindowDataset dataset, int qlen, double theta ) {
		this.theta = theta;
		this.dataset = dataset;
		witvMap = PkwiseIndexBuilder.buildTok2WitvMap(alg, dataset, qlen, theta);
	}
	
	public final Iterable<Subrecord> getCandWindowQuerySide( Record query ) {
		IterableConcatenator<Subrecord> iterableList = new IterableConcatenator<>();
		for ( int token : Util.getPrefix(query, theta) ) iterableList.addIterable(getWitvIterable(token));
//		for ( int token : query.getTokenArray() ) iterableList.addIterable(getWitvIterable(token));
		return iterableList.iterable();
	}

	public final Iterable<Subrecord> getWitvIterable( int token ) {
		return new Iterable<Subrecord>() {
			
			@Override
			public Iterator<Subrecord> iterator() {
				return getWitvIterator(token);
			}
		};
	}
	
	public final Iterator<Subrecord> getWitvIterator( int token ) {
		return new WitvIterator(token);
	}
	
	public final Int2ObjectMap<ObjectList<WindowInterval>> getWitvMap() {
		return witvMap;
	}
	
	public final void writeToFile() {
		try {
			PrintStream ps = null;
			ps = new PrintStream("tmp/PkwiseIndex.witvMap.txt");
			for ( Entry<Integer, ObjectList<WindowInterval>> e : getWitvMap().entrySet() ) ps.println(Record.tokenIndex.getToken(e.getKey())+"\t"+e);
			ps.close();
//			ps = new PrintStream("tmp/PkwiseIndex.twitvMap.txt");
//			for ( Entry<Integer, ObjectList<WindowInterval>> e : getTwitvMap().entrySet() ) ps.println(Record.tokenIndex.getToken(e.getKey())+"\t"+e);
//			ps.close();
		}
		catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	protected class AbstractWitvIterator implements Iterator<Subrecord> {
		
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
}
