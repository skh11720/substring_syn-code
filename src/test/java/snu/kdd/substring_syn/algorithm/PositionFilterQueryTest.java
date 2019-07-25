package snu.kdd.substring_syn.algorithm;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.RecordInterface;
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.utils.IntRange;
import snu.kdd.substring_syn.utils.Util;

public class PositionFilterQueryTest {

	double[] thetaList = {0.6, 0.7, 0.8, 0.9, 1.0};
	String[] sizeList = {"100", "101", "102", "103", "104", "105"};
	String dataName = "SPROT_long";
//	String dataName = "WIKI_3";
//	String[] sizeList = {"874070"};
	
	@Ignore
	public void testQuerySideAll() throws IOException {
		for ( double theta : thetaList ) {
			for ( String size : sizeList ) {
				testQuerySideSplit(dataName, size, theta);
			}
		}
	}
	
	@Test
	public void testSingle() throws IOException {
		testQuerySideSplit(dataName, "100", 0.7);
	}

	@Deprecated
	public void testQuerySideDistinctIncremental( String dataName, String size, double theta ) throws IOException {
		// assume tokens in a string are distinct 
		Dataset dataset = Dataset.createInstanceByName(dataName, size);
		
		for ( Record query : dataset.searchedList ) {
			IntSet tokenSet = new IntOpenHashSet(query.getTokens());
			IntList queryTokenList = new IntArrayList(query.getTokenArray());
			for ( Record rec : dataset.indexedList ) {
				IntList recTokenList = new IntArrayList(rec.getTokenArray());
				IntList idxList = new IntArrayList();
				for ( int i=0; i<rec.size(); ++i ) {
					if ( tokenSet.contains(rec.getToken(i)) ) idxList.add(i);
				}
				if ( idxList.size() == 0 ) continue;
				
				for ( int i=0; i<idxList.size()-1; ++i ) {
					int sidx = idxList.get(i);
					double simPrev;
					double sim = Util.jaccard(queryTokenList, recTokenList.subList(sidx, sidx+1));
					for ( int j=i+1; j<idxList.size(); ++j ) {
						simPrev = sim;
						int eidx = idxList.get(j);
//						IntSet appended = new IntOpenHashSet(recTokenList.subList(idxList.get(j-1)+1, eidx));
						int appendedSize = eidx - idxList.get(j-1) - 1;
						sim = Util.jaccard(queryTokenList, recTokenList.subList(sidx, eidx+1));
						if ( sim >= theta && theta >= 1.0/appendedSize ) {
							try {
								assertTrue( simPrev >= theta );
							} catch ( AssertionError e ) {
								System.out.println("theta: "+theta);
								System.out.println("query: ["+query.getID()+"]  "+queryTokenList);
								System.out.println("rec: ["+rec.getID()+"]  "+recTokenList);
								System.out.println("window: "+recTokenList.subList(sidx, eidx+1));
								System.out.println("window: "+(new Subrecord(rec, sidx, eidx+1)).toOriginalString());
								System.out.println("appendedSize: "+appendedSize);
								System.out.println("sidx, eidx, simPrev, sim: "+sidx+", "+eidx+", "+simPrev+", "+sim);
								throw e;
							}
						}
					}
				}
			}
		}
	}

	@Deprecated
	public void testQuerySideIncremental( String dataName, String size, double theta ) throws IOException {
		// do not assume tokens in a string are distinct 
		Dataset dataset = Dataset.createInstanceByName(dataName, size);
		
		for ( Record query : dataset.searchedList ) {
			IntSet tokenSet = new IntOpenHashSet(query.getTokens());
			IntList queryTokenList = new IntArrayList(query.getTokenArray());
			for ( Record rec : dataset.indexedList ) {
				IntList recTokenList = new IntArrayList(rec.getTokenArray());
				IntList idxList = new IntArrayList();
				for ( int i=0; i<rec.size(); ++i ) {
					if ( tokenSet.contains(rec.getToken(i)) ) idxList.add(i);
				}
				if ( idxList.size() == 0 ) continue;
				
				for ( int i=0; i<idxList.size()-1; ++i ) {
					int sidx = idxList.get(i);
					double simPrev;
					double sim = Util.jaccard(queryTokenList, recTokenList.subList(sidx, sidx+1));
					for ( int j=i+1; j<idxList.size(); ++j ) {
						simPrev = sim;
						int eidx0 = idxList.get(j-1);
						int eidx1 = idxList.get(j);
						IntSet appended = new IntOpenHashSet(recTokenList.subList(eidx0+1, eidx1));
						appended.removeAll(recTokenList.subList(sidx, eidx0+1));
						sim = Util.jaccard(queryTokenList, recTokenList.subList(sidx, eidx1+1));
						if ( sim >= theta && theta >= 1.0/appended.size() ) {
							try {
								assertTrue( simPrev >= theta );
							} catch ( AssertionError e ) {
								System.out.println("theta: "+theta);
								System.out.println("query: ["+query.getID()+"]  "+queryTokenList);
								System.out.println("rec: ["+rec.getID()+"]  "+recTokenList);
								System.out.println("window: "+recTokenList.subList(sidx, eidx1+1));
								System.out.println("window: "+(new Subrecord(rec, sidx, eidx1+1)).toOriginalString());
								System.out.println("appended.size: "+appended.size());
								System.out.println("sidx, eidx, simPrev, sim: "+sidx+", "+eidx1+", "+simPrev+", "+sim);
								throw e;
							}
						}
					}
				}
			}
		}
	}
	
	public void testQuerySideSplit( String dataName, String size, double theta ) throws IOException {
		Dataset dataset = Dataset.createInstanceByName(dataName, size);
		for ( Record query : dataset.searchedList ) {
			IntSet queryTokenSet = query.getCandTokenSet();
			int minCount = (int)Math.ceil(theta*query.getTransSetLB());
			System.out.println("minCount: "+minCount);
			for ( Record rec : dataset.indexedList ) {
				IntList idxList = getCommonTokenIdxListQuerySide(rec, queryTokenSet);
				if ( idxList.size() < minCount ) continue;
				visualizeCandRecord(rec, queryTokenSet);
				ObjectList<IntRange> segmentRangeList = findSegmentRanges(query, rec, idxList, theta);
				ObjectList<RecordInterface> segmentList = splitRecord(rec, segmentRangeList, idxList, minCount);
				if ( segmentList.size() == 0 ) continue;
			}
		}
	}
	
	private static IntList getCommonTokenIdxListQuerySide( Record rec, IntSet tokenSet ) {
		IntList idxList = new IntArrayList();
		for ( int i=0; i<rec.size(); ++i ) {
			if ( tokenSet.contains(rec.getToken(i)) ) idxList.add(i);
		}
		return idxList;
	}

	private static ObjectList<IntRange> findSegmentRanges( Record query, Record rec, IntList idxList, double theta ) {
		int m = idxList.size();
		ObjectList<IntRange> rangeList = new ObjectArrayList<>();
		for ( int i=0; i<m-1; ++i ) {
			int sidx = idxList.get(i);
			IntSet numSet = new IntOpenHashSet(rec.getToken(sidx));
			IntSet denumSet = new IntOpenHashSet(rec.getToken(sidx));
			int eidx0 = sidx;
			for ( int j=i; j<m; ++j ) {
				int eidx1 = idxList.get(j);
				numSet.add(rec.getToken(eidx1));
				denumSet.addAll(rec.getTokenList().subList(eidx0+1, eidx1+1));
				double score = (double)numSet.size()/Math.max(query.getTransSetLB(), denumSet.size());
				if ( score >= theta ) {
					if ( rangeList.size() > 0 && rangeList.get(rangeList.size()-1).min == sidx ) rangeList.get(rangeList.size()-1).max = eidx1;
					else rangeList.add(new IntRange(sidx, eidx1));
				}
				eidx0 = eidx1;
			}
		}
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
	
	private static ObjectList<RecordInterface> splitRecord( Record rec, ObjectList<IntRange> segmentRangeList, IntList idxList, int minCount ) {
		ObjectList<RecordInterface> segmentList = new ObjectArrayList<>();
		if ( segmentList != null ) {
			for ( IntRange range : segmentRangeList ) {
				int count = 0;
				for ( int idx : idxList ) {
					if ( range.min > idx ) continue;
					if ( idx > range.max ) break;
					++count;
				}
				if ( count >= minCount ) segmentList.add(new Subrecord(rec, range.min, range.max+1));
			}
		}
		return segmentList;
	}

	private static double[] computeSplitScore( Record rec, IntList idxList ) {
		int m = idxList.size();
		double[] scoreArr = new double[m-1];
		for ( int i=0; i<m-1; ++i ) {
			int sidx = idxList.get(i);
			IntSet numSet = new IntOpenHashSet(rec.getToken(sidx));
			IntSet denumSet = new IntOpenHashSet(rec.getToken(sidx));
			int eidx0 = sidx;
			for ( int j=i+1; j<m; ++j ) {
				int eidx1 = idxList.get(j);
				numSet.add(rec.getToken(eidx1));
				denumSet.addAll(rec.getTokenList().subList(eidx0+1, eidx1+1));
				for ( int k=i; k<j; ++k ) scoreArr[k] = Math.max(scoreArr[k], (double)numSet.size()/denumSet.size());
				eidx0 = eidx1;
			}
		}
		Arrays.stream(scoreArr).forEach(x -> System.out.print(String.format("%.3f, ", x)));
		System.out.println();
		return scoreArr;
	}

	private static void findSplitPoint( Record rec, IntList idxList, double theta ) {
		int m = idxList.size();
		ObjectList<IntRange> rangeList = new ObjectArrayList<>();
		for ( int i=0; i<m-1; ++i ) {
			int sidx = idxList.get(i);
			IntSet numSet = new IntOpenHashSet(rec.getToken(sidx));
			IntSet denumSet = new IntOpenHashSet(rec.getToken(sidx));
			int eidx0 = sidx;
			for ( int j=i+1; j<m; ++j ) {
				int eidx1 = idxList.get(j);
				numSet.add(rec.getToken(eidx1));
				denumSet.addAll(rec.getTokenList().subList(eidx0+1, eidx1+1));
				double score = (double)numSet.size()/denumSet.size();
				if ( score >= theta ) {
					if ( rangeList.size() > 0 && rangeList.get(rangeList.size()-1).min == sidx ) rangeList.get(rangeList.size()-1).max = eidx1;
					else rangeList.add(new IntRange(sidx, eidx1));
				}
				eidx0 = eidx1;
			}
		}
		for ( IntRange range : rangeList ) System.out.println(range);
		
		// merge
		if ( rangeList.size() > 0 ) {
			System.out.println("merge:");
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
			for ( IntRange range : mergedRangeList ) System.out.println(range);
		}
	}
	
	private static ObjectList<IntPair> splitRecord( Record rec, IntList idxList, double[] scoreArr, double theta, IntSet candTokenSet, int minCount ) {
		ObjectList<IntPair> segmentList = new ObjectArrayList<>();
		int sidx = idxList.get(0);
		int eidx = -1;
		int count = 1;
		for ( int i=0; i<scoreArr.length; ++i ) {
			if ( scoreArr[i] < theta ) {
				eidx = idxList.get(i);
				if ( count >= minCount ) segmentList.add(new IntPair(sidx, eidx+1));
				IntList segment = rec.getTokenList().subList(sidx, eidx+1);
				visualizeCandRecord(segment, candTokenSet);
				sidx = idxList.get(i+1);
				count = 1;
			}
			++count;
		}
		visualizeCandRecord(rec.getTokenList().subList(sidx, idxList.get(idxList.size()-1)+1), candTokenSet);
		System.out.println(segmentList);
		return segmentList;
	}

	private static void visualizeCandRecord( Record rec, IntSet candTokenSet ) {
		visualizeCandRecord(rec.getTokenList(), candTokenSet);
	}

	private static void visualizeCandRecord( IntList arr, IntSet candTokenSet ) {
		int count = 0;
		StringBuilder strbld = new StringBuilder();
		for ( int token : arr ) {
			if ( candTokenSet.contains(token) ) {
				strbld.append("O");
				++count;
			}
			else strbld.append('-');
		}
		System.out.println(count+"\t"+strbld.toString());
	}
}
