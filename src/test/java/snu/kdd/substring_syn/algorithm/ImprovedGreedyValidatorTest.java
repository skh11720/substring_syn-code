package snu.kdd.substring_syn.algorithm;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import snu.kdd.substring_syn.algorithm.validator.GreedyValidator;
import snu.kdd.substring_syn.algorithm.validator.ImprovedGreedyValidator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.StatContainer;

public class ImprovedGreedyValidatorTest {
	
	@Test
	public void test() throws IOException {
		/*
				val0		val1
		nPair	720			720
		t_Query	3701.598	2718.240
		t_Text	4791.618	3483.467
		 */
		DatasetParam param = new DatasetParam("WIKI", "10000", "107836", "5", "1.0");
		double theta = 0.6;
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		
		StatContainer stat0 = new StatContainer();
		GreedyValidator val0 = new GreedyValidator(theta, stat0);
		StatContainer stat1 = new StatContainer();
		ImprovedGreedyValidator val1 = new ImprovedGreedyValidator(theta, stat1);
		for ( Record query : dataset.getSearchedList() ) {
			query.preprocessAll();
			for ( Record rec : dataset.getIndexedList() ) {
				stat0.startWatch("t_Query");
				double simQ0 = val0.simQuerySide(query, rec);
				stat0.stopWatch("t_Query");
				stat0.startWatch("t_Text");
				double simT0 = val0.simTextSide(query, rec);
				stat0.stopWatch("t_Text");
				double sim0 = Math.max(simQ0, simT0);
				if (sim0 >= theta) stat0.increment("nPair");

				stat1.startWatch("t_Query");
				double simQ1 = val1.simQuerySide(query, rec);
				stat1.stopWatch("t_Query");
				stat1.startWatch("t_Text");
				double simT1 = val1.simTextSide(query, rec);
				stat1.stopWatch("t_Text");
				double sim1 = Math.max(simQ1, simT1);
				if (sim1 >= theta) stat1.increment("nPair");
				if (sim0 < theta && sim1 >= theta) {
					System.out.println("query="+query);
					System.out.println("rec="+rec);
					System.out.println("simQ0="+simQ0);
					System.out.println("simQ1="+simQ1);
					System.out.println("simT0="+simT0);
					System.out.println("simT1="+simT1);
				}

//				if ( simQ0 == 0 && simQ1 ==0 && simT0 == 0 && simT1 ==0 ) continue;
//				System.out.println(String.format("%.3f\t%.3f\t%.3f\t%.3f", simQ0, simQ1, simT0, simT1));
				
//				if (simT0 > simT1) outputDetailsTextSide(query, rec, val0, val1);
//
//				assertTrue(simQ0 <= simQ1);
//				assertTrue(simT0 <= simT1);
			}
		}
		stat0.finalize();
		stat1.finalize();
		
		System.out.println("\tval0\tval1");
		for (String key : new String[] {"nPair", "t_Query", "t_Text"} ) {
			System.out.print(key);
			for ( StatContainer stat : new StatContainer[] {stat0, stat1} ) {
				System.out.print("\t"+stat.getStat(key));
			}
			System.out.println();
		}
	}
	
	public void outputDetailsTextSide(Record query, Record rec, GreedyValidator val0, ImprovedGreedyValidator val1) {
		double simT0 = val0.simTextSide(query, rec);
		int[] trans0 = val0.getTransform(rec, query);
		double simT1 = val1.simTextSide(query, rec);
		int[] trans1 = val1.getTransform(rec, query);
		System.out.println("query="+query);
		System.out.println("rec="+rec);
		System.out.println("trans0="+Arrays.toString(trans0));
		System.out.println("simT0="+simT0);
		System.out.println("trans1="+Arrays.toString(trans1));
		System.out.println("simT1="+simT1);
	}
}
