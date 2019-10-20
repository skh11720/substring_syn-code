package snu.kdd.pkwise;

import java.util.Iterator;

import snu.kdd.substring_syn.data.IntQGram;
import snu.kdd.substring_syn.data.IntQGramStore;
import snu.kdd.substring_syn.data.QGram;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.QGramGenerator;

public class TransWindowDataset extends WindowDataset {
	
	protected IntQGramStore iqgramStore;
	public int numIntQGrams;
	final int qlen;
	final double theta;

	public TransWindowDataset(String datasetName, String size, String nr, String qlen, String theta ) {
		super(datasetName, size, nr, qlen);
		this.qlen = Integer.parseInt(qlen);
		this.theta = Double.parseDouble(theta);
	}
	
	public final int getMaxQueryTransformLength() {
		int l = 0;
		for ( Record query : getSearchedList() ) {
			query.preprocessAll();
			l = Math.max(l, query.getMaxTransLength());
		}
		return l;
	}

	public final int getMinQueryTransformLength() {
		int l = Integer.MAX_VALUE;
		for ( Record query : getSearchedList() ) {
			query.preprocessAll();
			l = Math.min(l, query.getMinTransLength());
		}
		return l;
	}
	
	public final void buildIntQGramStore() {
		iqgramStore = new IntQGramStore(getIntQGramsIterable());
		numIntQGrams = iqgramStore.getNumIntQGrams();
	}
	
	public final Iterable<Record> getIntQGrams() {
		return iqgramStore.getIntQGrams();
	}
	
	public final IntQGram getIntQGram( int id ) {
		return iqgramStore.getIntQGram(id);
	}
	
	public Iterable<IntQGram> getIntQGramsIterable() {
		int wMin = (int)Math.ceil(qlen*theta);
		int wMax = (int)Math.floor(qlen/theta);
		IterableConcatenator<IntQGram> iterableList = new IterableConcatenator<>();
		for ( int w=wMin; w<=wMax; ++w ) iterableList.addIterable(getIntQGramIterable(w));
		return iterableList.iterable();
	}
	
	public Iterable<IntQGram> getIntQGramIterable( int q ) {
		return new Iterable<IntQGram>() {
			
			@Override
			public Iterator<IntQGram> iterator() {
				return getIntQGramIterator(q);
			}
		};
	}
	
	public Iterator<IntQGram> getIntQGramIterator( int q ) {
		return new IntQGramIterator(q);
	}
	
	


	
	class IntQGramIterator implements Iterator<IntQGram> {

		final Iterator<Record> riter;
		final int q;
		Iterator<QGram> qiter = null;
		Record rec;
		QGramGenerator qgen;
		
		public IntQGramIterator( int q ) {
			riter = recordStore.getRecords().iterator();
			this.q = q;
			findNext();
		}

		@Override
		public boolean hasNext() {
			return (riter.hasNext() || qiter.hasNext());
		}

		@Override
		public IntQGram next() {
			QGram qgram = qiter.next();
			IntQGram iqgram = new IntQGram(rec.getID(), qgram); 
			findNext();
			return iqgram;
		}
		
		private void findNext() {
			while ( qiter == null || !qiter.hasNext() ) {
				if ( !riter.hasNext() ) return;
				rec = riter.next();
				rec.preprocessApplicableRules();
				qgen = new QGramGenerator(rec, q);
				qiter = qgen.gen().iterator();
			}
		}
	}
}
