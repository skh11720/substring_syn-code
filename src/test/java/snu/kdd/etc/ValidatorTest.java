package snu.kdd.etc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.Test;

import snu.kdd.substring_syn.algorithm.validator.GreedyQueryContainmentValidator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.utils.StatContainer;
import vldb18.GreedyPkduckValidator;

public class ValidatorTest {

	@Test
	public void testGreedyPkduckValidator() throws IOException {
		Dataset dataset = DatasetFactory.createInstanceByName("SPROT", "10000");
		long ts;
		long[] tArr = new long[2];
		GreedyPkduckValidator validator0 = new GreedyPkduckValidator(0.0, null);
		
		for ( Record recS : dataset.getSearchedList() ) {
			recS.preprocessApplicableRules();
			for ( Record recT : dataset.getIndexedList() ) {
				ts = System.nanoTime();
				double sim0 = validator0.sim(recS, recT);
				tArr[0] += System.nanoTime() - ts;
				ts = System.nanoTime();
//				double sim1 = validator1.sim(recS, recT);
				tArr[1] += System.nanoTime() - ts;
				
				if ( Records.expandAll(recS).size() > 5 && sim0 >= 0.5 && sim0 < 1) {
					System.out.println(recS.getID()+"\t("+recS.size()+")\t:\t"+recS);
					for ( Record exp : Records.expandAll(recS) ) {
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
	
	@Test
	public void testGreedyQueryContainmentValidator() throws IOException {
		String dataName = "WIKI";
		String size = "10";
		String nr = "10000";
		String qlen = "5";
		DatasetParam param = new DatasetParam(dataName, size, nr, qlen, "1.0");
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		StatContainer stat = new StatContainer();
		GreedyQueryContainmentValidator val = new GreedyQueryContainmentValidator(0, stat);
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("tmp/testGreedyQueryContainmentValidator.txt")));
		
		for ( Record query : dataset.getSearchedList() ) {
			query.preprocessAll();
			for ( Record rec : dataset.getIndexedList() ) {
				rec.preprocessAll();
				double simQ = val.simQuerySide(query, rec);
				double simT = val.simTextSide(query, rec);
				if (simQ >=0.6 || simT >= 0.6) {
					pw.println(query.toStringDetails());
					pw.println(rec.toStringDetails());
					pw.println("simQ="+simQ+"\tsimT="+simT+"\t"+(simQ==simT?"SAME":"DIFF"));
				}
			}
		}
		pw.close();
	}
}
