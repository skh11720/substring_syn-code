package snu.kdd.pkwise;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import snu.kdd.substring_syn.data.record.Subrecord;

public class PkwiseTokenOrderTest {

	@Test
	public void test() throws IOException {
		WindowDataset dataset = TestUtils.getTestDataset();
		PkwiseTokenOrder order = new PkwiseTokenOrder(dataset);
		
		Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
		for ( Subrecord window : dataset.getWindowList() ) {
			for ( int token : window.getTokenArray() ) {
				counter.addTo(token, 1);
			}
		}
		
		int c0 = -1;
		Iterator<Int2IntMap.Entry> iter = counter.int2IntEntrySet().stream().sorted( Comparator.comparing(Int2IntMap.Entry::getIntKey) ).iterator();
		while ( iter.hasNext() ) {
			int c = iter.next().getIntValue();
			assertTrue(c >= c0);
			c0 = c;
		}
	}

}
