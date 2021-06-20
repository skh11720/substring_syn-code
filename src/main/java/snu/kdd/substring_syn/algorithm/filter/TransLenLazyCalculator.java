package snu.kdd.substring_syn.algorithm.filter;

import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.StatContainer;

public final class TransLenLazyCalculator {

	private final StatContainer statContainer;
	private final Subrecord subrec;
	private final int sidx;
	private final double theta;
	private final int[] ub;
	private final int[] lb;

	private int eidxCurr; 

	public TransLenLazyCalculator( StatContainer statContainer, TransformableRecordInterface rec, int sidx, int maxlen, double theta ) {
		this.statContainer = statContainer;
		this.subrec = new Subrecord(rec, sidx, sidx+maxlen);
		this.sidx = sidx;
		this.theta = theta;
		ub = new int[maxlen];	
		lb = new int[maxlen];

		this.eidxCurr = sidx-1;
	}
	

	public int getLB( int eidx ) {
		check(eidx);
		if ( eidx > eidxCurr ) computeTransLen(eidx);
		return lb[eidx-sidx];
	}

	public int getUB( int eidx ) {
		check(eidx);
		if ( eidx > eidxCurr ) computeTransLen(eidx);
		return ub[eidx-sidx];
	}
	
	private void check( int eidx ) {
		if ( eidx-sidx >= subrec.size() ) {
			Exception e = new Exception("Invalid input: subrec.size="+subrec.size()+", sidx="+sidx+", but eidx="+eidx);
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public int getLFLB( int eidx ) {
		return (int)Math.ceil(1.0*getLB(eidx)*theta);
	}
	
	public int getLFUB( int eidx ) {
		return (int)(1.0*getUB(eidx)/theta);
	}
	
	protected void computeTransLen(int eidx) {
		if ( eidx <= eidxCurr ) return;
		if (statContainer != null) statContainer.startWatch("TransLenLazyCalculator.Time_ComputeTransLen");
		while ( eidxCurr < eidx ) {
			eidxCurr += 1;
			int j = eidxCurr - sidx;
			ub[j] = lb[j] = j+1;
			for ( IntPair pair : subrec.getSuffixRuleLens(j) ) {
				int l = pair.i1;
				int r = pair.i2;
				if ( j+1 < l ) continue;
				if ( l <= r ) ub[j] = Math.max(ub[j], r+(j-l>=0? ub[j-l]: 0));
				if ( l >= r ) lb[j] = Math.min(lb[j], r+(j-l>=0? lb[j-l]: 0));
			}
		}
		if (statContainer != null) statContainer.stopWatch("TransLenLazyCalculator.Time_ComputeTransLen");
	}
}
