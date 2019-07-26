package snu.kdd.substring_syn.data.record;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.ACAutomataR;
import snu.kdd.substring_syn.data.Rule;

public class RecordPreprocess {

	public static void preprocessApplicableRules( Record rec, ACAutomataR automata ) {
		rec.applicableRules = automata.applicableRules( rec.tokens );
	}

//	public static void preprocessEstimatedRecords( Record rec ) {
//		@SuppressWarnings( "unchecked" )
//		ArrayList<Rule>[] tmpAppRules = new ArrayList[ rec.tokens.length ];
//		for( int i = 0; i < rec.tokens.length; ++i )
//			tmpAppRules[ i ] = new ArrayList<Rule>();
//
//		for( int i = 0; i < rec.tokens.length; ++i ) {
//			for( Rule rule : rec.applicableRules[ i ] ) {
//				int eidx = i + rule.lhsSize() - 1;
//				tmpAppRules[ eidx ].add( rule );
//			}
//		}
//
//		long[] est = new long[ rec.tokens.length ];
//		rec.estTrans = est;
//		for( int i = 0; i < est.length; ++i ) {
//			est[ i ] = Long.MAX_VALUE;
//		}
//
//		for( int i = 0; i < rec.tokens.length; ++i ) {
//			long size = 0;
//			for( Rule rule : tmpAppRules[ i ] ) {
//				int sidx = i - rule.lhsSize() + 1;
//				if( sidx == 0 ) {
//					size += 1;
//				}
//				else {
//					size += est[ sidx - 1 ];
//				}
//
//				if( size < 0 ) {
//					return;
//				}
//			}
//			est[ i ] = size;
//		}
//	}

	public static void preprocessSuffixApplicableRules( Record rec ) {
		ObjectList<ObjectList<Rule>> tmplist = new ObjectArrayList<ObjectList<Rule>>();

		for( int i = 0; i < rec.tokens.length; ++i ) {
			tmplist.add( new ObjectArrayList<Rule>() );
		}

		for( int i = rec.tokens.length - 1; i >= 0; --i ) {
			for( Rule rule : rec.applicableRules[ i ] ) {
				int suffixidx = i + rule.getLhs().length - 1;
				tmplist.get( suffixidx ).add( rule );
			}
		}

		rec.suffixApplicableRules = new Rule[ rec.tokens.length ][];
		for( int i = 0; i < rec.tokens.length; ++i ) {
			rec.suffixApplicableRules[ i ] = tmplist.get( i ).toArray( new Rule[ 0 ] );
		}
	}

	public static void preprocessTransformLength( Record rec ) {
		rec.transformLengths = new int[ rec.tokens.length ][ 2 ];
		for( int i = 0; i < rec.tokens.length; ++i )
			rec.transformLengths[ i ][ 0 ] = rec.transformLengths[ i ][ 1 ] = i + 1;

		for( Rule rule : rec.applicableRules[ 0 ] ) {
			int fromSize = rule.lhsSize();
			int toSize = rule.rhsSize();
			if( fromSize > toSize ) {
				rec.transformLengths[ fromSize - 1 ][ 0 ] = Math.min( rec.transformLengths[ fromSize - 1 ][ 0 ], toSize );
			}
			else if( fromSize < toSize ) {
				rec.transformLengths[ fromSize - 1 ][ 1 ] = Math.max( rec.transformLengths[ fromSize - 1 ][ 1 ], toSize );
			}
		}
		for( int i = 1; i < rec.tokens.length; ++i ) {
			rec.transformLengths[ i ][ 0 ] = Math.min( rec.transformLengths[ i ][ 0 ], rec.transformLengths[ i - 1 ][ 0 ] + 1 );
			rec.transformLengths[ i ][ 1 ] = Math.max( rec.transformLengths[ i ][ 1 ], rec.transformLengths[ i - 1 ][ 1 ] + 1 );
			for( Rule rule : rec.applicableRules[ i ] ) {
				int fromSize = rule.lhsSize();
				int toSize = rule.rhsSize();
				if( fromSize > toSize ) {
					rec.transformLengths[ i + fromSize - 1 ][ 0 ] = Math.min( rec.transformLengths[ i + fromSize - 1 ][ 0 ],
							rec.transformLengths[ i - 1 ][ 0 ] + toSize );
				}
				else if( fromSize < toSize ) {
					rec.transformLengths[ i + fromSize - 1 ][ 1 ] = Math.max( rec.transformLengths[ i + fromSize - 1 ][ 1 ],
							rec.transformLengths[ i - 1 ][ 1 ] + toSize );
				}

			}
		}
	}
}
