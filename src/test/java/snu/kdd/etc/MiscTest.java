package snu.kdd.etc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Random;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.RecordSortedSlidingWindowIterator;

public class MiscTest {
	
	@Test
	public void testSubrecord() throws IOException {
		
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT_long", "1000");
		double theta = 1.0;
		Record rec = dataset.indexedList.get(622);
		System.out.println(rec.toStringDetails());
		
		for ( int w=1; w<=rec.size(); ++w ) {
			System.out.println("window size: "+w);
			RecordSortedSlidingWindowIterator slider = new RecordSortedSlidingWindowIterator(rec, w, theta);
			while ( slider.hasNext() ) {
				Subrecord window = slider.next();
				Record wrec = window.toRecord();
				System.out.println(wrec.toStringDetails());
			}
		}
	}
	
	@Test
	public void testWindowCount() throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT_long", "100");
		double theta = 0.6;
		for ( Record rec : dataset.indexedList ) {
			int nw0 = sumWindowSize(rec);
			int nw1 = 0;
			for ( int w=1; w<=rec.size(); ++w ) {
				RecordSortedSlidingWindowIterator witer = new RecordSortedSlidingWindowIterator(rec, w, theta);
				for ( int widx=0; witer.hasNext(); ++widx ) {
					Subrecord window = witer.next();
					nw1 += window.size();
				}
			}
			System.out.println(rec.getID()+"\t"+rec.size()+"\t"+nw0+"\t"+nw1);
			assertEquals(nw0, nw1);
		}
	}
	
	public static int sumWindowSize( Record rec ) {
		int n = rec.size();
		return n*(n+1)*(n+1)/2 - n*(n+1)*(2*n+1)/6;
	}
	
	@Test
	public void testTokenCountUpperBoundIterator() throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT", "1000");
		Random rn = new Random();
		for ( int i=0; i<10; ++i ) {
			Record rec = dataset.indexedList.get(rn.nextInt(dataset.indexedList.size()));
			System.out.println(rec.toStringDetails());
			Iterator<Entry> tokenCountIter = getTokenCountUpperBoundIterator(rec);
			while (tokenCountIter.hasNext()) {
				Entry entry = tokenCountIter.next();
				System.out.println(entry.getIntKey()+" : "+entry.getIntValue());
			}
		}
	}
	
	@Test
	public void testTransTokenSetSizeLowerBound() throws IOException {
		double avgDiff = 0;
		double avgRelDiff = 0;
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT", "1000");
		for ( Record rec : dataset.searchedList ) {
			int minTransSetSize = rec.getDistinctTokenCount();
			for ( Record exp : rec.expandAll() )  minTransSetSize = Math.min(minTransSetSize, exp.getDistinctTokenCount());
			int lb = getTransSetSizeLowerBound(rec);
//			System.out.println(rec.getID()+"\t"+minTransSetSize+"\t"+lb);
			assertTrue(minTransSetSize >= lb);
			avgDiff += minTransSetSize - lb;
			avgRelDiff += 1.0*(minTransSetSize - lb)/minTransSetSize;
		}
		avgDiff /= dataset.searchedList.size();
		avgRelDiff /= dataset.searchedList.size();
		System.out.println("avgDiff : "+avgDiff);
		System.out.println("avgRelDiff : "+avgRelDiff);
	}
	
	public static int getTransSetSizeLowerBound( Record rec ) {
		Iterator<Entry> tokenCountIter = getTokenCountUpperBoundIterator(rec);

		int lb = 0;
		int len = 0;
		while ( tokenCountIter.hasNext() && len < rec.getMinTransLength() ) {
			++lb;
			len += tokenCountIter.next().getIntValue();
		}
		return lb;
	}
	
	public static Iterator<Entry> getTokenCountUpperBoundIterator( Record rec ) {
		Comparator<Entry> comp = new Comparator<Entry>() {
			@Override
			public int compare(Entry o1, Entry o2) {
				if ( o1.getIntValue() > o2.getIntValue() ) return -1;
				else if ( o1.getIntValue() < o2.getIntValue() ) return 1;
				else return 0;
			}
		};
		
		Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
		for ( Rule rule : rec.getApplicableRuleIterable() ) {
			for ( int token : rule.getRhs() ) counter.addTo(token, rule.lhsSize());
		}
		Iterator<Entry> tokenCountIter = counter.int2IntEntrySet().stream().sorted(comp).iterator();
		return tokenCountIter;
	}
}
