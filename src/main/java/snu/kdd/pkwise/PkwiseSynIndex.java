package snu.kdd.pkwise;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map.Entry;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.IntQGram;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Util;

public class PkwiseSynIndex {
	
	private final double theta;
	private final TransWindowDataset dataset;
	private final Int2ObjectMap<ObjectList<WindowInterval>> witvMap;
	private final Int2ObjectMap<IntList> twitvMap;
	private final PkwiseSignatureGenerator siggen;

	public PkwiseSynIndex( PkwiseSynSearch alg, TransWindowDataset dataset, int qlen, double theta ) {
		this.theta = theta;
		this.dataset = dataset;
		siggen = alg.getSiggen();
		int wMin = (int)Math.ceil(dataset.getMinQueryTransformLength()*theta);
		int wMax = (int)Math.floor(dataset.getMaxQueryTransformLength()/theta);
//		Log.log.trace("PkwiseSynIndex: wMin=%d", wMin);
//		Log.log.trace("PkwiseSynIndex: wMax=%d", wMax);
		witvMap = PkwiseIndexBuilder.buildTok2WitvMap(alg, dataset, wMin, wMax, theta);
		twitvMap = buildTok2iqgramsMap(dataset, qlen, theta);
	}
	
	private Int2ObjectMap<IntList> buildTok2iqgramsMap( TransWindowDataset dataset, int qlen, double theta ) {
		Int2ObjectMap<IntList> tok2iqgramsMap = new Int2ObjectOpenHashMap<>();
		int maxDiff = -1;
		int q = -1;
		int idx = 0;
		for ( IntQGram iqgram : dataset.getIntQGrams() ) {
			if ( q != iqgram.size() ) {
				q = iqgram.size();
				maxDiff = Util.getPrefixLength(q, theta);
			}
			Record rec = iqgram.toRecord();
			IntArrayList sig = siggen.genSignature(rec, maxDiff, true);
			for ( int token : sig ) {
				if ( !tok2iqgramsMap.containsKey(token) ) tok2iqgramsMap.put(token, new IntArrayList());
				tok2iqgramsMap.get(token).add(idx);
			}
			idx += 1;
		}
		return tok2iqgramsMap;
	}
	
	public final Iterable<RecordInterface> getCandWindowQuerySide( Record query ) {
		IterableConcatenator<RecordInterface> iterableList = new IterableConcatenator<>();
		int maxDiff = Util.getPrefixLength(query, theta);
		IntArrayList sig = siggen.genSignature(query, maxDiff, false);
//		Log.log.trace("sig=%s", sig);
		for ( int token : sig ) iterableList.addIterable(getWitvIterable(token));
		return iterableList.iterable();
	}

	public final Iterable<RecordInterface> getCandWindowTextSide( Record query ) {
		IterableConcatenator<RecordInterface> iterableList = new IterableConcatenator<>();
		int maxDiff = Util.getPrefixLength(query, theta);
		IntArrayList sig = siggen.genSignature(query, maxDiff, false);
		for ( int token : sig )  iterableList.addIterable(getTwitvIterable(token));
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
	
	public final Int2ObjectMap<IntList> getTwitvMap() {
		return twitvMap;
	}
	
	public final void writeToFile( KwiseSignatureMap sigMap ) {
		try {
			PrintStream ps = null;
			ps = new PrintStream("tmp/PkwiseSynIndex.witvMap.txt");
			for ( Entry<Integer, ObjectList<WindowInterval>> e : getWitvMap().entrySet() ) {
				if ( e.getKey() <= Record.tokenIndex.getMaxID() ) ps.println(Record.tokenIndex.getToken(e.getKey())+"\t"+e);
				else {
					KwiseSignature ksig = sigMap.get(e.getKey());
					ps.println(ksig.toOriginalString()+"\t"+ksig+"\t"+e);
				}
			}
			ps.close();
			ps = new PrintStream("tmp/PkwiseSynIndex.twitvMap.txt");
			for (  Entry<Integer, IntList> e : getTwitvMap().entrySet() ) {
				if ( e.getKey() <= Record.tokenIndex.getMaxID() ) ps.println(Record.tokenIndex.getToken(e.getKey())+"\t"+e);
				else {
					KwiseSignature ksig = sigMap.get(e.getKey());
					ps.println(ksig.toOriginalString()+"\t"+ksig+"\t"+e);
				}
			}
			ps.close();
		}
		catch ( IOException e ) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	
	class WitvIterator implements Iterator<RecordInterface> {
		
		final int token;
		ObjectList<WindowInterval> list;
		int iidx = -1;
		int widx = -1;
		int w;
		Record rec = null;
		
		public WitvIterator( int token ) {
			this.token = token;
			list = witvMap.get(token);
			if ( list != null ) findNext();
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
//			Log.log.trace("PkwiseSynIndex.WitvIterator\t"+(new Subrecord(rec0, sidx0, eidx0))+"\t"+(new Subrecord(rec0, sidx0, eidx0)).toOriginalString());
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
	
	class TwitvIterator implements Iterator<RecordInterface> {

		IntListIterator iter;

		public TwitvIterator( int token ) {
			IntList list = twitvMap.get(token);
			if ( list == null ) iter = null;
			else iter = twitvMap.get(token).iterator();
		}

		@Override
		public boolean hasNext() {
			return iter != null && iter.hasNext();
		}

		@Override
		public RecordInterface next() {
			return dataset.getIntQGram(iter.next()).toRecord();
		}
		
	}
}
