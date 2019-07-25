package snu.kdd.substring_syn.algorithm;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.filter.TransSetBoundCalculator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.RecordInterface;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.utils.IntRange;

public class PositionFilterTextSideTest {

	double[] thetaList = {0.6, 0.7, 0.8, 0.9, 1.0};
	String[] sizeList = {"100", "101", "102", "103", "104", "105"};
	String dataName = "SPROT_long";
//	String dataName = "WIKI_3";
//	String[] sizeList = {"874070"};

	@Ignore
	public void testQuerySideAll() throws IOException {
		for ( double theta : thetaList ) {
			for ( String size : sizeList ) {
				testTextSideSplit(dataName, size, theta);
			}
		}
	}
	
	@Test
	public void testSingle() throws IOException {
		testTextSideSplit(dataName, "100", 0.7);
	}


	public void testTextSideSplit( String dataName, String size, double theta ) throws IOException {
		Dataset dataset = Dataset.createInstanceByName(dataName, size);
		for ( Record query : dataset.searchedList ) {
			IntSet queryTokenSet = new IntOpenHashSet(query.getTokens());
			int minCount = (int)Math.ceil(theta*query.size());
			System.out.println("minCount: "+minCount);
			for ( Record rec : dataset.indexedList ) {
				double modifiedTheta = getModifiedTheta(query, rec, theta);
				ObjectList<TokenPosPair> prefixIdxList = getCommonTokenIdxListPrefix(rec, queryTokenSet);
				ObjectList<TokenPosPair> suffixIdxList = getCommonTokenIdxListSuffix(rec, queryTokenSet);
				if ( prefixIdxList.size() < minCount ) continue;
				String vizPrefix = visualizeCandRecord(rec, prefixIdxList);
				String vizSuffix = visualizeCandRecord(rec, suffixIdxList);
				System.out.println(vizPrefix);
				System.out.println(vizSuffix);
				compareVisualizingStrigs(vizPrefix, vizSuffix);
				ObjectList<IntRange> segmentList = findSegments(query, rec, prefixIdxList, suffixIdxList, modifiedTheta);
				System.out.println("segmentList="+segmentList);
				ObjectList<RecordInterface> splitList = splitRecord(rec, segmentList, prefixIdxList, minCount);
				System.out.println("splitList="+strSplitList(splitList));
				System.out.println();
			}
		}
	}

	protected static double getModifiedTheta( Record query, Record rec, double theta ) {
		return theta * query.size() / (query.size() + 2*(rec.getMaxRhsSize()-1));
	}

	private static ObjectList<TokenPosPair> getCommonTokenIdxListPrefix( Record rec, IntSet queryTokenSet ) {
		ObjectList<TokenPosPair> idxList = new ObjectArrayList<>();
		for ( int k=0; k<rec.size(); ++k ) {
			for ( Rule rule : rec.getApplicableRules(k) ) {
				for ( int token : rule.getRhs() ) {
					if ( queryTokenSet.contains(token) ) idxList.add( new TokenPosPair(token, k) );
				}
			}
		}
		return idxList;
	}

	private static ObjectList<TokenPosPair> getCommonTokenIdxListSuffix( Record rec, IntSet queryTokenSet ) {
		ObjectList<TokenPosPair> idxList = new ObjectArrayList<>();
		for ( int k=0; k<rec.size(); ++k ) {
			for ( Rule rule : rec.getSuffixApplicableRules(k) ) {
				for ( int token : rule.getRhs() ) {
					if ( queryTokenSet.contains(token) ) idxList.add( new TokenPosPair(token, k) );
				}
			}
		}
		return idxList;
	}
	
	private static ObjectList<IntRange> findSegments( Record query, Record rec, ObjectList<TokenPosPair> prefixIdxList, ObjectList<TokenPosPair> suffixIdxList, double theta ) {
		TransSetBoundCalculator boundCalculator = new TransSetBoundCalculator(null, rec, theta);
		int m = prefixIdxList.size();
		ObjectList<IntRange> rangeList = new ObjectArrayList<>();
		for ( int i=0, j0=0; i<m; ++i ) {
			TokenPosPair entL = prefixIdxList.get(i);
			int sidx = entL.pos;
			IntSet numSet = new IntOpenHashSet(entL.token);
//			IntSet denumSet = new IntOpenHashSet(entL.token);
			int eidx0 = sidx;
			for ( int j=j0; j<m; ++j ) {
				TokenPosPair entR = suffixIdxList.get(j);
				int eidx1 = entR.pos;
				if ( eidx1 < sidx ) {
					++j0;
					continue;
				}
				numSet.add(entR.token);
//				denumSet.addAll(rec.getTokenList().subList(eidx0+1, eidx1+1));
				double score = (double)numSet.size()/Math.max(query.size(), boundCalculator.getLB(sidx, eidx1));
//				System.out.println(sidx+", "+eidx1+", "+score+", "+theta);
				if ( score >= theta ) {
					if ( rangeList.size() > 0 && rangeList.get(rangeList.size()-1).min == sidx ) rangeList.get(rangeList.size()-1).max = eidx1;
					else rangeList.add(new IntRange(sidx, eidx1));
				}
				eidx0 = eidx1;
			}
		}
		System.out.println("rangeList="+rangeList);
		if ( rangeList.size() == 0 ) return null;
		
		// merge
		ObjectList<IntRange> mergedRangeList = new ObjectArrayList<>();
		IntRange mergedRange = rangeList.get(0);
		for ( int i=1; i<rangeList.size(); ++i ) {
			IntRange thisRange = rangeList.get(i);
			if ( mergedRange.min == thisRange.min ) mergedRange.max = thisRange.max;
			else {
				if ( thisRange.min <= mergedRange.max ) mergedRange.max = Math.max(mergedRange.max, thisRange.max);
				else {
					mergedRangeList.add(mergedRange);
					mergedRange = thisRange;
				}
			}
		}
		mergedRangeList.add(mergedRange);
		return mergedRangeList;
	}

	private static ObjectList<RecordInterface> splitRecord( Record rec, ObjectList<IntRange> segmentList, ObjectList<TokenPosPair> prefixIdxList, int minCount ) {
		ObjectList<RecordInterface> splitList = new ObjectArrayList<>();
		if ( segmentList != null ) {
			for ( IntRange range : segmentList ) {
				int count = 0;
				for ( TokenPosPair e : prefixIdxList ) {
					if ( range.min > e.pos ) continue;
					if ( e.pos > range.max ) break;
					++count;
				}
				if ( count >= minCount ) splitList.add(new Subrecord(rec, range.min, range.max+1));
			}
		}
		return splitList;
	}
	
	private static String visualizeCandRecord( Record rec, ObjectList<TokenPosPair> idxList ) {
		StringBuilder strbld = new StringBuilder();
		for ( int i=0, j=0; i<rec.size(); ++i ) {
			int count = 0;
			while ( j < idxList.size() && i == idxList.get(j).pos ) {
				++j;
				++count;
			}
			if ( count == 0 ) strbld.append("-");
			else if ( count > 9 ) strbld.append("O");
			else strbld.append(count);
		}
		return strbld.toString();
	}

	private static void compareVisualizingStrigs( String vizPrefix, String vizSuffix ) {
		StringBuilder strbld = new StringBuilder();
		for ( int i=0; i<vizPrefix.length(); ++i ) {
			if ( vizPrefix.charAt(i) == vizSuffix.charAt(i) ) strbld.append('-');
			else strbld.append('#');
		}
		System.out.println( strbld.toString() );
	}
	
	static class TokenPosPair {
		int token;
		int pos;
		
		public TokenPosPair( int token, int pos ) {
			this.token = token;
			this.pos = pos;
		}
	}

	private static String strSplitList( ObjectList<RecordInterface> segmentList ) {
		StringBuilder strbld = new StringBuilder("[");
		for ( int i=0; i<segmentList.size(); ++i ) {
			if ( i > 0 ) strbld.append(", ");
			Subrecord subrec = (Subrecord)segmentList.get(i);
			strbld.append(String.format("(%d,%d)", subrec.sidx, subrec.eidx));
		}
		strbld.append("]");
		return strbld.toString();
	}
}
