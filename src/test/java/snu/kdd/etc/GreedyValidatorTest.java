package snu.kdd.etc;

import java.io.IOException;

import org.junit.Test;

import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.utils.Util;
import vldb18.GreedyPkduckValidator;

public class GreedyValidatorTest {

	@Test
	public void test() throws IOException {
		Dataset dataset = Util.getDatasetWithPreprocessing("SPROT", "1000");
		long ts;
		long[] tArr = new long[2];
		GreedyPkduckValidator validator0 = new GreedyPkduckValidator();
		
		for ( Record recS : dataset.searchedList ) {
			for ( Record recT : dataset.indexedList ) {
				ts = System.nanoTime();
				double sim0 = validator0.sim(recS, recT);
				tArr[0] += System.nanoTime() - ts;
				ts = System.nanoTime();
//				double sim1 = validator1.sim(recS, recT);
				tArr[1] += System.nanoTime() - ts;
				
				if ( recS.expandAll().size() > 5 && sim0 >= 0.5 && sim0 < 1) {
					System.out.println(recS.getID()+"\t("+recS.size()+")\t:\t"+recS);
					for ( Record exp : recS.expandAll() ) {
						System.out.println("\t("+exp.size()+")\t"+exp);
					}
					System.out.println(recT.getID()+"\t("+recT.size()+")\t:\t"+recT);
					System.out.println(sim0);
				}
			}
			if ( recS.getID() > 1000 ) break;
		}
		for ( long t : tArr ) {
			System.out.println(t/1e6);
		}
	}
}
