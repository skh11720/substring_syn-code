package snu.kdd.etc;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import snu.kdd.substring_syn.algorithm.filter.old.TransSetBoundCalculator3;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Util;

public class LowerBoundMonotonicityTest {

	@Test
	public void test() throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT_long", "1000");
		double theta = 0.7;
		for ( Record rec : dataset.indexedList ) {
			TransSetBoundCalculator3 boundCalculator = new TransSetBoundCalculator3(null, rec, theta);
			int[] lbArr = new int[rec.size()];
			for ( int j=0; j<rec.size(); ++j ) lbArr[j] = boundCalculator.getLB(0, j);
			if ( !isMonoIncreasing(lbArr) ) System.out.println(Arrays.toString(lbArr));

			int[] lbMonoArr = new int[rec.size()];
			for ( int j=0; j<rec.size(); ++j ) lbMonoArr[j] = boundCalculator.getLBMono(0, j);
			assertTrue( isMonoIncreasing(lbMonoArr) );
		}
	}
	
	public boolean isMonoIncreasing( int[] arr ) {
		for ( int i=0; i<arr.length-1; ++i ) {
			if ( arr[i+1] < arr[i] ) return false;
		}
		return true;
	}

}
