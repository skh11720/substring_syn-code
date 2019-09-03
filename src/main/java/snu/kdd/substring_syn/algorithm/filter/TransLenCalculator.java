package snu.kdd.substring_syn.algorithm.filter;

import java.util.Arrays;

import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.StatContainer;

public class TransLenCalculator {
	private final StatContainer statContainer;
	private final Record rec;
	private final int sidx;
	private final double theta;
	private final int[][] ub;
	private final int[][] lb;


	public TransLenCalculator( StatContainer statContainer, Record rec, double theta ) {
		this(statContainer, rec, 0, rec.size()-1, theta);
	}
		
		
	public TransLenCalculator( StatContainer statContainer, Record rec, int sidx, int eidx, double theta ) {
		// both sidx and eidx are inclusive
		this.statContainer = statContainer;
		this.rec = rec;
		this.sidx = sidx;
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
		for ( int i=0; i<ub.length; ++i ) {
			for ( int j=i; j<ub.length; ++j ) {
				ub[i][j] = lb[i][j] = j-i+1;
//				for ( Rule rule : rec.getSuffixApplicableRules(j+sidx) ) {
				for ( IntPair pair : rec.getSuffixRuleLens(j+sidx) ) {
					int l = pair.i1;
					int r = pair.i2;
					if ( j-i+1 < l ) continue;
					if ( l <= r ) ub[i][j] = Math.max(ub[i][j], r+(j-l>=0? ub[i][j-l]: 0));
					if ( l >= r ) lb[i][j] = Math.min(lb[i][j], r+(j-l>=0? lb[i][j-l]: 0));
				}
			}
		}
		if ( statContainer != null ) statContainer.stopWatch("Time_ComputeTransLen");
	}
	
	public final String toStringUB() {
		StringBuilder strbld = new StringBuilder();
		for ( int i=0; i< ub.length; ++i ) strbld.append("\n"+Arrays.toString(ub[i]));
		return strbld.toString();
	}

	public final String toStringLB() {
		StringBuilder strbld = new StringBuilder();
		for ( int i=0; i< lb.length; ++i ) strbld.append("\n"+Arrays.toString(lb[i]));
		return strbld.toString();
	}
}