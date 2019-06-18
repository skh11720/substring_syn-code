package snu.kdd.substring_syn.algorithm.filter;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Rule;

public class TransSetBoundCalculator {
	private final Record rec;
	private final double theta;
	private final Int2DoubleOpenHashMap[] counterArr;
	private final int[][] transLen;
	
	private Iterator<Integer> iterUB = null;
	private Iterator<Integer> iterLB = null;
	
	public TransSetBoundCalculator( Record rec, double theta ) {
		this.rec = rec;
		this.theta = theta;
		counterArr = new Int2DoubleOpenHashMap[rec.size()];
		for ( int i=0; i<rec.size(); ++i ) {
			counterArr[i] = new Int2DoubleOpenHashMap();
			for ( Rule rule : rec.getIncompatibleRules(i) ) {
				for ( int token : rule.getRhs() ) counterArr[i].addTo(token, 1.0/rule.rhsSize());
			}
		}
		transLen = new int[rec.size()+1][2];
	}
	
	public void setStart( int sidx ) {
		computeTransLenFrom(sidx);
		iterLB = new Iterator<Integer>() {
			Int2DoubleOpenHashMap counter = new Int2DoubleOpenHashMap();
			int idx = sidx;

			Comparator<Int2DoubleMap.Entry> comp = new Comparator<Int2DoubleMap.Entry>() {
				@Override
				public int compare(Int2DoubleMap.Entry o1, Int2DoubleMap.Entry o2) {
					if ( o1.getDoubleValue() > o2.getDoubleValue() ) return -1;
					else if ( o1.getDoubleValue() < o2.getDoubleValue() ) return 1;
					else return 0;
				}
			};

			@Override
			public boolean hasNext() {
				return idx < rec.size();
			}

			@Override
			public Integer next() {
				addCounter(counter, counterArr[idx]);
				int lb = computeLowerBound();
				++idx;
				return lb;
			}

			private int computeLowerBound() {
				int lb = 0;
				double len = 0;
				// TODO: sorting could be time-consuming (need check). more efficient way?
				Iterator<Int2DoubleMap.Entry> tokenCountIter = counter.int2DoubleEntrySet().stream().sorted(comp).iterator();
				while ( tokenCountIter.hasNext() && len < transLen[idx+1][0] ) {
					++lb;
					len += tokenCountIter.next().getDoubleValue();
				}
				return Math.max(1, lb);
			}
		};
		
		iterUB = new Iterator<Integer>() {
			int idx = sidx;
			
			@Override
			public boolean hasNext() {
				return idx < rec.size();
			}
			
			@Override
			public Integer next() {
				if ( ! hasNext() ) throw new NoSuchElementException("idx: "+idx+", rec.size(): "+rec.size()+", transLen.length: "+transLen.length);
//				System.out.println("idx: "+idx+", rec.size(): "+rec.size()+", transLen.length: "+transLen.length);
				int ub = transLen[idx+1][1];
				++idx;
				return ub;
			}
		};
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

	public int getNextUB() {
		return (int)(1.0*iterUB.next()/theta);
	}
	
	public int getNextLB() {
		return (int)Math.ceil(1.0*iterLB.next()*theta);
	}
}