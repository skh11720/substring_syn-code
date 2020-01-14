package snu.kdd.etc;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import org.junit.Test;

import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.StatContainer;

public class RecordSerializationTest {
	
	public static boolean[] checkEquivalence(Record rec0, Record rec1) {
		BooleanArrayList list = new BooleanArrayList();
		list.add(rec0.getID() == rec1.getID());
		list.add(rec0.size() == rec1.size());
		list.add(rec0.getApplicableRules().length == rec1.getApplicableRules().length);
		for ( int i=0; i<rec0.size(); ++i ) {
			list.add(rec0.getToken(i) == rec1.getToken(i));
			Iterator<Rule> iter0;
			Iterator<Rule> iter1;
			iter0 = rec0.getApplicableRules(i).iterator();
			iter1 = rec1.getApplicableRules(i).iterator();
			while (iter0.hasNext()) list.add(iter0.next().getID() == iter1.next().getID());
			iter0 = rec0.getSuffixApplicableRules(i).iterator();
			iter1 = rec1.getSuffixApplicableRules(i).iterator();
			while (iter0.hasNext()) list.add(iter0.next().getID() == iter1.next().getID());
			for ( int j=0; j<rec0.getSuffixRuleLens(i).length; ++j ) list.add(rec0.getSuffixRuleLens(i)[j].equals(rec1.getSuffixRuleLens(i)[j]));
			list.add(rec0.getMaxRhsSize() == rec1.getMaxRhsSize());
		}
		return list.toBooleanArray();
	}

	@Test
	public void testCorrectness() throws IOException {
		DatasetParam param = new DatasetParam("WIKI", "10000", "107836", "5", "1.0");
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		
		for ( Record rec0 : dataset.getIndexedList() ) {
			rec0.preprocessApplicableRules();
			rec0.preprocessSuffixApplicableRules();
			rec0.getMaxRhsSize();
			
			byte[] buf = rec0.serialize();
			Record rec1 = Record.deserialize(buf, buf.length, dataset.ruleSet);
			boolean[] b = checkEquivalence(rec0, rec1);
			assertTrue(BooleanArrayList.wrap(b).stream().allMatch(b0 -> b0));
		}
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testEfficiency() throws IOException {
		/*
		Record.serialize: 0.0125406 ms
		Record.deserialize: 0.0074352 ms
		 */
		DatasetParam param = new DatasetParam("WIKI", "10000", "107836", "5", "1.0");
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		byte[][] bs = new byte[dataset.size][];
		StatContainer stat = new StatContainer();
		
		for ( Record rec : dataset.getIndexedList() ) {
			rec.preprocessApplicableRules();
			rec.preprocessSuffixApplicableRules();
			rec.getMaxRhsSize();
			stat.startWatch("Record.serialize");
			byte[] b = rec.serialize();
			stat.stopWatch("Record.serialize");
			bs[rec.getID()] = b;
		}
		
		for ( int i=0; i<bs.length; ++i ) {
			stat.startWatch("Record.deserialize");
			Record rec = Record.deserialize(bs[i], bs[i].length, dataset.ruleSet);
			stat.stopWatch("Record.deserialize");
		}

		stat.finalize();
		System.out.println("Record.serialize: "+(Double.parseDouble(stat.getStat("Record.serialize"))/dataset.size)+" ms");
		System.out.println("Record.deserialize: "+(Double.parseDouble(stat.getStat("Record.deserialize"))/dataset.size)+" ms");
	}
}
