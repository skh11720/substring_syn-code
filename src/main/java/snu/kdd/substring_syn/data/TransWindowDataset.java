package snu.kdd.substring_syn.data;

import java.io.File;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;

import snu.kdd.pkwise.IterableConcatenator;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.QGramGenerator;
import snu.kdd.substring_syn.utils.StatContainer;

public class TransWindowDataset extends WindowDataset {
	
	protected final double theta;
	protected final IntQGramStore iqgramStore;

	public TransWindowDataset(StatContainer statContainer, DatasetParam param, Ruleset ruleset, RecordStore store, String theta) {
		super(statContainer, param, ruleset, store);
		this.theta = Double.parseDouble(theta);
		this.iqgramStore = buildIntQGramStore();
	}

	@Override
	public void addStat() {
		super.addStat();
		statContainer.setStat("Size_Recordstore", store.diskSpaceUsage().toString());
		statContainer.setStat("Size_IntQGramStore", FileUtils.sizeOfAsBigInteger(new File(IntQGramStore.path)).toString());
		statContainer.setStat("Num_IntQGrams", Integer.toString(iqgramStore.getNumIntQGrams()));
	}
	
	public final IntQGramStore getIqgramStore() {
		return iqgramStore;
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
	
	public final IntQGramStore buildIntQGramStore() {
		return new IntQGramStore(getIntQGramsIterable());
	}
	
	public final Iterable<Record> getIntQGrams() {
		return iqgramStore.getIntQGrams();
	}
	
	public final IntQGram getIntQGram( int idx ) {
		return iqgramStore.getIntQGram(idx);
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
			riter = store.getRecords().iterator();
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
			IntQGram iqgram = new IntQGram(rec.getIdx(), qgram); 
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
