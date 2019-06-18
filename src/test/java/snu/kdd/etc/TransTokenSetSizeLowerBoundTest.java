package snu.kdd.etc;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.utils.Util;

public class TransTokenSetSizeLowerBoundTest {

	@Test
	public void testTokenCountUpperBoundIterator() throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT", "1000");
		Random rn = new Random();
		for ( int i=0; i<10; ++i ) {
			Record rec = dataset.indexedList.get(rn.nextInt(dataset.indexedList.size()));
			System.out.println(rec.toStringDetails());
			Iterator<Int2DoubleMap.Entry> tokenCountIter = getTokenCountUpperBoundIterator3(rec);
			while (tokenCountIter.hasNext()) {
				Int2DoubleMap.Entry entry = tokenCountIter.next();
				System.out.println(entry.getIntKey()+" : "+entry.getDoubleValue());
			}
		}
	}
	
	@Test
	public void testTransTokenSetSizeLowerBound() throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT", "1000");
		
		for ( int option=1; option<=3; ++option ) {
			long t = 0;
			int[] diffArr = new int[dataset.searchedList.size()];
			double[] relDiffArr = new double[dataset.searchedList.size()];
			for ( int i=0; i<dataset.searchedList.size(); ++i ) {
				Record rec = dataset.searchedList.get(i);
				int minTransSetSize = rec.getDistinctTokenCount();
				for ( Record exp : rec.expandAll() )  minTransSetSize = Math.min(minTransSetSize, exp.getDistinctTokenCount());
				long ts = System.nanoTime();
				int lb = getTransSetSizeLowerBound(rec, option);
				t += System.nanoTime() - ts;
	//			System.out.println(rec.getID()+"\t"+minTransSetSize+"\t"+lb);
				assertTrue(minTransSetSize >= lb);
				diffArr[i] = minTransSetSize - lb;
				relDiffArr[i] = 1.0*(minTransSetSize - lb)/minTransSetSize;
				
//				if ( option == 3 && diffArr[i] == 10 ) {
//					System.out.println(rec.toStringDetails());
//					Iterator<Int2DoubleMap.Entry> iter = getTokenCountUpperBoundIterator2(rec);
//					while ( iter.hasNext() ) {
//						Int2DoubleMap.Entry entry = iter.next();
//						System.out.println(entry.getIntKey()+" : "+entry.getDoubleValue());
//					}
//					System.out.println("minTransLen: "+rec.getMinTransLength());
//					System.out.println("minTransSetSize: "+minTransSetSize);
//					System.out.println("transSetSizeLowerBound: "+lb);
//				}
			}
			System.out.println("option: "+option);
			System.out.println("execution time: "+t/1e6);
			System.out.println("diff : "+ 
					Arrays.stream(diffArr).asDoubleStream().min().getAsDouble()+" / "+
					Arrays.stream(diffArr).asDoubleStream().average().getAsDouble()+" / "+
					Arrays.stream(diffArr).asDoubleStream().max().getAsDouble());
			System.out.println("relDiff : "+
					Arrays.stream(relDiffArr).min().getAsDouble()+" / "+
					Arrays.stream(relDiffArr).average().getAsDouble()+" / "+
					Arrays.stream(relDiffArr).max().getAsDouble());
		}
	}
	
	public static int getTransSetSizeLowerBound( Record rec, int option ) {
		Iterator<Int2DoubleMap.Entry> tokenCountIter = null;
		if ( option == 1 ) tokenCountIter = getTokenCountUpperBoundIterator1(rec);
		else if ( option == 2 ) tokenCountIter = getTokenCountUpperBoundIterator2(rec);
		else if ( option == 3 ) tokenCountIter = getTokenCountUpperBoundIterator3(rec);
		/*
		 * option == 1: aggregates counts of tokens by sum at each position
		 * option == 2: aggregates counts of tokens by max at each position
		 * option == 3: aggregates "modified" counts of tokens by max at each position
		 */

		int lb = 0;
		int len = 0;
		while ( tokenCountIter.hasNext() && len < rec.getMinTransLength() ) {
			++lb;
			len += tokenCountIter.next().getDoubleValue();
		}
		return Math.max(1, lb);
	}

	public static Iterator<Int2DoubleMap.Entry> getTokenCountUpperBoundIterator1( Record rec ) {
		Comparator<Int2DoubleMap.Entry> comp = new Comparator<Int2DoubleMap.Entry>() {
			@Override
			public int compare(Int2DoubleMap.Entry o1, Int2DoubleMap.Entry o2) {
				if ( o1.getDoubleValue() > o2.getDoubleValue() ) return -1;
				else if ( o1.getDoubleValue() < o2.getDoubleValue() ) return 1;
				else return 0;
			}
		};
		
		Int2DoubleOpenHashMap counter = new Int2DoubleOpenHashMap();
		for ( int i=0; i<rec.size(); ++i ) {
			for ( Rule rule : rec.getApplicableRuleIterable() ) {
				for ( int token : rule.getRhs() ) counter.addTo(token, rule.lhsSize());
			}
		}
		Iterator<Int2DoubleMap.Entry> tokenCountIter = counter.int2DoubleEntrySet().stream().sorted(comp).iterator();
		return tokenCountIter;
	}
	
	public static Iterator<Int2DoubleMap.Entry> getTokenCountUpperBoundIterator2( Record rec ) {
		Comparator<Int2DoubleMap.Entry> comp = new Comparator<Int2DoubleMap.Entry>() {
			@Override
			public int compare(Int2DoubleMap.Entry o1, Int2DoubleMap.Entry o2) {
				if ( o1.getDoubleValue() > o2.getDoubleValue() ) return -1;
				else if ( o1.getDoubleValue() < o2.getDoubleValue() ) return 1;
				else return 0;
			}
		};

		Int2DoubleOpenHashMap counter = new Int2DoubleOpenHashMap();
		Object2ObjectOpenHashMap<Rule, Int2DoubleMap> counterByRule = new Object2ObjectOpenHashMap<>();
		for ( Rule rule : rec.getApplicableRuleIterable() ) {
			Int2DoubleOpenHashMap counterOfRule = new Int2DoubleOpenHashMap();
			for ( int token : rule.getRhs() ) counterOfRule.addTo(token, 1);
			counterByRule.put(rule, counterOfRule);
		}
		for ( int i=0; i<rec.size(); ++i ) {
			Int2DoubleOpenHashMap counterByPos = new Int2DoubleOpenHashMap();
			for ( Rule rule : rec.getIncompatibleRules(i) ) {
				aggregateByMax(counterByPos, counterByRule.get(rule));
			}
			aggregateBySum(counter, counterByPos);
		}
		Iterator<Int2DoubleMap.Entry> tokenCountIter = counter.int2DoubleEntrySet().stream().sorted(comp).iterator();
		return tokenCountIter;
	}
	
	public static void aggregateByMax( Int2DoubleMap counter, Int2DoubleMap other ) {
		for ( it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry entry : other.int2DoubleEntrySet() ) {
			int key = entry.getIntKey();
			double value = entry.getDoubleValue();
			if ( counter.containsKey(key) ) counter.put(key, Math.max(counter.get(key), value));
			else counter.put(key, value);
		}
	}

	public static void aggregateBySum( Int2DoubleMap counter, Int2DoubleMap other ) {
		for ( it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry entry : other.int2DoubleEntrySet() ) {
			int key = entry.getIntKey();
			double value = entry.getDoubleValue();
			if ( counter.containsKey(key) ) counter.put(key, counter.get(key)+value);
			else counter.put(key, value);
		}
	}

	public static Iterator<Int2DoubleMap.Entry> getTokenCountUpperBoundIterator3( Record rec ) {
		Comparator<Int2DoubleMap.Entry> comp = new Comparator<Int2DoubleMap.Entry>() {
			@Override
			public int compare(Int2DoubleMap.Entry o1, Int2DoubleMap.Entry o2) {
				if ( o1.getDoubleValue() > o2.getDoubleValue() ) return -1;
				else if ( o1.getDoubleValue() < o2.getDoubleValue() ) return 1;
				else return 0;
			}
		};
		
		Int2DoubleOpenHashMap counter = new Int2DoubleOpenHashMap();
		Object2ObjectOpenHashMap<Rule, Int2DoubleMap> counterByRule = new Object2ObjectOpenHashMap<>();
		for ( Rule rule : rec.getApplicableRuleIterable() ) {
			Int2DoubleOpenHashMap counterOfRule = new Int2DoubleOpenHashMap();
			for ( int token : rule.getRhs() ) counterOfRule.addTo(token, 1.0/rule.lhsSize());
			counterByRule.put(rule, counterOfRule);
		}
		for ( int i=0; i<rec.size(); ++i ) {
			Int2DoubleOpenHashMap counterByPos = new Int2DoubleOpenHashMap();
			for ( Rule rule : rec.getIncompatibleRules(i) ) {
				aggregateByMax(counterByPos, counterByRule.get(rule));
			}
			aggregateBySum(counter, counterByPos);
		}
		Iterator<Int2DoubleMap.Entry> tokenCountIter = counter.int2DoubleEntrySet().stream().sorted(comp).iterator();
		return tokenCountIter;
	}

	@Test
	public void testIncrementalTransSetBound() throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT_long", "1000");
		long ts = System.nanoTime();
		for ( Record rec : dataset.searchedList ) {
			if ( rec.getNumApplicableRules() < 5 ) continue;
			System.out.println(rec.toStringDetails());
			System.out.println("transLen: "+rec.getMinTransLength()+", "+rec.getMaxTransLength());
			TransSetBoundCalculator boundCal = new TransSetBoundCalculator(rec);
			for ( int sidx=0; sidx<rec.size(); ++sidx ) {
				System.out.println("sidx: "+sidx);
				boundCal.setStart(sidx);
				for ( int i=sidx; i<rec.size(); ++i ) {
					int lb = boundCal.getNextLB();
					int ub = boundCal.getNextUB();
					System.out.println("lb, ub: "+lb+", "+ub);
				}
			}
			System.in.read();
		}
		System.out.println("testIncrementalTransSetBound(): "+((System.nanoTime()-ts)/1e6)+" ms");
	}

	class TransSetBoundCalculator {
		private final Record rec;
		private final Int2DoubleOpenHashMap[] counterArr;
		private final int[][] transLen;
		
		private Iterator<Integer> iterUB = null;
		private Iterator<Integer> iterLB = null;
		
		public TransSetBoundCalculator( Record rec ) {
			this.rec = rec;
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
			return iterUB.next();
		}
		
		public int getNextLB() {
			return iterLB.next();
		}
	}
}
