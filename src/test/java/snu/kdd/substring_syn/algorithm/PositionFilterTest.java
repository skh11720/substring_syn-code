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
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.utils.Util;

public class PositionFilterTest {

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
				IntList commonTokenIdxList = getCommonTokenIdxList(rec, queryTokenSet);
				if ( commonTokenIdxList.size() < minCount ) continue;
				double[] scoreArr = computeSplitScore(rec, commonTokenIdxList);
				ObjectList<IntPair> segmentList = splitRecord(rec, commonTokenIdxList, scoreArr, theta, queryTokenSet, minCount);
				if ( segmentList.size() == 0 ) continue;
				visualizeCandRecord(rec, queryTokenSet);
			}
		}
	}
	
	private static IntList getCommonTokenIdxList( Record rec, IntSet tokenSet ) {
		IntList commonTokenIdxList = new IntArrayList();
		for ( int i=0; i<rec.size(); ++i ) {
			if ( tokenSet.contains(rec.getToken(i)) ) commonTokenIdxList.add(i);
		}
		return commonTokenIdxList;
	}

	private static double[] computeSplitScore( Record rec, IntList commonTokenIdxList ) {
		int m = commonTokenIdxList.size();
		double[] scoreArr = new double[m-1];
		for ( int i=0; i<m-1; ++i ) {
			int sidx = commonTokenIdxList.get(i);
			IntSet numSet = new IntOpenHashSet(rec.getToken(sidx));
			IntSet denumSet = new IntOpenHashSet(rec.getToken(sidx));
			int eidx0 = sidx;
			for ( int j=i+1; j<m; ++j ) {
				int eidx1 = commonTokenIdxList.get(j);
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
	
	private static ObjectList<IntPair> splitRecord( Record rec, IntList commonTokenIdxList, double[] scoreArr, double theta, IntSet candTokenSet, int minCount ) {
		ObjectList<IntPair> segmentList = new ObjectArrayList<>();
		int sidx = commonTokenIdxList.get(0);
		int eidx = -1;
		int count = 1;
		for ( int i=0; i<scoreArr.length; ++i ) {
			if ( scoreArr[i] < theta ) {
				eidx = commonTokenIdxList.get(i);
				if ( count >= minCount ) segmentList.add(new IntPair(sidx, eidx+1));
				IntList segment = rec.getTokenList().subList(sidx, eidx+1);
				visualizeCandRecord(segment, candTokenSet);
				sidx = commonTokenIdxList.get(i+1);
				count = 1;
			}
			++count;
		}
		visualizeCandRecord(rec.getTokenList().subList(sidx, commonTokenIdxList.get(commonTokenIdxList.size()-1)+1), candTokenSet);
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
