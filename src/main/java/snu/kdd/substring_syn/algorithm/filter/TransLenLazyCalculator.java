package snu.kdd.substring_syn.algorithm.filter;

import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.StatContainer;

public final class TransLenLazyCalculator {

	private final StatContainer statContainer;
	private final Subrecord subrec;
	private final int sidx;
	private final double theta;
	private final int[] ub;
	private final int[] lb;

	private int eidxCurr; // ub[eidx], lb[eidx] are computed for all eidx <= eidxCurr


	public TransLenLazyCalculator( StatContainer statContainer, Record rec, int sidx, int maxlen, double theta ) {
		// sidx: inclusive
		this.statContainer = statContainer;
//		this.subrec = initRecord(rec, sidx, maxlen);
		this.subrec = new Subrecord(rec, sidx, sidx+maxlen);
		this.sidx = sidx;
		this.theta = theta;
		ub = new int[maxlen];	
		lb = new int[maxlen];

		this.eidxCurr = sidx-1;
	}
	
//	private Record initRecord(Record rec, int sidx, int maxlen) {
//		Record subrec = rec.getSubrecord(sidx, Math.min(sidx+maxlen, rec.size()));
//		statContainer.startWatch("TransLenLazyCalculator.Time_preprocessApplicableRules");
//		subrec.preprocessApplicableRules();
//		statContainer.stopWatch("TransLenLazyCalculator.Time_preprocessApplicableRules");
//		statContainer.startWatch("TransLenLazyCalculator.Time_preprocessSuffixApplicableRules");
//		subrec.preprocessSuffixApplicableRules();
//		statContainer.stopWatch("TransLenLazyCalculator.Time_preprocessSuffixApplicableRules");
//		return subrec;
//	}

	public int getLB( int eidx ) {
		// eidx: inclusive
		check(eidx);
		if ( eidx > eidxCurr ) computeTransLen(eidx);
		return lb[eidx-sidx];
	}

	public int getUB( int eidx ) {
		// eidx: inclusive
		check(eidx);
		if ( eidx > eidxCurr ) computeTransLen(eidx);
		return ub[eidx-sidx];
	}
	
	private void check( int eidx ) {
		// eidx: inclusive
		if ( eidx-sidx >= subrec.size() ) {
			Exception e = new Exception("Invalid input: subrec.size="+subrec.size()+", sidx="+sidx+", but eidx="+eidx);
		}
	}
	
	public int getLFLB( int eidx ) {
		// eidx: inclusive
		return (int)Math.ceil(1.0*getLB(eidx)*theta);
	}
	
	public int getLFUB( int eidx ) {
		// eidx: inclusive
		return (int)(1.0*getUB(eidx)/theta);
	}
	
	protected void computeTransLen(int eidx) {
		// eidx: inclusive
		if ( eidx <= eidxCurr ) return;
		if (statContainer != null) statContainer.startWatch("TransLenLazyCalculator.Time_ComputeTransLen");
		while ( eidxCurr < eidx ) {
			eidxCurr += 1;
			int j = eidxCurr - sidx;
			ub[j] = lb[j] = j+1;
			for ( Rule rule : subrec.getSuffixApplicableRules(j) ) {
				int l = rule.lhsSize();
				int r = rule.rhsSize();
				if ( j+1 < l ) continue;
				if ( l <= r ) ub[j] = Math.max(ub[j], r+(j-l>=0? ub[j-l]: 0));
				if ( l >= r ) lb[j] = Math.min(lb[j], r+(j-l>=0? lb[j-l]: 0));
			}
		}
		if (statContainer != null) statContainer.stopWatch("TransLenLazyCalculator.Time_ComputeTransLen");
	}
}
