package snu.kdd.etc;

import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.junit.Test;

import snu.kdd.substring_syn.TestDatasetManager;
import snu.kdd.substring_syn.algorithm.filter.TransLenCalculator;
import snu.kdd.substring_syn.algorithm.filter.TransLenLazyCalculator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;

@Deprecated
public class TransLenCalculatorTest {

	@Test
	public void testTransLenLazyCalculatorCorrectness() {
		Dataset dataset = TestDatasetManager.getDataset("WIKI", "10000", "107836", "5", "1.0");
		Random rn = new Random();
		for ( double theta : new double[] {0.7, 1.0} ) {
			for ( TransformableRecordInterface rec : dataset.getIndexedList() ) {
				rec.preprocessApplicableRules();
				rec.preprocessSuffixApplicableRules();
				TransLenCalculator transLen0 = new TransLenCalculator(null, rec, theta);
				
				for ( int sidx=0; sidx<rec.size(); ++sidx ) {
					int maxlen = rec.size()-sidx;
					TransLenLazyCalculator transLen1 = new TransLenLazyCalculator(null, rec, sidx, maxlen, theta);
					int eidx = sidx;
					while ( eidx < rec.size() ) {
						assertEquals(transLen0.getLB(sidx, eidx), transLen1.getLB(eidx));
						eidx += rn.nextInt(5);
					}
				}
//				break;
			}
		}
	}
}
