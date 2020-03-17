package snu.kdd.etc;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Random;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.object.indexstore.EntryStore;
import snu.kdd.substring_syn.object.indexstore.IndexStore;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IndexStoreTest {
	
	@Test
	public void test00DataStoreCorrectness() {
		int n = 100;
		ObjectArrayList<DataEntry> entryList = new ObjectArrayList<>(DataEntry.genDataEntries(n, 1000, 1000).iterator());
		DataEntryStore store = new DataEntryStore(entryList);
		store.printDetailStats();

		ObjectListIterator<DataEntry> iter = entryList.iterator();
		for ( DataEntry entry : store.getEntries() ) {
			assertEquals(iter.next(), entry);
		}
		Random rn = new Random();
		for ( int i=0; i<n; ++i ) {
			int idx = rn.nextInt(n);
			assertEquals(entryList.get(idx), store.getEntry(idx));
		}
	}
	
	@Test
	public void test01DataStoreEfficiencyComparedToWithSnappy() {
		/*
		DataEntryStore.construct: 25068.2559
		DataEntryStore.search: 2247.5299
		DataEntryStore.space: 273270053
		DataEntryStore.construct: 24851.6156
		DataEntryStore.search: 2072.4801
		DataEntryStore.space: 272546997
		*/
		int n = 100000;
		long ts;
		ts = System.nanoTime();
		DataEntryStore store0 = new DataEntryStore(DataEntry.genDataEntries(n, 1000, 1000));
		System.out.println("DataEntryStore.construct: "+(System.nanoTime()-ts)/1e6);
		ts = System.nanoTime();
		randomSearchTest(store0, n);
		System.out.println("DataEntryStore.search: "+(System.nanoTime()-ts)/1e6);
		System.out.println("DataEntryStore.space: "+store0.storeSize);

		ts = System.nanoTime();
		DataEntryStore store1 = new DataEntryStoreWithSnappy(DataEntry.genDataEntries(n, 1000, 1000));
		System.out.println("DataEntryStore.construct: "+(System.nanoTime()-ts)/1e6);
		ts = System.nanoTime();
		randomSearchTest(store1, n);
		System.out.println("DataEntryStore.search: "+(System.nanoTime()-ts)/1e6);
		System.out.println("DataEntryStore.space: "+store1.storeSize);
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
		System.out.println("Disk space usage "+istore.diskSpaceUsage());
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
		public int hashCode() {
			return score + name.hashCode() + valueList.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if ( obj == null ) return false;
			DataEntry o = (DataEntry) obj;
			return this.score == o.score && this.name.equals(o.name) && this.valueList.equals(o.valueList);
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
	}

	class DataEntryStoreWithSnappy extends DataEntryStore {

		public DataEntryStoreWithSnappy(Iterable<DataEntry> entryList) {
			super(entryList);
		}
	}
}
