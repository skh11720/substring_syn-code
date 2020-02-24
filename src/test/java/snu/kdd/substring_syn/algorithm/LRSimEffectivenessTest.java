package snu.kdd.substring_syn.algorithm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.search.AbstractIndexBasedSearch.IndexChoice;
import snu.kdd.substring_syn.algorithm.search.ExactPositionPrefixSearch;
import snu.kdd.substring_syn.algorithm.search.ExactSimWPositionPrefixSearch;
import snu.kdd.substring_syn.algorithm.validator.NaiveValidator;
import snu.kdd.substring_syn.algorithm.validator.NaiveWindowBasedValidator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.StatContainer;

public class LRSimEffectivenessTest {
	/*
	 * Investigate between simL and simW
	 */

	static StatContainer statContainer = new StatContainer();
	static PrintWriter pw;
	
	String[] dataNameArray = {"WIKI", "PUBMED", "AMAZON"};
//	String[] qlenArray = {"1", "3", "5", "7", "9"};
	String[] qlenArray = {"5"};
//	double[] thetaArray = {0.6, 0.7, 0.8, 0.9, 1.0};
	double[] thetaArray = {1.0, 0.9, 0.8, 0.7, 0.6};

	String size = "100000";
	String dataName;
	String qlen;
	String nr;
	ObjectList<Record> searchedList;
	ObjectList<Record> indexedList;
	NaiveValidator val0 = new NaiveValidator(0, null);
	NaiveWindowBasedValidator val1 = new NaiveWindowBasedValidator(0, null);
//	double theta;
	

	@BeforeClass
	public static void setup() throws IOException {
		pw = new PrintWriter(new BufferedWriter(new FileWriter("tmp/LRSimEffectivenessTest.txt", true)));
	}

	@Test
	public void naiveTest() throws IOException {
		Log.disable();
		for ( String dataName : dataNameArray ) {
			this.dataName = dataName;
//			nr = dataName.equals("PUBMED")?"79011":"107836";
			nr = "1000";
			for ( String qlen : qlenArray ) {
				this.qlen = qlen;
				DatasetParam param = new DatasetParam(dataName, size, nr, qlen, "1.0");
				Dataset dataset = DatasetFactory.createInstanceByName(param);
				searchedList = getSearchedList(dataset);
				indexedList = getIndexedList(dataset);
				singleRun();
			}
		}
	}
	
	private ObjectList<Record> getSearchedList( Dataset dataset ) {
		ObjectArrayList<Record> list = new ObjectArrayList<Record>(dataset.getSearchedList().iterator());
		for ( Record rec : list ) rec.preprocessAll();
		return list;
	}

	private ObjectList<Record> getIndexedList( Dataset dataset ) {
		ObjectArrayList<Record> list = new ObjectArrayList<Record>(dataset.getIndexedList().iterator());
		for ( Record rec : list ) rec.preprocessAll();
		return list;
	}


//		NaiveValidator val0 = new NaiveValidator(theta, statContainer);
//		NaiveWindowBasedValidator val1 = new NaiveWindowBasedValidator(theta, statContainer);
	
	public void singleRun() {
		System.out.println(dataName+"\t"+qlen);
		for ( Record query : searchedList ) {
			for ( Record rec : indexedList ) {
				double simT0 = val0.simTextSide(query, rec);
				double simT1 = val1.simTextSide(query, rec);
				if ( simT0 != simT1 ) {
					System.out.printf("%8s%4s%8d%8d%8d%8.3f%8.3f %s\n", dataName, qlen, nr, query.getID(), rec.getID(), simT0, simT1, simT0!=simT1?"*":"");
					pw.printf("%8s%4s%8d%8d%8d%8.3f%8.3f %s\n", dataName, qlen, nr, query.getID(), rec.getID(), simT0, simT1, simT0!=simT1?"*":"");
				}
			}
		}
	}
	
	
	@Test
	public void testUsingPositionPrefixSearch() throws IOException {
//		Log.disable();
		for ( String dataName : dataNameArray ) {
			this.dataName = dataName;
//			nr = dataName.equals("PUBMED")?"79011":"107836";
			nr = "10000";
			for ( String qlen : qlenArray ) {
				this.qlen = qlen;
				DatasetParam param = new DatasetParam(dataName, size, nr, qlen, "1.0");
				Dataset dataset = DatasetFactory.createInstanceByName(param);
				searchedList = getSearchedList(dataset);
				indexedList = getIndexedList(dataset);
				
				for ( double theta : thetaArray ) {
					ExactPositionPrefixSearch alg0 = new ExactPositionPrefixSearch(theta, true, false, IndexChoice.CountPosition);
					ExactSimWPositionPrefixSearch alg1 = new ExactSimWPositionPrefixSearch(theta, true, false, IndexChoice.CountPosition);
					alg0.run(dataset);
					alg1.run(dataset);
					Set<IntPair> rslt0 = alg0.getResultTextSide();
					Set<IntPair> rslt1 = alg1.getResultTextSide();
					rslt0.removeAll(rslt1);
					pw.printf("E\t%s_%s_%s_%.1f\t%d\n", dataName, qlen, nr, theta, rslt1.size());
					for ( IntPair pair : rslt0 ) {
						System.out.printf("%8s%4s%8s%8.1f%8d%8d\n", dataName, qlen, nr, theta, pair.i1, pair.i2);
						pw.printf("N_%s_%s_%s_%.1f\t%d\t%d\n", dataName, qlen, nr, theta, pair.i1, pair.i2);
					}
				}
			}
		}
		pw.flush();
	}
}
