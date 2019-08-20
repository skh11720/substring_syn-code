package snu.kdd.substring_syn.data.record;

import java.util.Comparator;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.Rule;

public class Records {

	public static String[] tokenize( String str ) {
		return str.split( "( |\t)+" );
	}

	public static ObjectList<Record> expandAll( Record rec ) {
		ObjectList<Record> rslt = new ObjectArrayList<Record>();
		int[] tokens = rec.getTokenArray();
		expandAll( rslt, rec, 0, tokens );
		return rslt;
	}

	private static void expandAll( ObjectList<Record> rslt, Record rec, int idx, int[] t ) {

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
