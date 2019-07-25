package snu.kdd.substring_syn;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Random;

import org.junit.Test;

import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Util;

public class SubrecordTest {

	@Test
	public void test() throws IOException {
		String dataName = "SPROT_long";
		String size = "100";
		Dataset dataset = Dataset.createInstanceByName(dataName, size);
		Random rn = new Random(0);
		
		for ( Record rec : dataset.indexedList ) {
			if ( rec.getNumApplicableRules() < 10 ) continue;
			System.out.println(rec.toStringDetails());
			
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
		System.out.println(subrec0.toStringDetails());
		
		assertEquals(Util.sumWindowSize(subrec0), Util.sumWindowSize(subrec1));
	}
}
