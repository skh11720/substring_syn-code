package snu.kdd.substring_syn.data.record;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.Rule;

public class Records {

	public static String[] tokenize( String str ) {
		return str.split( "( |\t)+" );
	}

	public static ObjectList<Record> expandAll( RecordInterface rec ) {
		ObjectList<Record> rslt = new ObjectArrayList<Record>();
		int[] tokens = rec.getTokenArray();
		expandAll( rslt, rec, 0, tokens );
		return rslt;
	}

	private static void expandAll( ObjectList<Record> rslt, RecordInterface rec, int idx, int[] t ) {

		Iterable<Rule> rules = rec.getApplicableRules(idx);

		for( Rule rule : rules ) {
			if( rule.isSelfRule ) {
				if( idx + 1 != rec.size() ) {
					expandAll( rslt, rec, idx + 1, t );
				}
				else {
					rslt.add( new Record( t ) );
				}
			}
			else {
				int newSize = t.length - rule.lhsSize() + rule.rhsSize();

				int[] new_rec = new int[ newSize ];

				int rightSize = rec.size() - idx;
				int rightMostSize = rightSize - rule.lhsSize();

				int[] rhs = rule.getRhs();

				int k = 0;
				for( int i = 0; i < t.length - rightSize; i++ ) {
					new_rec[ k++ ] = t[ i ];
				}
				for( int i = 0; i < rhs.length; i++ ) {
					new_rec[ k++ ] = rhs[ i ];
				}
				for( int i = t.length - rightMostSize; i < t.length; i++ ) {
					new_rec[ k++ ] = t[ i ];
				}

				int new_idx = idx + rule.lhsSize();
				if( new_idx == rec.size() ) {
					rslt.add( new Record( new_rec ) );
				}
				else {
					expandAll( rslt, rec, new_idx, new_rec );
				}
			}
		}
	}
	
	public static Iterable<Record> expands( Iterable<Record> records ) {
		return new Iterable<Record>() {

			@Override
			public Iterator<Record> iterator() {
				return new ExpandMultipleIterator(records);
			}
		};
	}
	
	private static class ExpandMultipleIterator implements Iterator<Record> {
		
		Iterator<Record> rIter = null;
		Record rec = null;
		ExpandIterator eIter = null;

		public ExpandMultipleIterator(Iterable<Record> records) {
			rIter = records.iterator();
			findNext();
		}

		@Override
		public Record next() {
			Record recExp = eIter.next();
			findNext();
			return recExp;
		}
		
		protected void findNext() {
			while ( eIter == null || !eIter.hasNext() ) {
				if ( !rIter.hasNext() ) {
					rIter = null;
					eIter = null;
					rec = null;
					return;
				}
				else {
					rec = rIter.next();
					rec.preprocessAll();
					eIter = new ExpandIterator(rec);
				}
			}
		}
		
		@Override
		public boolean hasNext() {
			return eIter != null && eIter.hasNext();
		}
	}
	
	public static Iterable<Record> expands( Record rec ) {
		return new Iterable<Record>() {
			
			@Override
			public Iterator<Record> iterator() {
				return new ExpandIterator(rec);
			}
		};
	}
	
	private static class ExpandIterator implements Iterator<Record> {

		final State state;
		boolean hasNext = true;
		
		public ExpandIterator( Record rec ) {
			state = new State(rec);
			findNext();
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public Record next() {
			Record exp = state.getRecord();
			findNext();
			return exp;
		}
		
		private void findNext() {
			hasNext = state.transit();
		}

		private class State {
			Record rec;
			Rule[][] rules;
			Rule[] ruleList;
			int[] ridxList;
			int[] expand;
			int nRule;
			int lhsSize;
			int rhsSize;
			
			public State( Record rec ) {
				this.rec = rec;
				rules = rec.getApplicableRules();
				ruleList = new Rule[rec.size()];
				ridxList = new int[rec.size()];
				expand = new int[rec.getMaxTransLength()];
				nRule = 0;
				lhsSize = 0;
				rhsSize = 0;
			}
			
			public boolean transit() {
				while ( lhsSize >= rec.size() || ridxList[lhsSize] >= rules[lhsSize].length ) {
					if ( lhsSize < rec.size() ) {
						ridxList[lhsSize] = 0;
					}
					if ( nRule > 0 ) removeRule();
					else return false;
				}
				while ( lhsSize < rec.size() ) {
					int ridx = ridxList[lhsSize];
					if ( ridx < rules[lhsSize].length ) {
						Rule r = rules[lhsSize][ridx];
						ridxList[lhsSize] += 1;
						if ( lhsSize + r.lhsSize() <= rec.size() ) addRule(r);
					}
				}
				return true;
			}
			
			public void addRule( Rule r ) {
				ruleList[nRule] = r;
				nRule += 1;
				for ( int i=0; i<r.rhsSize(); ++i  ) expand[i+rhsSize] = r.getRhs()[i];
				lhsSize += r.lhsSize();
				rhsSize += r.rhsSize();
			}
			
			public void removeRule() {
				nRule -= 1;
				Rule r = ruleList[nRule];
				lhsSize -= r.lhsSize();
				rhsSize -= r.rhsSize();
			}
			
			public Record getRecord() {
				return new Record(rec.getID(), IntArrayList.wrap(expand).subList(0, rhsSize).toIntArray());
			}
			
			public String getExpandString() {
				if (nRule == 0 ) return "";
				else return getRecord().toOriginalString();
			}
			
			@Override
			public String toString() {
				return String.format("%s, %d, %d, %d, %s", Arrays.toString(ridxList), nRule, lhsSize, rhsSize, getExpandString());
			}
		}
	}


	static int getTransSetSizeLowerBound( Record rec ) {
		Iterator<Int2DoubleMap.Entry> tokenCountIter = getTokenCountUpperBoundIterator(rec);
		return computeLowerBound(rec, tokenCountIter);
	}

	private static Iterator<Int2DoubleMap.Entry> getTokenCountUpperBoundIterator( Record rec ) {
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

	private static void aggregateByMax( Int2DoubleMap counter, Int2DoubleMap other ) {
		for ( it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry entry : other.int2DoubleEntrySet() ) {
			int key = entry.getIntKey();
			double value = entry.getDoubleValue();
			if ( counter.containsKey(key) ) counter.put(key, Math.max(counter.get(key), value));
			else counter.put(key, value);
		}
	}

	private static void aggregateBySum( Int2DoubleMap counter, Int2DoubleMap other ) {
		for ( it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry entry : other.int2DoubleEntrySet() ) {
			int key = entry.getIntKey();
			double value = entry.getDoubleValue();
			if ( counter.containsKey(key) ) counter.put(key, counter.get(key)+value);
			else counter.put(key, value);
		}
	}
	
	private static int computeLowerBound( Record rec, Iterator<Int2DoubleMap.Entry> tokenCountIter ) {
		int lb = 0;
		double len = 0;
		while ( tokenCountIter.hasNext() && len < rec.getMinTransLength() ) {
			++lb;
			len += tokenCountIter.next().getDoubleValue();
		}
		return Math.max(1, lb);
	}

	public static int compare( int[] str1, int[] str2 ) {
		if( str1.length == 0 || str2.length == 0 ) {
			return str1.length - str2.length;
		}

		int idx = 0;
		int lastcmp = 0;

		while( idx < str1.length && idx < str2.length && ( lastcmp = Integer.compare( str1[ idx ], str2[ idx ] ) ) == 0 ) {
			++idx;
		}

		if( lastcmp != 0 ) {
			return lastcmp;
		}
		else if( str1.length == str2.length ) {
			return 0;
		}
		else if( idx == str1.length ) {
			return -1;
		}
		else {
			return 1;
		}
	}
}
