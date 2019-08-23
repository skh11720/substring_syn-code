package snu.kdd.substring_syn.algorithm.filter;

import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.utils.StatContainer;

public class TransLenCalculator {
	private final StatContainer statContainer;
	private final RecordInterface rec;
	private final int sidx;
	private final int eidx;
	private final double theta;
	private final int[][] ub;
	private final int[][] lb;


	public TransLenCalculator( StatContainer statContainer, RecordInterface rec, double theta ) {
		this(statContainer, rec, 0, rec.size(), theta);
	}
		
		
	public TransLenCalculator( StatContainer statContainer, RecordInterface rec, int sidx, int eidx, double theta ) {
		this.statContainer = statContainer;
		this.rec = rec;
		this.sidx = sidx; // left inclusive
		this.eidx = eidx; // right inclusive
		this.theta = theta;
		int len = eidx - sidx + 1;
		ub = new int[len][len];	
		lb = new int[len][len];

		computeTransLen();
	}

	public int getLB( int i, int j ) {
		// both inclusive
		return lb[i-sidx][j-sidx];
	}

	public int getUB( int i, int j ) {
		// both inclusive
		return ub[i-sidx][j-sidx];
	}
	
	public int getLFLB( int i, int j ) {
		// both inclusive
		return (int)Math.ceil(1.0*lb[i-sidx][j-sidx]*theta);
	}
	
	public int getLFUB( int i, int j ) {
		// both inclusive
		return (int)(1.0*ub[i-sidx][j-sidx]/theta);
	}
	
	protected void computeTransLen() {
		if ( statContainer != null ) statContainer.startWatch("Time_ComputeTransLen");
		for ( int i=sidx; i<eidx+1; ++i ) {
			for ( int j=i; j<eidx+1; ++j ) {
				ub[i-sidx][j-sidx] = lb[i-sidx][j-sidx] = j-i+1;
				for ( Rule rule : rec.getSuffixApplicableRules(j) ) {
					int l = rule.lhsSize();
					int r = rule.rhsSize();
					if ( j-i+1 < l ) continue;
					if ( l < r ) ub[i-sidx][j-sidx] = Math.max(ub[i-sidx][j-sidx], r+(j-sidx-l>=0? ub[i-sidx][j-sidx-l]: 0));
					if ( l > r ) lb[i-sidx][j-sidx] = Math.min(lb[i-sidx][j-sidx], r+(j-sidx-l>=0? lb[i-sidx][j-sidx-l]: 0));
				}
			}
		}
		if ( statContainer != null ) statContainer.stopWatch("Time_ComputeTransLen");
	}
}