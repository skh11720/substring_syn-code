package snu.kdd.pkwise;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map.Entry;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.WindowDataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Util;

public class PkwiseIndex {
	
	private final double theta;
	private final WindowDataset dataset;
	private final Int2ObjectMap<ObjectList<WindowInterval>> witvMap;

	public PkwiseIndex( PkwiseSearch alg, WindowDataset dataset, int qlen, double theta ) {
		this.theta = theta;
		this.dataset = dataset;
		witvMap = PkwiseIndexBuilder.buildTok2WitvMap(alg, dataset, qlen, qlen, theta);
	}
	
	public final Iterable<RecordInterface> getCandWindowQuerySide( Record query, PkwiseSignatureGenerator siggen ) {
		IterableConcatenator<RecordInterface> iterableList = new IterableConcatenator<>();
		int maxDiff = Util.getPrefixLength(query, theta);
		IntArrayList sig = siggen.genSignature(query, maxDiff, false);
//		Log.log.trace("query="+query);
//		Log.log.trace("query="+query.toOriginalString());
//		Log.log.trace("query sig="+sig);
//		for ( int token : sig )
//			if ( token <= Record.tokenIndex.getMaxID() ) Log.log.trace(token+"\t"+Record.tokenIndex.getToken(token));
//			else Log.log.trace(token+"\t"+siggen.getSigMap().get(token).toOriginalString());
		for ( int token : sig ) iterableList.addIterable(getWitvIterable(token));
//		for ( int token : query.getTokenArray() ) iterableList.addIterable(getWitvIterable(token));
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
	
	public final Iterator<RecordInterface> getWitvIterator( int token ) {
		return new WitvIterator(token);
	}
	
	public final Int2ObjectMap<ObjectList<WindowInterval>> getWitvMap() {
		return witvMap;
	}
	
	public final void writeToFile( KwiseSignatureMap sigMap ) {
		try {
			PrintStream ps = null;
			ps = new PrintStream("tmp/PkwiseIndex.witvMap.txt");
			for ( Entry<Integer, ObjectList<WindowInterval>> e : getWitvMap().entrySet() ) {
				if ( e.getKey() <= Record.tokenIndex.getMaxID() ) ps.println(Record.tokenIndex.getToken(e.getKey())+"\t"+e);
				else {
					KwiseSignature ksig = sigMap.get(e.getKey());
					ps.println(ksig.toOriginalString()+"\t"+ksig+"\t"+e);
				}
			}
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
	
	
	protected class AbstractWitvIterator implements Iterator<RecordInterface> {
		
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
