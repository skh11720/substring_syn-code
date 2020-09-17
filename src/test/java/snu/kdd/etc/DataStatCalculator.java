package snu.kdd.etc;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetInfo;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;

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
				"WIKI,10000000,107836",
				"PUBMED,"+DatasetInfo.getMaxSize("PUBMED")+",79011",
				"AMAZON,10000000,107836"
//				"WIKI-DOC,"+DatasetInfo.getMaxSize("WIKI-DOC")+",107836",
//				"PUBMED-DOC,"+DatasetInfo.getMaxSize("PUBMED-DOC")+",79011",
//				"AMAZON-DOC,"+DatasetInfo.getMaxSize("AMAZON-DOC")+",107836"
				};
		for ( int i=0; i<targetList.length; ++i ) {
			String[] tokens =  targetList[i].split(",");
			DatasetParam param = new DatasetParam(tokens[0], tokens[1], tokens[2], "3", "1.0", "-1");
			Dataset dataset = DatasetFactory.createInstanceByName(param);
			extractStat(dataset);
		}
	}
	
	public static void extractStat( Dataset dataset ) {
		DatasetStat dataStat = new DatasetStat();
		countDocument(dataset, dataStat);
		countRecord(dataset, dataStat);
		countToken(dataset, dataStat);
		countNumRule(dataset, dataStat);
		countLenRecords(dataset, dataStat);
		countNumApplicableRules(dataset, dataStat);
		countLenRHS(dataset, dataStat);
		writeResult(dataset, dataStat);
	}
	
	public static void writeResult( Dataset dataset, DatasetStat dataStat ) {
		ps.println(String.format("%s\t%s", dataset.name, dataStat));
	}
	
	public static void countDocument(Dataset dataset, DatasetStat dataStat) {
		if ( !dataset.isDocInput() ) return;
		Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
		for ( IntPair val : dataset.getRid2idpairMap().values() ) {
			counter.addTo(val.i1, 1);
		}
		dataStat.nDoc = counter.keySet().size();

		long n = 0;
		long sum = 0;
		long sqsum = 0;
		for ( int nSnt : counter.values() ) {
			dataStat.nSnt.max = Math.max(dataStat.nSnt.max, nSnt);
			dataStat.nSnt.min = Math.min(dataStat.nSnt.min, nSnt);
			++n;
			sum += nSnt;
			sqsum += nSnt*nSnt;
		}
		double mean = 1.0*sum/n;
		dataStat.nSnt.avg = mean;
		dataStat.nSnt.std = Math.sqrt(1.0*sqsum/n - mean*mean);
	}
	
	@SuppressWarnings("unused")
	public static void countRecord(Dataset dataset, DatasetStat dataStat) {
		for ( TransformableRecordInterface rec : dataset.getIndexedList() ) {
			dataStat.nRecord += 1;
		}
	}

	public static void countToken( Dataset dataset, DatasetStat dataStat ) {
		IntSet tokenSet = new IntOpenHashSet();
		for ( TransformableRecordInterface rec : dataset.getIndexedList() ) {
			tokenSet.addAll(rec.getTokenList());
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
		for ( TransformableRecordInterface rec : dataset.getIndexedList() ) {
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
		for ( TransformableRecordInterface rec : dataset.getIndexedList() ) {
			rec.preprocessApplicableRules();
			int nr = rec.getNumApplicableNonselfRules(); // do not count the self rules
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
	
	public static void countLenRHS( Dataset dataset, DatasetStat dataStat ) {
		long n = 0;
		long sum = 0;
		long sqsum = 0;
		for ( Rule rule : dataset.ruleset.get() ) {
			if ( rule.isSelfRule ) continue;
			dataStat.lenRHS.max = Math.max(dataStat.lenRHS.max, rule.rhsSize());
			dataStat.lenRHS.min = Math.min(dataStat.lenRHS.min, rule.rhsSize());
			++n;
			sum += rule.rhsSize();
			sqsum += rule.rhsSize() * rule.rhsSize();
		}
		double mean = 1.0*sum/n;
		double std = Math.sqrt(1.0*sqsum/n - mean*mean);
		dataStat.lenRHS.avg = mean;
		dataStat.lenRHS.std = std;
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
		int nDoc = 0;
		int nRecord = 0;
		int nToken = 0;
		int nRule = 0;
		StatTuple nSnt = new StatTuple();
		StatTuple len = new StatTuple();
		StatTuple nApp = new StatTuple();
		StatTuple lenRHS = new StatTuple();
		
		@Override
		public String toString() {
			return String.format("%d\t%d\t%d\t%d\t%s\t%s\t%s\t%s", nDoc, nRecord, nToken, nRule, nSnt, len, nApp, lenRHS);
		}
	}
}
