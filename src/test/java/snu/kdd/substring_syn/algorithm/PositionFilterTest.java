package snu.kdd.substring_syn.algorithm;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.utils.Util;

public class PositionFilterTest {

	double[] thetaList = {0.6, 0.7, 0.8, 0.9, 1.0};
//	String[] sizeList = {"100", "101", "102", "103", "104", "105"};
//	String dataName = "SPROT_long";
	String dataName = "WIKI_3";
	String[] sizeList = {"874070"};
	
	@Test
	public void testQuerySideAll() throws IOException {
		for ( double theta : thetaList ) {
			for ( String size : sizeList ) {
				testQuerySide(dataName, size, theta);
			}
		}
	}

	public void testQuerySide( String dataName, String size, double theta ) throws IOException {
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
						IntSet appended = new IntOpenHashSet(recTokenList.subList(idxList.get(j-1)+1, eidx));
						sim = Util.jaccard(queryTokenList, recTokenList.subList(sidx, eidx+1));
						if ( simPrev >= theta && theta >= 1.0/appended.size() ) {
							try {
								assertTrue( sim <= simPrev );
							} catch ( AssertionError e ) {
								System.out.println("theta: "+theta);
								System.out.println("query: "+queryTokenList);
								System.out.println("rec: "+recTokenList);
								System.out.println("window: "+recTokenList.subList(sidx, eidx+1));
								System.out.println("appended: "+appended);
								System.out.println("sidx, eidx, simPrev, sim: "+sidx+", "+eidx+", "+simPrev+", "+sim);
								throw e;
							}
						}
					}
				}
			}
		}
	}
}
