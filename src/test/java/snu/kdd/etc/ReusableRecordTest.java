package snu.kdd.etc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Iterator;

import org.junit.Test;

import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.ReusableRecord;

public class ReusableRecordTest {

	@Test
	public void testCorrectnessComparedToRecord() throws IOException {
		ReusableRecord recBuf = new ReusableRecord();
		Dataset dataset = DatasetFactory.createInstanceByName(new DatasetParam("WIKI", "100000", "107836", "5", "1.0"));
		for ( Record rec : dataset.getIndexedList() ) {
			recBuf.set(rec.getIdx(), rec.getID(), rec.getTokenArray(), rec.size());
			for ( int i=0; i<rec.size(); ++i ) {
				Iterator<Rule> iter;
				iter = rec.getApplicableRules(i).iterator();
				for ( int j=0; j<rec.getNumApplicableRules(i); ++j ) recBuf.addApplicableRule(i, iter.next());
				iter = rec.getSuffixApplicableRules(i).iterator();
				for ( int j=0; j<rec.getNumSuffixApplicableRules(i); ++j ) recBuf.addSuffixApplicableRule(i, iter.next());
				Iterator<IntPair> ipIter = rec.getSuffixRuleLens(i).iterator();
				for ( int j=0; j<rec.getNumSuffixRuleLens(i); ++j ) recBuf.addSuffixRuleLenPairs(i, ipIter.next());
			}
			recBuf.setMaxRhsSize(rec.getMaxRhsSize());
			
			String s0 = rec.toStringDetails();
			String s1 = recBuf.toStringDetails();
//			System.out.println(s0);
//			System.out.println(s1);
//			System.out.println(s0.equals(s1));
//			System.in.read();
			assertEquals(s0, s1);
		}
	}

	@Test
	public void testPreprocess() throws IOException {
		ReusableRecord recBuf = new ReusableRecord();
		Dataset dataset = DatasetFactory.createInstanceByName(new DatasetParam("WIKI", "100000", "107836", "5", "1.0"));
		for ( Record rec : dataset.getIndexedList() ) {
			recBuf.set(rec.getIdx(), rec.getID(), rec.getTokenArray(), rec.size());
			recBuf.preprocessApplicableRules();
			recBuf.preprocessSuffixApplicableRules();

			String s0 = rec.toStringDetails();
			String s1 = recBuf.toStringDetails();
//			System.out.println(s0);
//			System.out.println(s1);
//			System.out.println(s0.equals(s1));
//			System.in.read();
			assertEquals(s0, s1);
		}
	}
}

