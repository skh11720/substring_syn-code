package snu.kdd.substring_syn.algorithm.validator;

import java.util.Iterator;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;

public class NaiveWindowBasedValidator extends NaiveValidator {
	
	public NaiveWindowBasedValidator( double theta, StatContainer statContainer ) {
		super(theta, statContainer);
	}

	@Override
	protected AbstractTextSideIterator getTextSideIterator(Record query, Record rec) {
		return new TextSideIterator(query, rec);
	}

	protected class TextSideIterator extends AbstractTextSideIterator {
		
		Iterator<Subrecord> wIter;
		Record w;
		Record thisExp;
		
		public TextSideIterator( Record query, Record rec ) {
			super(query);
			wIter = Records.getSubrecords(rec).iterator();
			thisExp = findNext();
		}

		@Override
		public boolean hasNext() {
			return thisExp != null;
		}

		@Override
		public Double next() {
			Record exp = thisExp;
			thisExp = findNext();
			double sim = Util.jaccardM(query.getTokenArray(), exp.getTokenArray());
			if ( statContainer != null ) {
				statContainer.addCount(Stat.Len_TS_Verified, exp.size());
				statContainer.increment(Stat.Num_TS_Verified);
			}
//			System.out.println("LINE0\t"+"eidx: "+eidx+"\thasNext: "+witer.hasNext()+"\tw: "+w+"/"+expList.get(eidx).size()+"\twidx: "+window.sidx+"/"+(expList.get(eidx).size()-1));
//			System.out.println("LINE1\t"+"eidx: "+eidx+"\thasNext: "+witer.hasNext()+"\tw: "+w+"/"+expList.get(eidx).size()+"\twidx: "+window.sidx+"/"+(expList.get(eidx).size()-1));
			return sim;
		}
		
		private Record findNext() {
			while ( expIter == null || !expIter.hasNext() ) {
				if ( wIter == null || !wIter.hasNext() ) return null;
				else w = wIter.next().toRecord();
				w.preprocessAll();
				expIter = Records.expands(w).iterator();
			}
			return expIter.next();
		}
	}
	
	@Override
	public String getName() {
		return "NaiveWindowBasedValidator";
	}
}
