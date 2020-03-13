package snu.kdd.etc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

import org.junit.Test;
import org.xerial.snappy.Snappy;

import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.DiskBasedDataset;
import snu.kdd.substring_syn.data.RecordStore;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.StatContainer;

public class RecordStoreTest {
	
	@Test
	public void storeAndGetRecords() throws IOException {
		StatContainer.global = new StatContainer();
		DatasetParam param = new DatasetParam("WIKI", "10000", "107836", "3", "1.0");
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		ObjectList<Record> recordList = new ObjectArrayList<Record>(dataset.getIndexedList().iterator());
		RecordStore store = new RecordStore(recordList, dataset.ruleset);
		
		for ( Record rec0 : recordList ) {
			int idx = rec0.getIdx();
			Record rec1 = store.getRecord(idx);
			assertTrue( rec1.equals(rec0));
		}

		for ( int i=0; i<dataset.size; ++i ) {
			Record rec0 = recordList.get(i);
			Record rec1 = store.getRawRecord(i);
			assertEquals(rec0.getIdx(), rec1.getIdx());
			assertEquals(rec0, rec1);
		}
		
		ObjectListIterator<Record> iter = recordList.iterator();
		for ( Record rec1 : store.getRecords() ) {
			Record rec0 = iter.next();
			rec0.preprocessApplicableRules();
			rec0.preprocessSuffixApplicableRules();
			rec0.getMaxRhsSize();
			boolean[] b = RecordSerializationTest.checkEquivalence(rec0, rec1);
			assertTrue(BooleanArrayList.wrap(b).stream().allMatch(b0 -> b0));
		}
	}
	
	@Test
	public void recordStoreIterator() throws IOException {
		/*
		 * recordStore.getRecords():	DatasetParam(AMAZON, 100000, 107836, 3, 1.0)	2037.6584 ms
		 */
		DatasetParam param = new DatasetParam("AMAZON", "1000000", "107836", "3", "1.0");
		Dataset dataset = DatasetFactory.createInstanceByName(param);

		Log.log.trace("recordStoreIterator(): start iteration");
		long ts = System.nanoTime();
		for ( Record rec : dataset.getIndexedList() ) {
			Log.log.trace("recordStoreIterator(): get record "+rec.getIdx());
		}
		System.out.println("recordStore.getRecords():\t"+param.toString()+"\t"+(System.nanoTime()-ts)/1e6+" ms");
	}

	@Deprecated
	@SuppressWarnings("unused")
	@Test
	public void recordIOWithSnappy() throws IOException {
		Random rn = new Random();
		int maxlen = 0;
		DatasetParam param = new DatasetParam("WIKI", "10000", "107836", "3", "1.0");
		Dataset dataset = DatasetFactory.createInstanceByName(param);
		IntList posList = new IntArrayList();
		int n = 0;
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("./tmp/RecordStoreTest"));
		int cur = 0;
		for ( Record rec : dataset.getIndexedList() ) {
			posList.add(cur);
			byte[] b = Snappy.compress(rec.getTokenArray());
			cur += b.length;
			maxlen = Math.max(maxlen, b.length);
			bos.write(b);
			++n;
		}
		posList.add(cur);
		bos.close();
		System.out.println("max(b.length)="+maxlen);
		
		long t;
		int retries = 1000000;

		t = System.nanoTime();
		RandomAccessFile raf = new RandomAccessFile("./tmp/RecordStoreTest", "r");
		byte[] b = new byte[378];
		for ( int i=0; i<retries; ++i ) {
			int idx = rn.nextInt(n);
			raf.seek(posList.get(idx));
			raf.read(b);
			int[] tokens = Snappy.uncompressIntArray(b, 0, posList.get(idx+1)-posList.get(idx));
		}
		raf.close();
		System.out.println("recordIOWithSnappy: "+(System.nanoTime()-t)/1e6);
	}

	@Test
	public void getRawRecordTest() throws IOException {
		DatasetParam param = new DatasetParam("WIKI", "30", "107836", "3", "1.0");
		DiskBasedDataset dataset = (DiskBasedDataset)DatasetFactory.createInstanceByName(param);
		for ( int i=0; i<30; ++i ) {
			Record rec = dataset.getRawRecord(i);
			System.out.println(rec.getIdx()+"\t"+rec.getID()+"\t"+rec.toOriginalString());
		}
	}

	@Test
	public void iterableSntTest() throws IOException {
		DatasetParam param = new DatasetParam("WIKI", "30", "107836", "3", "1.0");
		DiskBasedDataset dataset = (DiskBasedDataset)DatasetFactory.createInstanceByName(param);
		for ( int i=0; i<30; ++i ) {
			Record rec = dataset.getRecord(i);
			System.out.println(rec.getIdx()+"\t"+rec.getID()+"\t"+rec.toOriginalString());
		}
	}
	
	@Test
	public void iterableDocTest() throws IOException {
		DatasetParam param = new DatasetParam("WIKI-DOC", "30", "107836", "3", "1.0");
		DiskBasedDataset dataset = (DiskBasedDataset)DatasetFactory.createInstanceByName(param);
		for ( int i=0; i<30; ++i ) {
			Record rec = dataset.getRecord(i);
//			System.out.println(rec.getIdx()+"\t"+rec.getID()+"\t"+rec.toOriginalString());
			System.out.println(dataset.getRid2idpairMap().get(rec.getIdx())+"\t"+rec.toOriginalString());
		}
	}
}
