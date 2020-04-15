package snu.kdd.etc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Random;

import org.junit.Test;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.TestDatasetManager;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Util;

public class SubrecordTest {
	
	boolean showLog = false;

	@Test
	public void test() throws IOException {
		Dataset dataset = TestDatasetManager.getDataset("WIKI", "10000", "107836", "5");
		Random rn = new Random(0);
		
		for ( Record rec : dataset.getIndexedList() ) {
			rec.preprocessAll();
			if ( rec.getNumApplicableRules() < 5 ) continue;
			if (showLog) System.out.println(rec.toStringDetails());
			
			for ( int i=0; i<5; ++i ) {
				int sidx = rn.nextInt(rec.size()/2);
				int eidx = rn.nextInt(rec.size()/2) + rec.size()/2;
				testSubrec(rec, sidx, eidx);
			}
		}
	}

	public void testSubrec( Record rec, int sidx, int eidx ) {
		Subrecord subrec0 = new Subrecord(rec, sidx, eidx);
		Record subrec1 = subrec0.toRecord();
		subrec1.preprocessAll();
		if (showLog) System.out.println("[sidx, eidx] = ["+sidx+", "+eidx+"]");
		if (showLog) System.out.println(subrec0.toStringDetails());
		
		assertEquals(Util.sumWindowSize(subrec0), Util.sumWindowSize(subrec1));
		ObjectList<Rule> prefixRuleList0 = new ObjectArrayList<>(subrec0.getApplicableRuleIterable().iterator());
		ObjectList<Rule> prefixRuleList1 = new ObjectArrayList<>(subrec1.getApplicableRuleIterable().iterator());
		assertEquals(prefixRuleList0.size(), prefixRuleList1.size());
		for ( int i=0; i<prefixRuleList0.size(); ++i ) assertEquals(prefixRuleList0.get(i), prefixRuleList1.get(i));
		for ( int k=0; k<subrec1.size(); ++k ) {
			ObjectList<Rule> suffixRuleList0 = new ObjectArrayList<>(subrec0.getSuffixApplicableRules(k).iterator());
			ObjectList<Rule> suffixRuleList1 = new ObjectArrayList<>(subrec1.getSuffixApplicableRules(k).iterator());
			if (showLog) System.out.println("k: "+k);
			if (showLog) System.out.println(suffixRuleList0);
			if (showLog) System.out.println(suffixRuleList1);
			if (showLog) System.out.println(subrec1.getNumApplicableRules());
			assertEquals(suffixRuleList0.size(), suffixRuleList1.size());
//			for ( int i=0; i<suffixRuleList0.size(); ++i ) assertEquals(suffixRuleList0.get(i), suffixRuleList1.get(i));
		}
	}
}
