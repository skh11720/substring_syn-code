package snu.kdd.pkwise;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;

public class WindowDatasetTest {

	@Test
	public void visualize() throws IOException {
		WindowDataset dataset = TestUtils.getTestRawDataset();
		int w = 5;
		int n = 10000;
		Record[] recList = new Record[n];
		int ridx = 0;
		for ( Record rec : dataset.getIndexedList() ) {
			recList[ridx] = rec;
			ridx += 1;
			if ( ridx >= n ) break;
		}
		
		
		for ( int i=0; i<recList.length; ++i ) {
			System.out.println(String.format("rec[%d]: "+recList[i], i));
		}
		
		ridx = 0;
		for ( Subrecord window : dataset.getWindowList(w) ) {
			if ( window.getID() == ridx ) {
				if ( window.getID() >= n ) break;
				System.out.println(String.format("\nrec[%d]: "+recList[ridx], ridx));
				ridx += 1;
			}
			System.out.println(window);
		}
	}

	
	@Test
	@SuppressWarnings("unused")
	public void checkCount() throws IOException {
		WindowDataset dataset = TestUtils.getTestRawDataset();
		int w = 5;
		long n0 = 0;
		long n1 = 0;
		
		for ( Record rec : dataset.getIndexedList() ) {
			n0 += Math.max(0, rec.size()-w+1);
		}
		
		for ( Subrecord window : dataset.getWindowList(w) ) {
			n1 += 1;
		}
		
		assertEquals(n0, n1);
	}
	
	@Test
	public void getRecordTest() throws IOException {
		WindowDataset dataset = TestUtils.getTestDataset();
		int n = 10;
		int i = 0;
		for ( Record rec : dataset.getIndexedList() ) {
			System.out.println(rec);
			System.out.println(dataset.getRecord(i));
			i += 1;
			if ( i >= n ) break;
		}
	}
}
