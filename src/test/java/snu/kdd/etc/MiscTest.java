package snu.kdd.etc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayPriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.DatasetFactory;
import snu.kdd.substring_syn.data.DatasetInfo;
import snu.kdd.substring_syn.data.DatasetParam;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.Substring;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.FileBasedLongList;
import snu.kdd.substring_syn.utils.Int2IntBinaryHeap;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.iterator.SortedRecordSlidingWindowIterator;
import vldb18.PkduckDP;

public class MiscTest {
	
	@Test
	public void testExactVerification() throws IOException {
		int qidx = 23;
		int sidx = 287;
		Dataset dataset = DatasetFactory.createInstanceByName("WIKI_3", "10000");
		Record query = null;
		TransformableRecordInterface text = null;
		for ( Record rec : dataset.getSearchedList() ) {
			if ( rec.getIdx() == qidx ) {
				query = rec;
				break;
			}
		}
		
		for ( TransformableRecordInterface rec : dataset.getIndexedList() ) {
			if ( rec.getIdx() == sidx ) {
				text = rec;
				break;
			}
		}
		
		System.out.println(query.toOriginalString());
		System.out.println(text.toOriginalString());
	}
	
	@SuppressWarnings("unused")
	@Test
	public void testRecord() throws IOException {
		Dataset dataset = DatasetFactory.createInstanceByName("WIKI_3", "10000");
//		Record rec = dataset.searchedList.get(1);
		for ( TransformableRecordInterface rec : dataset.getIndexedList() ) {
			int n1 = 0;
			for ( int k=0; k<rec.size(); ++k ) {
				for ( Rule rule : rec.getApplicableRules(k) ) ++n1;
			}
			
			int n2 = 0;
			for ( Rule rule : rec.getApplicableRuleIterable() ) ++n2;
			
			assertEquals(n1, n2);
		}
	}
	
	@Test
	public void testTransformLength() throws IOException {
		Dataset dataset = DatasetFactory.createInstanceByName("WIKI_3", "10000");
		for ( Record rec : dataset.getSearchedList() ) {
			System.out.println(rec.getIdx()+"\t"+rec.getMinTransLength()+"\t"+rec.getMaxTransLength());
		}
	}

	@Test
	public void testQueryCandTokenSet() throws IOException {
		Dataset dataset = DatasetFactory.createInstanceByName("WIKI_3", "10000");
		for ( Record rec : dataset.getSearchedList() ) {
			System.out.println(rec.getIdx()+"\t"+(new IntArrayList(rec.getCandTokenSet().stream().sorted().iterator())));
		}
	}
	
	protected IntSet getExpandedPrefix( Record query, double theta ) {
		IntSet candTokenSet = query.getCandTokenSet();
		IntSet expandedPrefix = new IntOpenHashSet();
		PkduckDP pkduckdp = new PkduckDP(query, theta);
		for ( int target : candTokenSet ) {
			if ( pkduckdp.isInSigU(target) ) expandedPrefix.add(target);
		}
		return expandedPrefix;
	}
	
	protected IntSet getExpandedPrefix2( Record rec, double theta ) {
		IntSet expandedPrefix = new IntOpenHashSet();
		for ( Record exp : Records.expandAll(rec) ) {
			System.out.println("exp: "+exp);
			System.out.println("exp.prefix: "+Util.getPrefixLength(exp, theta));
			expandedPrefix.addAll(Util.getPrefix(exp, theta));
		}
		return expandedPrefix;
	}
	
	@Test
	public void testWindowCount() throws IOException {
		Dataset dataset = DatasetFactory.createInstanceByName("SPROT_long", "1000");
		double theta = 0.6;
		for ( TransformableRecordInterface rec : dataset.getIndexedList() ) {
			int nw0 = sumWindowSize(rec);
			int nw1 = 0;
			for ( int w=1; w<=rec.size(); ++w ) {
				SortedRecordSlidingWindowIterator witer = new SortedRecordSlidingWindowIterator(rec, w, theta);
				while ( witer.hasNext() ) {
					Subrecord window = witer.next();
					nw1 += window.size();
				}
			}
//			System.out.println(rec.getID()+"\t"+rec.size()+"\t"+nw0+"\t"+nw1);
			assertEquals(nw0, nw1);
		}
	}
	
	private static int sumWindowSize( TransformableRecordInterface rec ) {
		int n = rec.size();
		return n*(n+1)*(n+1)/2 - n*(n+1)*(2*n+1)/6;
	}
	
	@Test
	public void testRangeArray() {
		int n = 10;
		int[][] arr2d = new int[n][n];
		Random rn = new Random();
		for ( int i=0; i<n; ++i ) {
			for ( int j=0; j<n; ++j ) {
				arr2d[i][j] = rn.nextInt(10);
			}
		}
		
		UpperTri2dArray rangeArr = new UpperTri2dArray(n);
		for ( int i=0; i<n; ++i ) {
			for ( int j=i; j<n; ++j ) {
				rangeArr.set(i, j, arr2d[i][j]);
			}
		}

		for ( int i=0; i<n; ++i ) {
			for ( int j=i; j<n; ++j ) {
				System.out.println(i+", "+j+" : "+arr2d[i][j]+"\t"+rangeArr.get(i, j));
				assertEquals(arr2d[i][j], rangeArr.get(i, j));
			}
		}
	}
	
	class UpperTri2dArray {
		final int n;
		final int size;
		final int[] cell;
		
		public UpperTri2dArray( int n ) {
			this.n = n;
			size = n*(n+1)/2;
			cell = new int[size];
		}
		
		public int get( int i, int j ) {
			return cell[getPos(i, j)];
		}
		
		public void set( int i, int j, int val ) {
			cell[getPos(i, j)] = val;
		}

		private int getPos( int i, int j ) {
			// i, j are 0-based
			return size - (n-i)*(n-i+1)/2 - i + j;
		}
	}
	
//	@Test
//	public void testIntDoubleList() {
//		Int2DoubleMap counter = new Int2DoubleOpenHashMap();
//		int n = 10;
//		Random rn = new Random(0);
//		for ( int i=0; i<n; ++i ) counter.put(i, rn.nextDouble());
//		IntDoubleList list = new IntDoubleList(counter);
//		System.out.println(list);
//		
//		for ( int i=0; i<n; ++i ) {
//			int k = rn.nextInt(n+n/2);
//			double dv = rn.nextDouble();
//			System.out.println(k+", "+dv);
//			list.update(k, dv);
//			System.out.println(list);
//		}
//	}
//
//	class IntDoubleList {
//		Int2IntMap key2posMap = null;
//		List<IntDouble> list = null;
//		
//		public IntDoubleList() {
//			list = new ObjectArrayList<>();
//			key2posMap = new Int2IntOpenHashMap();
//		}
//		
//		public IntDoubleList( Int2DoubleMap counter ) {
//			this();
//			for ( Int2DoubleMap.Entry entry : counter.int2DoubleEntrySet() ) {
//				list.add( new IntDouble(entry.getIntKey(), entry.getDoubleValue()) );
//			}
//			list.sort(IntDouble.comp);
//			for ( int i=0; i<list.size(); ++i ) key2posMap.put(list.get(i).k, i);
//		}
//		
//		public void update( int k, double dv ) {
//			if ( key2posMap.containsKey(k) ) updateExistingKey(k, dv);
//			else updateNewKey(k, dv);
//		}
//		
//		private void updateExistingKey( int k, double dv ) {
//			int i = key2posMap.get(k);
//			IntDouble entry = list.get(i);
//			entry.v += dv;
//			updatePosition(entry, i);
//		}
//		
//		private void updateNewKey( int k, double dv ) {
//			IntDouble entry = new IntDouble(k, dv);
//			list.add(entry);
//			int i = list.size()-1;
//			key2posMap.put(k, i);
//			updatePosition(entry, i);
//		}
//		
//		private void updatePosition( IntDouble entry, int i ) {
//			while ( i > 0 ) {
//				if ( list.get(i-1).v < entry.v ) swap(i-1, i);
//				else break;
//				--i;
//			}
//		}
//		
//		private void swap( int i, int j ) {
//			key2posMap.put(list.get(i).k, j);
//			key2posMap.put(list.get(j).k, i);
//			IntDouble tmp = list.get(i);
//			list.set(i, list.get(j));
//			list.set(j, tmp);
//		}
//		
//		public String toString() {
//			StringBuilder strbld = new StringBuilder();
//			for ( IntDouble entry : list ) strbld.append(key2posMap.get(entry.k)+":"+entry+", ");
//			return strbld.toString();
//		}
//	}
	
	@Test
	public void testMergeSortedIntLIsts() {
		Random rn = new Random();
		int m = rn.nextInt(5)+2;
		ObjectList<IntList> intLists = new ObjectArrayList<>();
		for ( int i=0; i<m; ++i ) {
			IntList list = new IntArrayList();
			int n = rn.nextInt(10)+1;
			for ( int j=0; j<n; ++j ) list.add(rn.nextInt(1000));
			list.sort(Integer::compare);
			intLists.add(list);
		}
		
		for ( int i=0; i<m; ++i ) System.out.println(intLists.get(i));
		
		IntList mergedList0 = Util.mergeSortedIntLists(intLists);
		IntList mergedList1 = new IntArrayList();
		for ( int i=0; i<m; ++i ) {
			for ( int j=0; j<intLists.get(i).size(); ++j ) mergedList1.add(intLists.get(i).get(j));
		}
		mergedList1.sort(Integer::compare);
		assertTrue(mergedList0.equals(mergedList1));
		System.out.println(mergedList0);
	}

	@Test
	public void testPrefixWithLengthRatio() {
		String str = "aaa bbbb cc ddd eeeee f g hh i jj kkk";
		int nTokens = (int) str.chars().filter(ch -> ch == ' ').count() + 1;
		for ( double lenRatio : new double[] {0.2, 0.4, 0.6, 0.8, 1.0} ) {
			int eidx=0;
			int len0 = (int)(nTokens*lenRatio);
			int len = 0;
			for ( ; eidx<str.length(); ++eidx ) {
				if ( str.charAt(eidx) == ' ' ) {
					len += 1;
					if ( len == len0 ) break;
				}
			}
			System.out.println(nTokens+"\t"+len0+"\t"+len+"\t"+eidx+"\t"+str.substring(0, eidx));
		}
		
		for ( double lenRatio : new double[] {0.2, 0.4, 0.6, 0.8, 1.0} ) {
			System.out.println(getPrefixWithLengthRatio(str, lenRatio));
		}
	}
	
	public static String getPrefixWithLengthRatio( String str, double lenRatio ) {
		int nTokens = (int) str.chars().filter(ch -> ch == ' ').count() + 1;
		int eidx=0;
		int len0 = (int)(nTokens*lenRatio);
		int len = 0;
		for ( ; eidx<str.length(); ++eidx ) {
			if ( str.charAt(eidx) == ' ' ) {
				len += 1;
				if ( len == len0 ) break;
			}
		}
		return str.substring(0, eidx);
	}
	
	@Test
	public void testLongToByteArray() {
		int n = 100;
		long v = 1;
		ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
		for ( int i=0; i<n; ++i ) {
			buf.putLong(0, v);
			System.out.println(v+"\t"+Arrays.toString(buf.array())+buf.getLong(0));
			v *= 2.1;
		}
	}
	
	@Test
	public void testFileBasedLongListCorrectness() {
		FileBasedLongList list = new FileBasedLongList();
		long n = 100;
		System.out.println(list.size()+"\t"+list.diskSpaceUsage());
		for ( long i=0; i<n; ++i ) {
			list.add(i);
			System.out.println(list.size()+"\t"+list.diskSpaceUsage());
		}
		list.finalize();
		
		for ( int i=0; i<n; ++i ) {
			System.out.println(i+"\t"+list.get(i));
		}
	}

	@Test
	public void testFileBasedLongListEfficiency() {
		/*
		add: 44.003
		get (sequential): 13.8895
		get (random): 6455.7184
		 */
		FileBasedLongList list = new FileBasedLongList();
		Random rn = new Random();
		int n = 1000000;
		long[] val = rn.longs().limit(n).toArray();
		int[] pos = rn.ints(0, n).limit(n).toArray();
		long ts = System.nanoTime();
//		System.out.println(list.size()+"\t"+list.diskSpaceUsage());
		for ( int i=0; i<n; ++i ) {
			list.add(val[i]);
//			System.out.println(list.size()+"\t"+list.diskSpaceUsage());
		}
		list.finalize();
		System.out.println("add: "+(System.nanoTime()-ts)*1.0/n);

		ts = System.nanoTime();
		for ( int i=0; i<n; ++i ) {
			list.get(i);
//			System.out.println(i+"\t"+list.get(i));
		}
		System.out.println("get (sequential): "+(System.nanoTime()-ts)*1.0/n);
		
		ts = System.nanoTime();
		for ( int i=0; i<n; ++i ) {
			list.get(pos[i]);
//			System.out.println(i+"\t"+list.get(i));
		}
		System.out.println("get (random): "+(System.nanoTime()-ts)*1.0/n);
	}
	
//	@Test
//	public void testBinarySearch() {
//		Random rn = new Random(0);
//		int nMax = 1000;
//		int triesMax = 1000000;
//		
//		for ( int tries=0; tries<triesMax; ++tries ) {
//			IntArrayList intList = new IntArrayList();
//			int n = rn.nextInt(nMax)+1;
//			intList.add(0);
//			for ( int i=1; i<n; ++i ) intList.add(intList.getInt(i-1)+rn.nextInt(4));
//			NaiveInvList list = new NaiveInvList(intList.toIntArray(), n);
//			assertEquals(-1, Util.binarySearch(list, -1));
//			for ( int j=list.size()-1; j>=0; --j ) {
//				while ( j > 0 && list.getIdx(j-1) == list.getIdx(j) ) j -= 1;
//				try {
//					assertEquals(j, Util.binarySearch(list, list.getIdx(j)));
//				}
//				catch ( AssertionError e ) {
//					System.err.println(list);
//					System.err.println(list.size());
//					System.err.println(j);
//					System.err.println(Util.binarySearch(list, list.getIdx(j)));
//					throw e;
//				}
//			}
//			
//			for ( int i=0, v=0; i<list.size(); ) {
//				if ( list.getIdx(i) == v ) i += 1;
//				else if ( list.getIdx(i) > v ) {
//					v += 1;
//					if ( list.getIdx(i) > v ) assertEquals(-1, Util.binarySearch(list, v));
//				}
//			}
////			System.out.println((tries+1)+"/"+triesMax);
//		}
//	}
	
	@Test
	public void testGetSubstrings() throws IOException {
		Dataset dataset = DatasetFactory.createInstanceByName(new DatasetParam("WIKI", "1000", "1000", "5", "1.0"));
		for ( TransformableRecordInterface rec : dataset.getIndexedList() ) {
			System.out.println(rec.size());
			for ( Subrecord window : Records.getSubrecords(rec) ) {
				System.out.println(window.sidx+", "+window.eidx);
			}
			System.in.read();
		}
	}
	
	@Test
	public void testSubstring() {
		char[] chseq = "abcde".toCharArray();
		String s = "bcd";
		Substring w = new Substring(chseq, 1, 4);
		Substring v = new Substring(chseq, 1, 4);
		Substring x = new Substring(chseq, 2, 4);
		System.out.println(w);
		System.out.println(w.equals(w));
		System.out.println(w.equals(v));
		System.out.println(w.equals(s));
		System.out.println(w.equals(x));
	}

	@Test
	public void testCompatibilityBetweenSubstringAndString() {
		char[] chseq = "abcde".toCharArray();
		String t = "bcd";
		Substring w = new Substring(chseq, 1, 4);
		System.out.println(t.hashCode());
		System.out.println(w.hashCode());
		System.out.println(t.equals(w));
		System.out.println(w.equals(t));
		Object2IntOpenHashMap<String> map = new Object2IntOpenHashMap<>();
		map.put(t, 111);
		System.out.println(map.containsKey(w));
		System.out.println(map.getInt(t));
		System.out.println(map.getInt(w));
	}
	
	@Test
	public void testIntArrayList() {
		IntArrayList list = new IntArrayList();
		list.add(1);
		list.add(2);
		list.add(3);
		int[] arr = list.elements();
		System.out.println(list);
		System.out.println(Arrays.toString(arr));
		list.add(4);
		System.out.println("intArrayList.elements() does not copy the array.");
		System.out.println(list);
		System.out.println(Arrays.toString(arr));
		int[] arr1 = list.toIntArray();
		list.add(5);
		System.out.println("intArrayList.toIntArray() copies the array.");
		System.out.println(list);
		System.out.println(Arrays.toString(arr1));
	}
	
	@Test
	public void testCharBuffering() throws IOException {
		String indexedPath = DatasetInfo.getIndexedPath("WIKI");
		char[] cbuf = new char[8];
		BufferedReader br = new BufferedReader(new FileReader(indexedPath));
		int n = 10;
		for ( int i=0; i<n; ++i ) {
			br.read(cbuf);
			System.out.println(Arrays.toString(cbuf));
		}
		br.close();
	}
	
	@Test
	public void testStringConstructors() {
		char[] cbuf = "abcde".toCharArray();
		String s = new String(cbuf);
		s = String.valueOf(cbuf, 0, 4);
		System.out.println(Arrays.toString(cbuf));
		System.out.println(s);
		cbuf[0] = 'A';
		System.out.println(Arrays.toString(cbuf));
		System.out.println(s);
	}
	
	@Test
	public void testPriorityQueue() {
		/*
		 * when n = 100, 1000, q1 shows the best for poll and insert
		 */
		Random rn = new Random(0);
		int n = 1000;
		Int2IntBinaryHeap q0 = new Int2IntBinaryHeap();
		PriorityQueue<IntPair> q1 = new PriorityQueue<>(IntPair.keyComparator());
		ObjectHeapPriorityQueue<IntPair> q2 = new ObjectHeapPriorityQueue<>(IntPair.keyComparator());
		ObjectArrayPriorityQueue<IntPair> q3 = new ObjectArrayPriorityQueue<>(IntPair.keyComparator());
		long ts, t0, t1, t2, t3;

		System.out.println("insert");
		t0 = t1 = t2 = t3 = 0;
		for ( int i=0; i<n; ++i ) {
			IntPair e= new IntPair(rn.nextInt(), rn.nextInt());
			ts = System.nanoTime();
			q0.insert(e.i1, e.i2);
			t0 += System.nanoTime() - ts;
			ts = System.nanoTime();
			q1.add(e);
			t1 += System.nanoTime() - ts;
			ts = System.nanoTime();
			q2.enqueue(e);
			t2 += System.nanoTime() - ts;
			ts = System.nanoTime();
			q3.enqueue(e);
			t3 += System.nanoTime() - ts;
		}
		System.out.printf("%12d\n", t0);
		System.out.printf("%12d\n", t1);
		System.out.printf("%12d\n", t2);
		System.out.printf("%12d\n", t3);
		
		System.out.println("Poll and insert");
		t0 = t1 = 0;
		for ( int i=0; i<10000; ++i ) {
			IntPair e= new IntPair(rn.nextInt(), rn.nextInt());
			ts = System.nanoTime();
			IntPair h0 = q0.poll();
			q0.insert(e.i1, e.i2);
			t0 += System.nanoTime() - ts;
			ts = System.nanoTime();
			IntPair h1 = q1.poll();
			q1.add(e);
			t1 += System.nanoTime() - ts;
			ts = System.nanoTime();
			IntPair h2 = q2.dequeue();
			q2.enqueue(e);
			t2 += System.nanoTime() - ts;
			ts = System.nanoTime();
			IntPair h3 = q3.dequeue();
			q3.enqueue(e);
			t3 += System.nanoTime() - ts;
			
			assertEquals(h0, h1);
			assertEquals(h0, h2);
			assertEquals(h0, h3);
		}
		System.out.printf("%12d\n", t0);
		System.out.printf("%12d\n", t1);
		System.out.printf("%12d\n", t2);
		System.out.printf("%12d\n", t3);
	}
}
