package snu.kdd.substring_syn.algorithm.filter;

import java.util.Comparator;
import java.util.List;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.data.IntDouble;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.utils.StatContainer;

public class TransSetBoundCalculator3 {
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
	 */
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

	public TransSetBoundCalculator3( StatContainer statContainer, Record rec, double theta ) {
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
		IntDoubleList list = new IntDoubleList();
		for ( int j=i; j<rec.size(); ++j ) {
			if ( statContainer != null ) statContainer.startWatch("Time_UpdateIntDoubleList");
			for ( Int2DoubleOpenHashMap.Entry entry : counterArr[j].int2DoubleEntrySet() ) {
				list.update( entry.getIntKey(), entry.getDoubleValue() );
			}
			if ( statContainer != null ) statContainer.stopWatch("Time_UpdateIntDoubleList");
			if ( statContainer != null ) statContainer.startWatch("Time_ComputeLowerBound");
			lb[i][j] = (int)Math.ceil(1.0*computeLowerBound(j, list)*theta);
			if ( statContainer != null ) statContainer.stopWatch("Time_ComputeLowerBound");
			ub[i][j] = (int)(1.0*transLen[j+1][1]/theta);
		}
	}
	
	private int computeLowerBound( int j, IntDoubleList list ) {
		int lb = 0;
		double len = 0;
		for ( int i=0; i<list.size(); ++i ) {
			++lb;
			len += list.get(i).v;
			if ( len >= transLen[j+1][0] ) break;
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

	class IntDoubleList {
		Int2IntMap key2posMap = null;
		List<IntDouble> list = null;
		
		public IntDoubleList() {
			list = new ObjectArrayList<>();
			key2posMap = new Int2IntOpenHashMap();
		}
		
		public IntDoubleList( Int2DoubleMap counter ) {
			this();
			for ( Int2DoubleMap.Entry entry : counter.int2DoubleEntrySet() ) {
				list.add( new IntDouble(entry.getIntKey(), entry.getDoubleValue()) );
			}
			list.sort(IntDouble.comp);
			for ( int i=0; i<list.size(); ++i ) key2posMap.put(list.get(i).k, i);
		}
		
		public int size() {
			return list.size();
		}
		
		public IntDouble get( int i ) {
			return list.get(i);
		}
		
		public void update( int k, double dv ) {
			if ( key2posMap.containsKey(k) ) updateExistingKey(k, dv);
			else updateNewKey(k, dv);
		}
		
		private void updateExistingKey( int k, double dv ) {
			int i = key2posMap.get(k);
			IntDouble entry = list.get(i);
			entry.v += dv;
			updatePosition(entry, i);
		}
		
		private void updateNewKey( int k, double dv ) {
			IntDouble entry = new IntDouble(k, dv);
			list.add(entry);
			int i = list.size()-1;
			key2posMap.put(k, i);
			updatePosition(entry, i);
		}
		
		private void updatePosition( IntDouble entry, int i ) {
			while ( i > 0 ) {
				if ( list.get(i-1).v < entry.v ) swap(i-1, i);
				else break;
				--i;
			}
		}
		
		private void swap( int i, int j ) {
			key2posMap.put(list.get(i).k, j);
			key2posMap.put(list.get(j).k, i);
			IntDouble tmp = list.get(i);
			list.set(i, list.get(j));
			list.set(j, tmp);
		}
		
		public String toString() {
			StringBuilder strbld = new StringBuilder();
			for ( IntDouble entry : list ) strbld.append(key2posMap.get(entry.k)+":"+entry+", ");
			return strbld.toString();
		}
	}
}