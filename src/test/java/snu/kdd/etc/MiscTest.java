package snu.kdd.etc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.RecordSortedSlidingWindowIterator;

public class MiscTest {
	
	@Test
	public void testSubrecord() throws IOException {
		
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT_long", 1000);
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
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT_long", 100);
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
}
