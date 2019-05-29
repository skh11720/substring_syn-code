package snu.kdd.etc;

import java.io.IOException;

import org.junit.Test;

import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.RecordSortedSlidingWindow;

public class MiscTest {
	
	@Test
	public void testSubrecord() throws IOException {
		
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT_long", 1000);
		double theta = 1.0;
		Record rec = dataset.indexedList.get(622);
		System.out.println(rec.toStringDetails());
		
		for ( int w=1; w<=rec.size(); ++w ) {
			System.out.println("window size: "+w);
			RecordSortedSlidingWindow slider = new RecordSortedSlidingWindow(rec, w, theta);
			while ( slider.hasNext() ) {
				Subrecord window = slider.next();
				Record wrec = window.toRecord();
				System.out.println(wrec.toStringDetails());
			}
		}
	}
}
