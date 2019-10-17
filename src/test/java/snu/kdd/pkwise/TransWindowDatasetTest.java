package snu.kdd.pkwise;

import java.io.IOException;
import java.util.Iterator;

import org.junit.Test;

import snu.kdd.substring_syn.data.QGram;

public class TransWindowDatasetTest {

	@Test
	public void test() throws IOException {
		TransWindowDataset dataset = TestUtils.getTestDataset("WIKI", "10000", "107836", "5", "0.6");
		Iterator<QGram> iter = dataset.getIterator();
		while ( iter.hasNext() ) {
			iter.next();
//			System.out.println(iter.next());
		}
	}

}
