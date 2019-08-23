package snu.kdd.etc;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.algorithm.index.disk.InvertedListPool;
import snu.kdd.substring_syn.algorithm.index.disk.NaiveIndexStore;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;

public class InvertedListPoolTest {

	@Test
	public void putSpeed() throws IOException {
		Dataset dataset = Dataset.createInstanceByName("WIKI_3", "10000");
		ObjectList<Record> recordList = new ObjectArrayList<Record>(dataset.getIndexedList().iterator());
		NaiveIndexStore store = new NaiveIndexStore(recordList);
		IntSet tokenSet = new IntOpenHashSet();
		for ( int i=0; i<10000; ++i ) tokenSet.addAll( IntArrayList.wrap(recordList.get(i).getTokenArray()) );
		
		InvertedListPool<Integer> pool = new InvertedListPool<>();
		
//		System.out.println(String.format("size: %d/%d", pool.size(), pool.capacity()));
//		System.out.println("buffered keys: "+pool.getKeys());
		
		long ts;
		long t = 0;
		for ( int token : tokenSet ) {
//			System.out.println("----------------------------------------------------");
			ObjectList<Integer> list0 = new ObjectArrayList<>(store.getInvList(token));
//			System.out.println("key: "+token+", list.size: "+list0.size());
			ts = System.nanoTime();
			pool.put(token, list0);
			t += (System.nanoTime() - ts);
//			System.out.println(String.format("size: %d/%d", pool.size(), pool.capacity()));
//			System.out.println("buffered keys: "+pool.getKeys());
			ObjectList<Integer> list1 = pool.get(token);
			assertTrue(list0.equals(list1));
		}
		
		System.out.println( "average time for put(): "+t/1e3/tokenSet.size() +" us");
	}

}
