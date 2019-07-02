package snu.kdd.substring_syn.algorithm.filter;

import java.util.Comparator;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.utils.Double2IntSetList;
import snu.kdd.substring_syn.utils.StatContainer;

public class TransSetBoundCalculator implements TransSetBoundCalculatorInterface {
	/*
	 * TransSetBoundCalculator1: 
	 * 		Compute the bounds incrementally. it does not keep the computation result.
	 * TransSetBoundCalculator2:
	 * 		Use O(|s|^2) space to keep the computed bounds for all substrings.
	 *      Sort tokens by their counts each time (using Stream.sorted).
	 *      It is verified that the sorting is the main bottle-neck.
	 * TransSetBoundCalculator3:
	 * 		Use O(|s|^2) space to keep the computed bounds for all substrings.
	 * 	    Keep a sorted list of pairs of token and its score.
	 * 		When the window is extended and the scores are updated,
	 * 		sort the updated entries only in insertion sort-style.
	 * TransSetboundCalculator5:
	 * 		Use O(|s|^2) space to keep the computed bounds for all substrings.
	 */
	private static final double eps = 1e-10;
	private final StatContainer statContainer;
	private final Record rec;
	private final double theta;
	private final Int2DoubleOpenHashMap[] counterArr;
	private final int[][] transLen;
	private final int[][] ub;
	private final int[][] lb;
	private final int[][] lbMono;

	Comparator<Int2DoubleMap.Entry> comp = new Comparator<Int2DoubleMap.Entry>() {
		@Override
		public int compare(Int2DoubleMap.Entry o1, Int2DoubleMap.Entry o2) {
			if ( o1.getDoubleValue() > o2.getDoubleValue() ) return -1;
			else if ( o1.getDoubleValue() < o2.getDoubleValue() ) return 1;
			else return 0;
		}
	};

	public TransSetBoundCalculator( StatContainer statContainer, Record rec, double theta ) {
		this.statContainer = statContainer;
		this.rec = rec;
		this.theta = theta;
		counterArr = new Int2DoubleOpenHashMap[rec.size()];
		transLen = new int[rec.size()+1][2];
		ub = new int[rec.size()][rec.size()];
		lb = new int[rec.size()][rec.size()];
		lbMono = new int[rec.size()][rec.size()];

		buildCounterArr();
		computeTransLenAndBounds();
		computeLBMono();
	}

	public int getLB( int i, int j ) {
		return lb[i][j];
	}

	public int getLBMono( int i, int j ) {
		return lbMono[i][j];
	}

	public int getUB( int i, int j ) {
		return ub[i][j];
	}
	
	public int getLFLB( int i, int j ) {
		return (int)Math.ceil(1.0*lb[i][j]*theta);
	}
	
	public int getLFLBMono( int i, int j ) {
		return (int)Math.ceil(1.0*lbMono[i][j]*theta);
	}

	public int getLFUB( int i, int j ) {
		return (int)(1.0*ub[i][j]/theta);
	}

	protected void buildCounterArr() {
		if ( statContainer != null ) statContainer.startWatch("Time_BuildCounterArr");
		for ( int i=0; i<rec.size(); ++i ) {
			counterArr[i] = new Int2DoubleOpenHashMap();
			for ( Rule rule : rec.getIncompatibleRules(i) ) {
				for ( int token : rule.getRhs() ) counterArr[i].addTo(token, 1.0/rule.rhsSize());
			}
		}
		if ( statContainer != null ) statContainer.stopWatch("Time_BuildCounterArr");
	}
	
	protected void computeTransLenAndBounds() {
		for ( int i=0; i<rec.size(); ++i ) {
			if ( statContainer != null ) statContainer.startWatch("Time_ComputeTransLenFrom");
			computeTransLenFrom(i);
			if ( statContainer != null ) statContainer.stopWatch("Time_ComputeTransLenFrom");
			if ( statContainer != null ) statContainer.startWatch("Time_ComputeBounds");
			computeBounds(i);
			if ( statContainer != null ) statContainer.stopWatch("Time_ComputeBounds");
		}
	}

	private void computeTransLenFrom( int sidx ) {
		transLen[sidx][0] = transLen[sidx][1] = 0;
		for ( int i=sidx+1; i<=rec.size(); ++i ) {
			transLen[i][0] = Math.min( i-sidx, transLen[i-1][0]+1 );
			transLen[i][1] = Math.max( i-sidx, transLen[i-1][1]+1 );
			for ( Rule rule : rec.getSuffixApplicableRules(i-1) ) {
				int l = rule.lhsSize();
				int r = rule.rhsSize();
				if ( i-sidx < l ) continue;
				if ( l < r ) transLen[i][1] = Math.max( transLen[i][1], transLen[i-l][1]+r );
				if (l > r )	transLen[i][0] = Math.min( transLen[i][0], transLen[i-l][0]+r );
			}
		}
	}
	
	private void computeBounds( int i ) {
		Double2IntSetList list = new Double2IntSetList();
		for ( int j=i; j<rec.size(); ++j ) {
			if ( statContainer != null ) statContainer.startWatch("Time_UpdateIntDoubleList");
			for ( Int2DoubleOpenHashMap.Entry entry : counterArr[j].int2DoubleEntrySet() ) {
				list.update( entry.getIntKey(), entry.getDoubleValue()+eps );
			}
			if ( statContainer != null ) statContainer.stopWatch("Time_UpdateIntDoubleList");
			if ( statContainer != null ) statContainer.startWatch("Time_ComputeLowerBound");
			lb[i][j] = computeLowerBound(j, list);
//			System.out.println(i+", "+j+", lb: "+lb[i][j]+", "+list.toString());
			if ( statContainer != null ) statContainer.stopWatch("Time_ComputeLowerBound");
			ub[i][j] = transLen[j+1][1];
		}
	}
	
	private int computeLowerBound( int j, Double2IntSetList list ) {
		int minLen = transLen[j+1][0];
		int lb = 0;
		double len = 0;
		for ( int i=0; i<list.length(); ++i ) {
			double v = list.getValue(i);
			IntSet set = list.getIntSet(i);
			if ( len + v*set.size() < minLen ) {
				len += v*set.size();
				lb += set.size();
			}
			else {
				int n = (int)Math.ceil((minLen-len)/v);
				len += v*n;
				lb += n;
				break;
			}
		}
		return Math.max(1, lb);
	}
	
	private void computeLBMono() {
		for ( int i=0; i<rec.size(); ++i ) {
			lbMono[i][rec.size()-1] = lb[i][rec.size()-1];
			for ( int j=rec.size()-2; j>=i; --j ) {
				lbMono[i][j] = Math.min(lbMono[i][j+1], lb[i][j]);
			}
		}
	}
}