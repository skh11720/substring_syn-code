package snu.kdd.pkwise;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Iterator;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntQGram;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TransWindowDatasetTest {
	
	@Test
	public void test001_QgramRecordIterator() throws IOException {
		TransWindowDataset dataset = TestUtils.getTestDataset("WIKI", "10000", "1000", "5", "0.6");
		Iterator<IntQGram> iter = dataset.getIntQGramIterator(5);
		while ( iter.hasNext() ) {
			iter.next();
//			System.out.println(iter.next());
		}
	}
	
	@SuppressWarnings("unused")
	@Test
	public void test002_Construction() throws IOException {
		TransWindowDataset dataset = Dataset.createTransWindowInstanceByName("WIKI", "10000", "1000", "5", "0.6");
	}
	
	@Test
	public void test003_getIQGram() throws IOException {
		TransWindowDataset dataset = Dataset.createTransWindowInstanceByName("WIKI", "10000", "1000", "5", "0.6");
		Iterator<IntQGram> iter = dataset.getIntQGramsIterable().iterator();
		for ( int i=0; iter.hasNext() && i < 20; ++i ) {
			IntQGram iqgram0 = iter.next();
			IntQGram iqgram1 = dataset.getIntQGram(i);
			assertEquals(iqgram0, iqgram1);
		}
	}
}
