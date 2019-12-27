package snu.kdd.etc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Random;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.xerial.snappy.Snappy;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.object.indexstore.EntryStore;
import snu.kdd.substring_syn.object.indexstore.IndexStore;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IndexStoreTest {
	
	@Test
	public void test00DataStoreCorrectness() {
		int n = 5;
		DataEntryStore store = new DataEntryStoreWithSnappy(DataEntry.genDataEntries(n, 1000, 1000));
		for ( DataEntry entry : store.getEntries() ) {
			System.out.println(entry);
		}
		
		System.out.println(n/2+"\t"+store.getEntry(n/2));
	}
	
	@Test
	public void test01DataStoreEfficiencyComparedToWithSnappy() {
		/*
		DataEntryStore.construct: 26611.7664
		DataEntryStore.search: 2377.2701
		DataEntryStore.construct: 26986.5987
		DataEntryStore.search: 2516.4783
		*/
		int n = 100000;
		long ts;
		ts = System.nanoTime();
		DataEntryStore store0 = new DataEntryStore(DataEntry.genDataEntries(n, 1000, 1000));
		System.out.println("DataEntryStore.construct: "+(System.nanoTime()-ts)/1e6);
		ts = System.nanoTime();
		randomSearchTest(store0, n);
		System.out.println("DataEntryStore.search: "+(System.nanoTime()-ts)/1e6);

		ts = System.nanoTime();
		DataEntryStore store1 = new DataEntryStoreWithSnappy(DataEntry.genDataEntries(n, 1000, 1000));
		System.out.println("DataEntryStore.construct: "+(System.nanoTime()-ts)/1e6);
		ts = System.nanoTime();
		randomSearchTest(store1, n);
		System.out.println("DataEntryStore.search: "+(System.nanoTime()-ts)/1e6);
	}
	
	@Test
	public void test02DataStoreEfficiency() {
		/*
		DataEntryStore.construct: 26670.9548
		DataEntryStore.search: 2268.213

		 */
		int n = 100000;
		long ts;
		ts = System.nanoTime();
		DataEntryStore store0 = new DataEntryStore(DataEntry.genDataEntries(n, 1000, 1000));
		System.out.println("DataEntryStore.construct: "+(System.nanoTime()-ts)/1e6);
		ts = System.nanoTime();
		randomSearchTest(store0, n);
		System.out.println("DataEntryStore.search: "+(System.nanoTime()-ts)/1e6);
	}

	@Test
	public void test03IndexStoreCorrectness() {
		int n = 10;
		DataEntryStore store = new DataEntryStore(DataEntry.genDataEntries(n, 10, 10));
		for ( int i=0; i< store.size(); ++i ) System.out.println(i+"\t"+store.getEntry(i));
		IndexStore<DataEntry> istore = new DataEntryIndexStore(store);
		for ( int key=0; key<30; ++key ) {
			System.out.println(istore.getInvList(key));
			for ( DataEntry e : istore.getEntries(key) ) {
				System.out.println(key+"\t"+e);
			}
		}
	}
	
	public void randomSearchTest(DataEntryStore store, int tries) {
		int n = store.size();
		Random rn = new Random();
		for ( int j=0; j<tries; ++j ) {
			int i = rn.nextInt(n);
			DataEntry entry = store.getEntry(i);
			entry.hashCode();
		}
	}
	
	static class DataEntry implements Serializable {

		static Random rn = new Random(0);

		public static DataEntry genDataEntry(int slenMax, int llenMax) {
			int score = rn.nextInt(100);
			int slen = rn.nextInt(slenMax);
			StringBuilder strbld = new StringBuilder();
			for ( int i=0; i<slen; ++i ) strbld.append(String.format("%c", rn.nextInt(26)+96));
			String name = strbld.toString();
			IntList valueList = new IntArrayList();
			int llen = rn.nextInt(llenMax);
			for ( int i=0; i<llen; ++i ) valueList.add(rn.nextInt(20));
			return new DataEntry(score, name, valueList);
		}
		
		public static Iterable<DataEntry> genDataEntries(int n, int slenMax, int llenMax) {
			return new Iterable<IndexStoreTest.DataEntry>() {
				
				@Override
				public Iterator<DataEntry> iterator() {
					return new Iterator<IndexStoreTest.DataEntry>() {
						
						int i = 0;
						
						@Override
						public DataEntry next() {
							i += 1;
							return genDataEntry(slenMax, llenMax);
						}
						
						@Override
						public boolean hasNext() {
							return i < n;
						}
					};
				}
			};
		}

		private static final long serialVersionUID = 32985472394823L;
		public final int score;
		public final String name;
		public final IntList valueList;
		
		public DataEntry(int score, String name, IntList valueList ) {
			this.score = score;
			this.name = name;
			this.valueList = valueList;
		}
		
		@Override
		public String toString() {
			return String.format("(%d, %s, %s)", score, name, valueList);
		}
	}
	
	class DataEntryIndexStore extends IndexStore<DataEntry> {

		public DataEntryIndexStore(EntryStore<DataEntry> store) {
			super(store);
		}

		@Override
		protected Iterable<IntPair> getKvList(Iterable<DataEntry> entryList) {
			ObjectSet<IntPair> kvList = new ObjectOpenHashSet<>();
			int i = 0;
			for ( DataEntry e : entryList ) {
				for ( int key : e.valueList ) kvList.add(new IntPair(key, i));
				i += 1;
			}
			return kvList;
		}
		
	}
	
	class DataEntryStore extends EntryStore<DataEntry> {

		public DataEntryStore(Iterable<DataEntry> entryList) {
			super(entryList, "EntryStore");
		}

		@Override
		protected byte[] serialize(DataEntry entry) {
			ByteArrayOutputStream bos = null;
			try {
				bos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(bos);
				oos.writeObject(entry);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			return bos.toByteArray();
		}

		@Override
		protected DataEntry deserialize(byte[] buf, int offset, int length) {
			ByteArrayInputStream bis = new ByteArrayInputStream(buf, offset, length);
			DataEntry entry = null;
			try {
				ObjectInputStream ois = new ObjectInputStream(bis);
				entry = (DataEntry) ois.readObject();
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			return entry;
		}
	}

	class DataEntryStoreWithSnappy extends DataEntryStore {

		public DataEntryStoreWithSnappy(Iterable<DataEntry> entryList) {
			super(entryList);
		}

		@Override
		protected byte[] serialize(DataEntry entry) {
			ByteArrayOutputStream bos = null;
			ObjectOutputStream oos = null;
			bos = new ByteArrayOutputStream();
			try {
				oos = new ObjectOutputStream(bos);
				oos.writeObject(entry);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			byte[] buf = null;
			try {
				buf = Snappy.compress(bos.toByteArray());
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			return buf;
		}

		@Override
		protected DataEntry deserialize(byte[] buf, int offset, int length) {
			DataEntry entry = null;
			try {
				int bo_len = Snappy.uncompressedLength(buf);
				byte[] buf_out = new byte[bo_len];
				Snappy.uncompress(buf, offset, length, buf_out, 0);
				ByteArrayInputStream bis = new ByteArrayInputStream(buf_out, 0, bo_len);
				ObjectInputStream ois = new ObjectInputStream(bis);
				entry = (DataEntry) ois.readObject();
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			return entry;
		}
	}
}
