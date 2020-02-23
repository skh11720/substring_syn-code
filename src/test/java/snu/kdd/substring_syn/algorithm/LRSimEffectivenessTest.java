package snu.kdd.substring_syn.algorithm;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.BeforeClass;
import org.junit.Test;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.validator.NaiveValidator;
import snu.kdd.substring_syn.algorithm.validator.NaiveWindowBasedValidator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.StatContainer;

public class LRSimEffectivenessTest {
	/*
	 * Investigate between simL and simW
	 */

	static StatContainer statContainer = new StatContainer();
	static PrintStream ps;
	
	String[] dataNameArray = {"WIKI", "PUBMED", "AMAZON"};
//	String[] qlenArray = {"1", "3", "5", "7", "9"};
	String[] qlenArray = {"5"};
//	double[] thetaArray = {0.6, 0.7, 0.8, 0.9, 1.0};

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
	public static void setup() throws FileNotFoundException {
		ps = new PrintStream("tmp/LRSimEffectivenessTest.txt");
	}

	@Test
	public void test() throws IOException {
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
					ps.printf("%8s%4s%8d%8d%8d%8.3f%8.3f %s\n", dataName, qlen, nr, query.getID(), rec.getID(), simT0, simT1, simT0!=simT1?"*":"");
				}
			}
		}
	}
}
