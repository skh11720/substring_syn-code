package snu.kdd.pkwise;

import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map.Entry;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.index.disk.objects.NaiveInvList;
import snu.kdd.substring_syn.data.TransWindowDataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.Util;

public class PkwiseSynIndex {
	
	private final double theta;
	private final TransWindowDataset dataset;
	private final Int2ObjectMap<ObjectList<WindowInterval>> witvMap;
	private final PkwiseQGramIndexStore qgramIndexStore;
	private final PkwiseSignatureGenerator siggen;

	public PkwiseSynIndex( PkwiseSynSearch alg, TransWindowDataset dataset, int qlen, double theta ) {
		this.theta = theta;
		this.dataset = dataset;
		siggen = alg.getSiggen();
		int wMin = (int)Math.ceil(dataset.getMinQueryTransformLength()*theta);
		int wMax = (int)Math.floor(dataset.getMaxQueryTransformLength()/theta);
		witvMap = PkwiseIndexBuilder.buildTok2WitvMap(alg, dataset, wMin, wMax, theta);
		qgramIndexStore = new PkwiseQGramIndexStore(dataset.getIntQGrams(), theta, siggen, "PkwiseQGramIndexStore");
	}
	

	public final Iterable<RecordInterface> getCandWindowQuerySide( Record query ) {
		IterableConcatenator<RecordInterface> iterableList = new IterableConcatenator<>();
		int maxDiff = Util.getPrefixLength(query, theta);
		IntArrayList sig = siggen.genSignature(query, maxDiff, false);
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

	public final BigInteger diskSpaceUsage() {
		return dataset.getIqgramStore().diskSpaceUsage().add(new BigInteger(""+qgramIndexStore.storeSize));
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
		TransformableRecordInterface rec = null;
		
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
			TransformableRecordInterface rec0 = rec;
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
					if ( rec == null || rec.getIdx() != witv.rid ) rec = dataset.getRecord(witv.rid);
				}
			}
		}
	}
	
	class TwitvIterator implements Iterator<RecordInterface> {

		NaiveInvList list;

		public TwitvIterator( int token ) {
			list = qgramIndexStore.getInvList(token);
		}

		@Override
		public boolean hasNext() {
			return list != null && list.hasNext();
		}

		@Override
		public RecordInterface next() {
			int ridx = list.getIdx();
			list.next();
			return dataset.getIntQGram(ridx).toRecord();
		}
		
	}
}
