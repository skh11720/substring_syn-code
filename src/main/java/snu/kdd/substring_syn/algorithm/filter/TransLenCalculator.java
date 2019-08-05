package snu.kdd.substring_syn.algorithm.filter;

import java.util.Comparator;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.utils.StatContainer;

public class TransLenCalculator {
	private final StatContainer statContainer;
	private final RecordInterface rec;
	private final double theta;
	private final int[][] ub;
	private final int[][] lb;

	Comparator<Int2DoubleMap.Entry> comp = new Comparator<Int2DoubleMap.Entry>() {
		@Override
		public int compare(Int2DoubleMap.Entry o1, Int2DoubleMap.Entry o2) {
			if ( o1.getDoubleValue() > o2.getDoubleValue() ) return -1;
			else if ( o1.getDoubleValue() < o2.getDoubleValue() ) return 1;
			else return 0;
		}
	};

	public TransLenCalculator( StatContainer statContainer, RecordInterface rec, double theta ) {
		this.statContainer = statContainer;
		this.rec = rec;
		this.theta = theta;
		ub = new int[rec.size()][rec.size()];
		lb = new int[rec.size()][rec.size()];

		computeTransLen();
	}

	public int getLB( int i, int j ) {
		// both inclusive
		return lb[i][j];
	}

	public int getUB( int i, int j ) {
		// both inclusive
		return ub[i][j];
	}
	
	public int getLFLB( int i, int j ) {
		// both inclusive
		return (int)Math.ceil(1.0*lb[i][j]*theta);
	}
	
	public int getLFUB( int i, int j ) {
		// both inclusive
		return (int)(1.0*ub[i][j]/theta);
	}
	
	protected void computeTransLen() {
		if ( statContainer != null ) statContainer.startWatch("Time_ComputeTransLen");
		for ( int i=0; i<rec.size(); ++i ) {
			for ( int j=i; j<rec.size(); ++j ) {
				ub[i][j] = lb[i][j] = j-i+1;
				for ( Rule rule : rec.getSuffixApplicableRules(j) ) {
					int l = rule.lhsSize();
					int r = rule.rhsSize();
					if ( j-i+1 < l ) continue;
					if ( l < r ) ub[i][j] = Math.max(ub[i][j], r+(j-l>=0? ub[i][j-l]: 0));
					if (l > r )	lb[i][j] = Math.min(lb[i][j], r+(j-l>=0? lb[i][j-l]: 0));
				}
			}
		}
		if ( statContainer != null ) statContainer.stopWatch("Time_ComputeTransLen");
	}
}