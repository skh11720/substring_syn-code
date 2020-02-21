package snu.kdd.substring_syn.algorithm.validator;

import java.util.Iterator;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.iterator.SortedRecordSlidingWindowIterator;

public class NaiveValidator extends AbstractValidator {
	
	public NaiveValidator( double theta, StatContainer statContainer ) {
		super(theta, statContainer);
	}
	
	public boolean isOverThresholdQuerySide( Record query, Record rec ) {
		QuerySideIterator iter = new QuerySideIterator(query, rec);
		return isOverThreahold(iter);
	}

	public double simQuerySide( Record query, Record rec ) {
		QuerySideIterator iter = new QuerySideIterator(query, rec);
		return sim(iter);
	}
	
	public boolean isOverThresholdTextSide( Record query, Record rec ) {
		TextSideIterator iter = new TextSideIterator(query, rec);
		return isOverThreahold(iter);
	}
	
	public double simTextSide( Record query, Record rec ) {
		TextSideIterator iter = new TextSideIterator(query, rec);
		return sim(iter);
	}
	
	protected boolean isOverThreahold( Iterator<Double> iter ) {
		while ( iter.hasNext() ) {
			if ( iter.next() >= theta ) return true;
		}
		return false;
	}
	
	protected double sim( Iterator<Double> iter ) {
		double simMax = 0;
		while ( iter.hasNext() ) simMax = Math.max(simMax, iter.next());
		return simMax;
	}
	
	protected class QuerySideIterator implements Iterator<Double> {

		final RecordInterface rec;
		Iterator<Subrecord> witer;
		Iterator<Record> eiter;
		int w = 1;
		Record exp;
		Subrecord nw;
		int qidx = 0;
		
		public QuerySideIterator( Record query, RecordInterface rec ) {
			this.rec = rec;
			eiter = Records.expandAll(query).iterator();
			nw = findNext();
		}

		@Override
		public boolean hasNext() {
			return eiter.hasNext() || nw != null;
		}

		@Override
		public Double next() {
			Subrecord w = nw;
			nw = findNext();
			double sim = Util.jaccardM(exp.getTokenArray(), w.getTokenArray());
			if ( statContainer != null ) {
				statContainer.addCount(Stat.Len_QS_Verified, w.size());
				statContainer.increment(Stat.Num_QS_Verified);
			}
			return sim;
		}
		
		private Subrecord findNext() {
			while ( witer == null || !witer.hasNext() ) {
				if ( !eiter.hasNext() ) return null;
				exp = eiter.next();
				witer = Records.getSubrecords(rec).iterator();
			}
			return witer.next();
		}
	}

	protected class TextSideIterator implements Iterator<Double> {
		
		final Record query;
		Iterator<Record> expIter;
		SortedRecordSlidingWindowIterator witer;
		
		public TextSideIterator( Record query, Record rec ) {
			this.query = query;
			expIter = Records.expands(rec).iterator();
		}

		@Override
		public boolean hasNext() {
			return expIter.hasNext();
		}

		@Override
		public Double next() {
			Record exp = expIter.next();
			double sim = Util.subJaccardM(query.getTokenArray(), exp.getTokenArray());
			if ( statContainer != null ) {
				statContainer.addCount(Stat.Len_TS_Verified, exp.size());
				statContainer.increment(Stat.Num_TS_Verified);
			}
			return sim;
		}
	}

	@Override
	public String getName() {
		return "NaiveValidator";
	}
}
