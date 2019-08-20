package snu.kdd.substring_syn.algorithm.validator;

import java.util.Iterator;

import it.unimi.dsi.fastutil.objects.ObjectList;
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
	
	class QuerySideIterator implements Iterator<Double> {

		final RecordInterface rec;
		SortedRecordSlidingWindowIterator witer;
		ObjectList<Record> queryExpArr;
		int w = 1;
		Subrecord window;
		int qidx = 0;
		
		public QuerySideIterator( Record query, RecordInterface rec ) {
			this.rec = rec;
			queryExpArr = Records.expandAll(query);
			witer = new SortedRecordSlidingWindowIterator(rec, w, theta);
			window = witer.next();
		}

		@Override
		public boolean hasNext() {
			return w <= rec.size();
		}

		@Override
		public Double next() {
			double sim = Util.jaccardM( queryExpArr.get(qidx).getTokenArray(), window.getTokenArray());
			++qidx;
			if ( statContainer != null ) {
				statContainer.addCount(Stat.Len_QS_Verified, window.size());
				statContainer.increment(Stat.Num_QS_Verified);
			}
			if ( qidx >= queryExpArr.size() ) {
				qidx = 0;
				if ( witer.hasNext() ) window = witer.next();
				else {
					++w;
					if ( w <= rec.size() ) {
						witer = new SortedRecordSlidingWindowIterator(rec, w, theta);
						window = witer.next();
					}
					else witer = null;
				}
			}
			return sim;
		}
	}

	class TextSideIterator implements Iterator<Double> {
		
		final Record query;
		final ObjectList<Record> expList;
		SortedRecordSlidingWindowIterator witer;
		int eidx = 0;
		int w = 1;
		Subrecord window;
		
		public TextSideIterator( Record query, Record rec ) {
			this.query = query;
			expList = Records.expandAll(rec);
			witer = new SortedRecordSlidingWindowIterator(expList.get(eidx), w, theta);
			window = witer.next();
		}

		@Override
		public boolean hasNext() {
			return eidx < expList.size();
		}

		@Override
		public Double next() {
			double sim = Util.jaccardM(window.getTokenArray(), query.getTokenArray());
			if ( statContainer != null ) {
				statContainer.addCount(Stat.Len_TS_Verified, window.size());
				statContainer.increment(Stat.Num_TS_Verified);
			}
//			System.out.println("LINE0\t"+"eidx: "+eidx+"\thasNext: "+witer.hasNext()+"\tw: "+w+"/"+expList.get(eidx).size()+"\twidx: "+window.sidx+"/"+(expList.get(eidx).size()-1));
			if ( !witer.hasNext() ) {
				++w;
				if ( w > expList.get(eidx).size() ) {
					w = 1;
					++eidx;
				}
				if ( eidx < expList.size() ) witer = new SortedRecordSlidingWindowIterator(expList.get(eidx), w, theta);
				else witer = null;
			}
//			System.out.println("LINE1\t"+"eidx: "+eidx+"\thasNext: "+witer.hasNext()+"\tw: "+w+"/"+expList.get(eidx).size()+"\twidx: "+window.sidx+"/"+(expList.get(eidx).size()-1));
			if ( witer != null ) window = witer.next();
			return sim;
		}
	}

	@Override
	public String getName() {
		return "NaiveValidator";
	}
}
