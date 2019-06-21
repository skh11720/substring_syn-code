package snu.kdd.substring_syn.algorithm.filter;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import snu.kdd.substring_syn.algorithm.search.AbstractSearch;
import snu.kdd.substring_syn.data.IntDouble;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.utils.StatContainer;

public class TransSetBoundCalculator2 {
	private final StatContainer statContainer;
	private final Record rec;
	private final double theta;
	private final Int2DoubleOpenHashMap[] counterArr;
	private final int[][] transLen;
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
	
	public TransSetBoundCalculator2( AbstractSearch alg, Record rec, double theta ) {
		this.statContainer = alg.getStatContainer();
		this.rec = rec;
		this.theta = theta;
		statContainer.startWatch("Time_BuildCounterArr");
		counterArr = new Int2DoubleOpenHashMap[rec.size()];
		for ( int i=0; i<rec.size(); ++i ) {
			counterArr[i] = new Int2DoubleOpenHashMap();
			for ( Rule rule : rec.getIncompatibleRules(i) ) {
				for ( int token : rule.getRhs() ) counterArr[i].addTo(token, 1.0/rule.rhsSize());
			}
		}
		statContainer.stopWatch("Time_BuildCounterArr");

		transLen = new int[rec.size()+1][2];
		ub = new int[rec.size()][rec.size()];
		lb = new int[rec.size()][rec.size()];
		
		for ( int i=0; i<rec.size(); ++i ) {
			statContainer.startWatch("Time_ComputeTransLenFrom");
			computeTransLenFrom(i);
			statContainer.stopWatch("Time_ComputeTransLenFrom");
			statContainer.startWatch("Time_ComputeBounds");
			computeBounds(i);
			statContainer.stopWatch("Time_ComputeBounds");
		}
	}
	
	public int getLB( int i, int j ) {
		return lb[i][j];
	}

	public int getUB( int i, int j ) {
		return ub[i][j];
	}

	private void computeBounds( int i ) {
		Int2DoubleOpenHashMap counter = new Int2DoubleOpenHashMap();
		for ( int j=i; j<rec.size(); ++j ) {
			statContainer.startWatch("Time_AddCounter");
			addCounter(counter, counterArr[j]);
			statContainer.stopWatch("Time_AddCounter");
			statContainer.startWatch("Time_ComputeLowerBound");
			lb[i][j] = (int)Math.ceil(1.0*computeLowerBound(j, counter)*theta);
			statContainer.stopWatch("Time_ComputeLowerBound");
			ub[i][j] = (int)(1.0*transLen[j+1][1]/theta);
		}
	}
	
	private void addCounter( Int2DoubleOpenHashMap thisCounter, Int2DoubleOpenHashMap otherCounter ) {
		for ( Int2DoubleOpenHashMap.Entry entry : otherCounter.int2DoubleEntrySet() ) {
			thisCounter.addTo(entry.getIntKey(), entry.getDoubleValue());
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
	
	private int computeLowerBound( int j, Int2DoubleOpenHashMap counter ) {
		int lb = 0;
		double len = 0;
		// TODO: sorting could be time-consuming (need check). more efficient way?
		Iterator<Int2DoubleMap.Entry> tokenCountIter = counter.int2DoubleEntrySet().stream().sorted(comp).iterator();
		while ( tokenCountIter.hasNext() && len < transLen[j+1][0] ) {
			++lb;
			len += tokenCountIter.next().getDoubleValue();
		}
		return Math.max(1, lb);
	}
}