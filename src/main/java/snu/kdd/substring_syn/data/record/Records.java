package snu.kdd.substring_syn.data.record;

import java.util.Arrays;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.Rule;

public class Records {

	public static ObjectList<Record> expandAll( TransformableRecordInterface rec ) {
		ObjectList<Record> rslt = new ObjectArrayList<Record>();
		int[] tokens = rec.getTokenArray();
		expandAll( rslt, rec, 0, tokens );
		return rslt;
	}

	private static void expandAll( ObjectList<Record> rslt, TransformableRecordInterface rec, int idx, int[] t ) {

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
	
	public static Iterable<Record> expands( Iterable<TransformableRecordInterface> records ) {
		return new Iterable<Record>() {

			@Override
			public Iterator<Record> iterator() {
				return new ExpandMultipleIterator(records);
			}
		};
	}
	
	private static class ExpandMultipleIterator implements Iterator<Record> {
		
		Iterator<TransformableRecordInterface> rIter = null;
		TransformableRecordInterface rec = null;
		ExpandIterator eIter = null;

		public ExpandMultipleIterator(Iterable<TransformableRecordInterface> records) {
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
//					Log.log.trace("rec.idx=%d, rec.nar=%d", ()->rec.getIdx(), ()->rec.getNumApplicableNonselfRules());
					eIter = new ExpandIterator(rec);
				}
			}
		}
		
		@Override
		public boolean hasNext() {
			return eIter != null && eIter.hasNext();
		}
	}
	
	public static Iterable<Record> expands( TransformableRecordInterface rec ) {
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
		
		public ExpandIterator( TransformableRecordInterface rec ) {
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
			TransformableRecordInterface rec;
			Rule[] ruleList;
			Iterator<Rule>[] riterList;
			int[] expand;
			int nRule;
			int lhsSize;
			int rhsSize;
			
			@SuppressWarnings("unchecked")
			public State( TransformableRecordInterface rec ) {
				this.rec = rec;
				ruleList = new Rule[rec.size()];
				riterList = new Iterator[rec.size()];
				for ( int i=0; i<rec.size(); ++i ) riterList[i] = rec.getApplicableRules(i).iterator();
				expand = new int[rec.getMaxTransLength()];
				nRule = 0;
				lhsSize = 0;
				rhsSize = 0;
			}
			
			public boolean transit() {
				while ( lhsSize >= rec.size() || !riterList[lhsSize].hasNext() ) {
					if ( lhsSize < rec.size() ) {
						riterList[lhsSize] = rec.getApplicableRules(lhsSize).iterator();
					}
					if ( nRule > 0 ) removeRule();
					else return false;
				}
				while ( lhsSize < rec.size() ) {
					if ( riterList[lhsSize].hasNext() ) {
						Rule r = riterList[lhsSize].next();
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
				return new Record(rec.getIdx(), rec.getID(), IntArrayList.wrap(expand).subList(0, rhsSize).toIntArray());
			}
			
			public String getExpandString() {
				if (nRule == 0 ) return "";
				else return getRecord().toOriginalString();
			}
			
			@Override
			public String toString() {
				return String.format("%s, %d, %d, %d, %s", Arrays.toString(riterList), nRule, lhsSize, rhsSize, getExpandString());
			}
		}
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
	
	public static Iterable<Subrecord> getSubrecords(RecordInterface rec) {
		return new Iterable<Subrecord>() {
			
			@Override
			public Iterator<Subrecord> iterator() {
				return new Iterator<Subrecord>() {
					
					int w = 1;
					int i = 0;
					
					@Override
					public Subrecord next() {
						Subrecord window = new Subrecord(rec,i, i+w);
						findNext();
						return window;
					}
					
					@Override
					public boolean hasNext() {
						return w <= rec.size();
					}
					
					private final void findNext() {
						i += 1;
						if ( i+w > rec.size() ) {
							w += 1;
							i = 0;
						}
					}
				};
			}
		};
	}

//	public static int getMaxTransformLength(TransformableRecordInterface rec) {
//		int[] transformLengths = new int[rec.size()];
//		for( int i = 0; i < rec.size(); ++i )
//			transformLengths[ i ] = i + 1;
//
//		for( Rule rule : rec.getApplicableRules(0) ) {
//			int fromSize = rule.lhsSize();
//			int toSize = rule.rhsSize();
//			if( fromSize < toSize ) {
//				transformLengths[ fromSize - 1 ] = Math.max( transformLengths[ fromSize - 1 ], toSize );
//			}
//		}
//		for( int i = 1; i < rec.size(); ++i ) {
//			transformLengths[ i ] = Math.max( transformLengths[ i ], transformLengths[ i - 1 ] + 1 );
//			for( Rule rule : rec.getApplicableRules(i) ) {
//				int fromSize = rule.lhsSize();
//				int toSize = rule.rhsSize();
//				if( fromSize < toSize ) {
//					transformLengths[ i + fromSize - 1 ] = Math.max( transformLengths[ i + fromSize - 1 ], transformLengths[ i - 1 ] + toSize );
//				}
//			}
//		}
//		return transformLengths[rec.size()-1];
//	}
}
