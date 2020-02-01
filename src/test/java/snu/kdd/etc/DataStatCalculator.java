package snu.kdd.etc;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetInfo;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;

public class DataStatCalculator {
	
	static PrintStream ps;
	
	static {
		try {
			ps = new PrintStream(new FileOutputStream("tmp/DataStat.txt", true));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	@Test
	public void test() throws IOException {
		String[] targetList = {
				"WIKI,"+DatasetInfo.getMaxSize("WIKI")+",107836",
				"PUBMED,"+DatasetInfo.getMaxSize("PUBMED")+",79011",
				"AMAZON,"+DatasetInfo.getMaxSize("AMAZON")+",107836"
				};
		for ( int i=0; i<targetList.length; ++i ) {
			String[] tokens =  targetList[i].split(",");
			DatasetParam param = new DatasetParam(tokens[0], tokens[1], tokens[2], "3", "1.0");
			Dataset dataset = DatasetFactory.createInstanceByName(param);
			extractStat(dataset);
		}
	}
	
	public static void extractStat( Dataset dataset ) {
		DatasetStat dataStat = new DatasetStat();
		countToken(dataset, dataStat);
		countNumRule(dataset, dataStat);
		countLenRecords(dataset, dataStat);
		countNumApplicableRules(dataset, dataStat);
		writeResult(dataset, dataStat);
	}
	
	public static void writeResult( Dataset dataset, DatasetStat dataStat ) {
		ps.println(String.format("%s\t%s", dataset.name, dataStat));
	}

	public static void countToken( Dataset dataset, DatasetStat dataStat ) {
		IntSet tokenSet = new IntOpenHashSet();
		for ( Record rec : dataset.getIndexedList() ) {
			tokenSet.addAll(rec.getTokens());
		}
		dataStat.nToken = tokenSet.size();
	}
	
	public static void countNumRule( Dataset dataset, DatasetStat dataStat ) {
		dataStat.nRule = 0;
		for ( Rule rule : dataset.ruleset.get() ) {
			if ( !rule.isSelfRule ) dataStat.nRule++;
		}
	}
	
	public static void countLenRecords( Dataset dataset, DatasetStat dataStat ) {
		long n = 0;
		long sum = 0;
		long sqsum = 0;
		for ( Record rec : dataset.getIndexedList() ) {
			dataStat.len.max = Math.max(dataStat.len.max, rec.size());
			dataStat.len.min = Math.min(dataStat.len.min, rec.size());
			++n;
			sum += rec.size();
			sqsum += rec.size()*rec.size();
		}
		double mean = 1.0*sum/n;
		double std = Math.sqrt(1.0*sqsum/n - mean*mean);
		dataStat.len.avg = mean;
		dataStat.len.std = std;
	}
	
	public static void countNumApplicableRules( Dataset dataset, DatasetStat dataStat ) {
		long n = 0;
		long sum = 0;
		long sqsum = 0;
		for ( Record rec : dataset.getIndexedList() ) {
			rec.preprocessApplicableRules();
			int nr = rec.getNumApplicableRules(); // do not count the self rules
			dataStat.nApp.max = Math.max(dataStat.nApp.max, nr);
			dataStat.nApp.min = Math.min(dataStat.nApp.min, nr);
			++n;
			sum += nr;
			sqsum += nr*nr;
		}
		double mean = 1.0*sum/n;
		double std = Math.sqrt(1.0*sqsum/n - mean*mean);
		dataStat.nApp.avg = mean;
		dataStat.nApp.std = std;
	}
	
	static class StatTuple {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		double avg = 0;
		double std = 0;
		
		@Override
		public String toString() {
			return String.format("%d\t%d\t%.3f\t%.3f", min, max, avg, std);
		}
	}
	
	static class DatasetStat {
		int nToken = 0;
		int nRule = 0;
		StatTuple len = new StatTuple();
		StatTuple nApp = new StatTuple();
		
		@Override
		public String toString() {
			return String.format("%d\t%d\t%s\t%s", nToken, nRule, len, nApp);
		}
	}
}
