package snu.kdd.etc;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.junit.Test;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.ACAutomataS;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetInfo;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Log;

public class AutomataTest {
	
	@Test
	public void testCorrectnessAutomataS() throws IOException {
		DatasetParam param = new DatasetParam("WIKI", "100000", "107836", "5", "1.0", "0");
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		
		RecordBasedNARIterator iter0 = new RecordBasedNARIterator(dataset);		

		String rulePath = DatasetInfo.getRulePath(param.name, param.nr);
		BufferedReader br = new BufferedReader(new FileReader(rulePath));
		ObjectList<String> ruleStrList = new ObjectArrayList<>(br.lines().iterator());
		
		long ts = System.nanoTime();
		ACAutomataS ac = new ACAutomataS(ruleStrList);
		Log.log.info("build ACAutomataS: "+(System.nanoTime()-ts)/1e6);
		br.close();

		String indexedPath = DatasetInfo.getIndexedPath(param.name);
		AutomataSBasedNARIterator iter1 = new AutomataSBasedNARIterator(indexedPath, param.size, ac);
	
		while ( iter0.hasNext() && iter1.hasNext() ) {
			int n0 = iter0.next();
			int n1 = iter1.next();
			try {
				assertEquals(n0, n1);
			}
			catch ( AssertionError e ) {
				System.err.println("iter0.rec: "+iter0.rec.toStringDetails());
				System.err.println("iter1.rec: "+Arrays.toString(iter1.rec));
				ac.printApplicableRules(iter1.rec);
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	class RecordBasedNARIterator implements Iterator<Integer> {
		
		final Iterator<Record> iter;
		Record rec;
		
		public RecordBasedNARIterator(Dataset dataset) {
			iter = dataset.getIndexedList().iterator();
		}

		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public Integer next() {
			rec = iter.next();
			return rec.getNumApplicableNonselfRules();
		}
	}
	
	class AutomataSBasedNARIterator implements Iterator<Integer> {
		final BufferedReader br;
		final Iterator<String> iter;
		final ACAutomataS ac;
		String[] rec;
		
		public AutomataSBasedNARIterator(String indexedPath, String size, ACAutomataS ac) throws FileNotFoundException {
			br = new BufferedReader(new FileReader(indexedPath));
			iter = br.lines().limit(Integer.parseInt(size)).iterator();
			this.ac = ac;
		}

		@Override
		public boolean hasNext() {
			return iter.hasNext();
		}

		@Override
		public Integer next() {
			rec = iter.next().split(" ");
			return ac.getNumApplicableRules(rec);
		}
	}
}
