package snu.kdd.etc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.Subrecord;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.iterator.SortedRecordSlidingWindowIterator;

public class MiscTest {
	
	@Test
	public void testSubrecord() throws IOException {
		
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT_long", "1000");
		double theta = 1.0;
		Record rec = dataset.indexedList.get(622);
		System.out.println(rec.toStringDetails());
		
		for ( int w=1; w<=rec.size(); ++w ) {
			System.out.println("window size: "+w);
			SortedRecordSlidingWindowIterator slider = new SortedRecordSlidingWindowIterator(rec, w, theta);
			while ( slider.hasNext() ) {
				Subrecord window = slider.next();
				Record wrec = window.toRecord();
				System.out.println(wrec.toStringDetails());
			}
		}
	}
	
	@Test
	public void testWindowCount() throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT_long", "1000");
		double theta = 0.6;
		for ( Record rec : dataset.indexedList ) {
			int nw0 = sumWindowSize(rec);
			int nw1 = 0;
			for ( int w=1; w<=rec.size(); ++w ) {
				SortedRecordSlidingWindowIterator witer = new SortedRecordSlidingWindowIterator(rec, w, theta);
				for ( int widx=0; witer.hasNext(); ++widx ) {
					Subrecord window = witer.next();
					nw1 += window.size();
				}
			}
//			System.out.println(rec.getID()+"\t"+rec.size()+"\t"+nw0+"\t"+nw1);
			assertEquals(nw0, nw1);
		}
	}
	
	public static int sumWindowSize( Record rec ) {
		int n = rec.size();
		return n*(n+1)*(n+1)/2 - n*(n+1)*(2*n+1)/6;
	}
	
	@Test
	public void testMaxTransLenOfSuffix() throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT_long", "1000");
		for ( Record rec : dataset.searchedList ) {
			if ( rec.size() - rec.getMinTransLength() < 0 || rec.getMaxTransLength() - rec.size() < 2 ) continue;
			System.out.println("min/0/maxTransLen: "+rec.getMinTransLength()+", "+rec.size()+", "+rec.getMaxTransLength());
			System.out.println( rec.toStringDetails() );
			int[] len = new int[rec.size()+1];
			for ( int sidx=0; sidx<rec.size(); ++sidx ) {
				getMaxTransLenOfSuffix(len, rec, sidx);
				System.out.println( "sidx: "+sidx+"\t"+Arrays.toString(len) );
			}
			System.in.read();
		}
	}

	public static void getMaxTransLenOfSuffix( int[] len, Record rec, int sidx ) {
		len[sidx] = 0;
		for ( int i=sidx+1; i<=rec.size(); ++i ) {
			len[i] = Math.max(i-sidx, len[i-1]+1);
			for ( Rule rule : rec.getSuffixApplicableRules(i-1) ) {
				// TODO: optimize this for loop ... so that iterates (|lhs|, |rhs|) pairs s.t. |lhs|<|rhs|)
				int l = rule.lhsSize();
				int r = rule.rhsSize();
				if ( l < r ) {
					len[i] = Math.max( len[i], len[i-l]+r );
				}
			}
		}
	}
}
