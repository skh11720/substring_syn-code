package snu.kdd.substring_syn.algorithm.validator;

import java.util.Iterator;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;

public class NaiveQueryContainmentValidator extends AbstractValidator {
	
	public NaiveQueryContainmentValidator( double theta, StatContainer statContainer ) {
		super(theta, statContainer);
	}
	
	public boolean isOverThresholdQuerySide( Record query, RecordInterface rec ) {
		QuerySideIterator iter = new QuerySideIterator(query, rec);
		return isOverThreahold(iter);
	}

	public double simQuerySide( Record query, RecordInterface rec ) {
		QuerySideIterator iter = new QuerySideIterator(query, rec);
		return sim(iter);
	}
	
	public boolean isOverThresholdTextSide( Record query, TransformableRecordInterface rec ) {
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
		Iterator<Record> eiter;
		
		public QuerySideIterator( Record query, RecordInterface rec ) {
			this.rec = rec;
			eiter = Records.expands(query).iterator();
		}

		@Override
		public boolean hasNext() {
			return eiter.hasNext();
		}

		@Override
		public Double next() {
			Record exp = eiter.next();
			double sim = Util.jaccardContainmentM(exp.getTokenArray(), rec.getTokenArray());
			if ( statContainer != null ) {
				statContainer.addCount(Stat.Len_QS_Verified, rec.size());
				statContainer.increment(Stat.Num_QS_Verified);
			}
			return sim;
		}
	}
	
	protected class TextSideIterator implements Iterator<Double> {

		final Record query;
		Iterator<Record> eiter;
		
		public TextSideIterator( Record query, TransformableRecordInterface rec ) {
			this.query = query;
			eiter = Records.expands(rec).iterator();
		}

		@Override
		public boolean hasNext() {
			return eiter.hasNext();
		}

		@Override
		public Double next() {
			Record exp = eiter.next();
			double sim = Util.jaccardContainmentM(query.getTokenArray(), exp.getTokenArray());
			if ( statContainer != null ) {
				statContainer.addCount(Stat.Len_TS_Verified, exp.size());
				statContainer.increment(Stat.Num_TS_Verified);
			}
			return sim;
		}
	}

	@Override
	public String getName() {
		return "NaiveQueryContainmentValidator";
	}
}
