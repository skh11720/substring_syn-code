package snu.kdd.etc;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Comparator;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import snu.kdd.substring_syn.algorithm.filter.TransSetBoundCalculator3;
import snu.kdd.substring_syn.algorithm.filter.TransSetBoundCalculator5;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntDouble;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.TokenOrder;
import snu.kdd.substring_syn.utils.Double2IntHashBasedBinaryHeap;
import snu.kdd.substring_syn.utils.StatContainer;
import snu.kdd.substring_syn.utils.Util;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TransSetBoundCalculatorTest {

	double[] thetaList = {0.6, 0.7, 0.8, 0.9, 1.0};
	String[] sizeList = {"100", "101", "102", "103", "104", "105"};
	
	@Test
	public void test001Correctness() throws IOException {
		for ( String size : sizeList ) {
			for ( double theta : thetaList ) {
				checkCorrectness(theta, size);
			}
		}
	}
	
	@Test
	public void test002Efficiency() throws IOException {
		for ( String size : sizeList ) {
			for ( double theta : thetaList ) {
				compareEfficiency(theta, size);
			}
		}
	}

	public void checkCorrectness( double theta, String size ) throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT_long", size);
		TokenOrder order = new TokenOrder(dataset);
		dataset.reindexByOrder(order);
		
		for ( Record rec : dataset.indexedList ) {
			TransSetBoundCalculator3 bc3 = new TransSetBoundCalculator3(null, rec, theta);
			TransSetBoundCalculator5 bc5 = new TransSetBoundCalculator5(null, rec, theta);
			for ( int i=0; i<rec.size(); ++i ) {
				for ( int j=i; j<rec.size(); ++j ) {
					try {
						assertTrue(bc3.getLB(i, j) == bc5.getLB(i, j));
					}
					catch ( AssertionError e ) {
						System.err.println(rec.getID());
						System.err.println("i: "+i);
						System.err.println("j: "+j);
						System.err.println("bc3.LB: "+bc3.getLB(i, j));
						System.err.println("bc5.LB: "+bc5.getLB(i, j));
						throw e;
					}
				}
			}
		}
	}
	
	public void compareEfficiency( double theta, String size ) throws IOException {
		StatContainer statContainer3 = new StatContainer();
		StatContainer statContainer5 = new StatContainer();

		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT_long", size);
		TokenOrder order = new TokenOrder(dataset);
		dataset.reindexByOrder(order);

		for ( Record query : dataset.searchedList ) {
			for ( Record rec : dataset.indexedList ) {
				statContainer3.startWatch("Time_TransSetBoundCalculator3");
				TransSetBoundCalculator3 bc3 = new TransSetBoundCalculator3(statContainer3, rec, theta);
				statContainer3.stopWatch("Time_TransSetBoundCalculator3");

				statContainer5.startWatch("Time_TransSetBoundCalculator5");
				TransSetBoundCalculator5 bc5 = new TransSetBoundCalculator5(statContainer5, rec, theta);
				statContainer5.stopWatch("Time_TransSetBoundCalculator5");
			}
		}
		
		statContainer3.finalize();
		statContainer5.finalize();
		String t3 = statContainer3.getStat("Time_TransSetBoundCalculator3");
		String t5 = statContainer5.getStat("Time_TransSetBoundCalculator5");
		System.out.println(String.format("%4.2f%9s\t%11s%11s", theta, size, t3, t5));
	}
	
	class TransSetBoundCalculator4 {
		private final StatContainer statContainer;
		private final Record rec;
		private final double theta;
		private final Int2DoubleOpenHashMap[] counterArr;
		private final int[][] transLen;
		private final int[][] ub;
		private final int[][] lb;
		private final int[][] lbMono;
		Double2IntHashBasedBinaryHeap minHeap;
		Double2IntHashBasedBinaryHeap maxHeap;

		Comparator<Int2DoubleMap.Entry> comp = new Comparator<Int2DoubleMap.Entry>() {
			@Override
			public int compare(Int2DoubleMap.Entry o1, Int2DoubleMap.Entry o2) {
				if ( o1.getDoubleValue() > o2.getDoubleValue() ) return -1;
				else if ( o1.getDoubleValue() < o2.getDoubleValue() ) return 1;
				else return 0;
			}
		};

		public TransSetBoundCalculator4( StatContainer statContainer, Record rec, double theta ) {
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
			Int2DoubleOpenHashMap counter = new Int2DoubleOpenHashMap();
			minHeap = new Double2IntHashBasedBinaryHeap();
			maxHeap = new Double2IntHashBasedBinaryHeap((x,y)->-Double.compare(x,y));
			double minHeapSum = 0;
			for ( int j=i; j<rec.size(); ++j ) {
				if ( statContainer != null ) statContainer.startWatch("Time_ComputeLowerBound");
				for ( Int2DoubleOpenHashMap.Entry entry : counterArr[j].int2DoubleEntrySet() ) {
					int token = entry.getIntKey();
					double diffCount = entry.getDoubleValue() + 1e-10;
					counter.addTo(token, diffCount);
					double acmCount = counter.get(token);
					if ( minHeap.containesValue(token) ) { // already in the minHeap
						minHeap.decreaseKeyOfValue(token, acmCount);
						minHeapSum += diffCount;
//						System.out.println("increase count of "+token+" by "+diffCount+", minHeapSum: "+minHeapSum);
					}
					else if ( minHeapSum < transLen[j+1][0] ) { // not in the minHeap, but there is a space
						minHeap.insert(acmCount, token);
						minHeapSum += acmCount;
//						System.out.println("insert token "+token+" of count "+acmCount+", minHeapSum: "+minHeapSum);
					}
					else {// not in the minHeap, and the minHeap is full
						// update the maxHeap.
						if ( maxHeap.containesValue(token) ) maxHeap.increaseKeyOfValue(token, acmCount);
						else maxHeap.insert(acmCount, token);

					}

					// make maxHeap.peek() <= minHeap.peek()
					while ( !maxHeap.isEmpty() && !minHeap.isEmpty() && maxHeap.peek() > minHeap.peek() ) {
						IntDouble maxHeapPeek = maxHeap.poll();
						IntDouble minHeapPeek = minHeap.poll();
						maxHeap.insert(minHeapPeek.v, minHeapPeek.k);
						minHeap.insert(maxHeapPeek.v, maxHeapPeek.k);
//						System.out.println("expand: "+minHeapSum+" -> "+(minHeapSum+maxHeapPeek.v-minHeapPeek.v)+" / "+transLen[j+1][0]);
						minHeapSum += maxHeapPeek.v - minHeapPeek.v;
					}

					// shrink heap if possible
					while ( minHeap.size() > 1 && minHeapSum - minHeap.peek() >= transLen[j+1][0] ) {
						IntDouble minHeapPeek = minHeap.poll();
						maxHeap.insert(minHeapPeek.v, minHeapPeek.k);
						minHeapSum -= minHeapPeek.v;
//						System.out.println("shrink: "+(minHeapSum+minHeapPeek.v)+" -> "+minHeapSum+" / "+transLen[j+1][0]);
					}
				}
				lb[i][j] = minHeap.size();
				if ( statContainer != null ) statContainer.stopWatch("Time_ComputeLowerBound");
				if ( i == 11 && j == 26 ) {
				}
//				System.out.println(i+", "+j+", "+minHeapSum+", "+transLen[j+1][0]+", "+minHeap.size());
//				System.out.println("minHeap: "+minHeap);
//				System.out.println("maxHeap: "+maxHeap);
//				System.out.println(counter);
				if ( minHeap.getKeySum() - minHeapSum > 1e-3 ) throw new RuntimeException("heap.getKeySum(): "+minHeap.getKeySum()+", minHeapSum: "+minHeapSum);
				ub[i][j] = transLen[j+1][1];
			}
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
}
