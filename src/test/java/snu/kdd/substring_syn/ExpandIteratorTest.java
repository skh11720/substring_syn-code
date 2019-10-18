package snu.kdd.substring_syn;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Records;

public class ExpandIteratorTest {

	@Test
	public void test() throws IOException {
		Dataset dataset = Dataset.createInstanceByName("WIKI", "10000", "1000", "5");
		for ( Record rec : dataset.getIndexedList() ) {
			rec.preprocessAll();
			ObjectList<Record> expList = Records.expandAll(rec);
			int n0 = expList.size();

			int n1 = 0;
			for ( Record exp : Records.expands(rec) ) n1 += 1;
			try {
				assertEquals(n0, n1);
			} catch ( AssertionError e ) {
				for ( Record exp : expList ) System.err.println(exp);
			}
		}
	}
}
