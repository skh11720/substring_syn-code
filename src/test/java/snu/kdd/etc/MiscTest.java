package snu.kdd.etc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntDouble;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Records;
import snu.kdd.substring_syn.data.record.Subrecord;
import snu.kdd.substring_syn.utils.Util;
import snu.kdd.substring_syn.utils.window.iterator.SortedRecordSlidingWindowIterator;
import vldb18.PkduckDP;

public class MiscTest {
	
	@Test
	public void testExactVerification() throws IOException {
		int qidx = 23;
		int sidx = 287;
		Dataset dataset = Dataset.createInstanceByName("WIKI_3", "10000");
		Record query = null;
		Record text = null;
		for ( Record rec : dataset.getSearchedList() ) {
			if ( rec.getID() == qidx ) {
				query = rec;
				break;
			}
		}
		
		for ( Record rec : dataset.getIndexedList() ) {
			if ( rec.getID() == sidx ) {
				text = rec;
				break;
			}
		}
		
		System.out.println(query.toOriginalString());
		System.out.println(text.toOriginalString());
	}
	
	@Test
	public void testRecord() throws IOException {
		Dataset dataset = Dataset.createInstanceByName("WIKI_3", "10000");
//		Record rec = dataset.searchedList.get(1);
		for ( Record rec : dataset.getIndexedList() ) {
			int n1 = 0;
			for ( int k=0; k<rec.size(); ++k ) {
				for ( Rule rule : rec.getApplicableRules()[k] ) ++n1;
			}
			
			int n2 = 0;
			for ( Rule rule : rec.getApplicableRuleIterable() ) ++n2;
			
			assertEquals(n1, n2);
		}
	}
	
	@Test
	public void testTransformLength() throws IOException {
		Dataset dataset = Dataset.createInstanceByName("WIKI_3", "10000");
		for ( Record rec : dataset.getSearchedList() ) {
			System.out.println(rec.getID()+"\t"+rec.getMinTransLength()+"\t"+rec.getMaxTransLength());
		}
	}

	@Test
	public void testQueryCandTokenSet() throws IOException {
		Dataset dataset = Dataset.createInstanceByName("WIKI_3", "10000");
		for ( Record rec : dataset.getSearchedList() ) {
			System.out.println(rec.getID()+"\t"+(new IntArrayList(rec.getCandTokenSet().stream().sorted().iterator())));
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
		Dataset dataset = Dataset.createInstanceByName("SPROT_long", "1000");
		double theta = 0.6;
		for ( Record rec : dataset.getIndexedList() ) {
			int nw0 = sumWindowSize(rec);
			int nw1 = 0;
			for ( int w=1; w<=rec.size(); ++w ) {
				SortedRecordSlidingWindowIterator witer = new SortedRecordSlidingWindowIterator(rec, w, theta);
				for ( int widx=0; witer.hasNext(); ++widx ) {
					Subrecord window = witer.next();
					nw1 += window.size();
				}
			}
//			System.out.println(rec.getID()+"\t"+rec.size()+"\t"+nw0+"\t"+nw1);
			assertEquals(nw0, nw1);
		}
	}
	
	private static int sumWindowSize( Record rec ) {
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
	
	@Test
	public void testIntDoubleList() {
		Int2DoubleMap counter = new Int2DoubleOpenHashMap();
		int n = 10;
		Random rn = new Random(0);
		for ( int i=0; i<n; ++i ) counter.put(i, rn.nextDouble());
		IntDoubleList list = new IntDoubleList(counter);
		System.out.println(list);
		
		for ( int i=0; i<n; ++i ) {
			int k = rn.nextInt(n+n/2);
			double dv = rn.nextDouble();
			System.out.println(k+", "+dv);
			list.update(k, dv);
			System.out.println(list);
		}
	}

	class IntDoubleList {
		Int2IntMap key2posMap = null;
		List<IntDouble> list = null;
		
		public IntDoubleList() {
			list = new ObjectArrayList<>();
			key2posMap = new Int2IntOpenHashMap();
		}
		
		public IntDoubleList( Int2DoubleMap counter ) {
			this();
			for ( Int2DoubleMap.Entry entry : counter.int2DoubleEntrySet() ) {
				list.add( new IntDouble(entry.getIntKey(), entry.getDoubleValue()) );
			}
			list.sort(IntDouble.comp);
			for ( int i=0; i<list.size(); ++i ) key2posMap.put(list.get(i).k, i);
		}
		
		public void update( int k, double dv ) {
			if ( key2posMap.containsKey(k) ) updateExistingKey(k, dv);
			else updateNewKey(k, dv);
		}
		
		private void updateExistingKey( int k, double dv ) {
			int i = key2posMap.get(k);
			IntDouble entry = list.get(i);
			entry.v += dv;
			updatePosition(entry, i);
		}
		
		private void updateNewKey( int k, double dv ) {
			IntDouble entry = new IntDouble(k, dv);
			list.add(entry);
			int i = list.size()-1;
			key2posMap.put(k, i);
			updatePosition(entry, i);
		}
		
		private void updatePosition( IntDouble entry, int i ) {
			while ( i > 0 ) {
				if ( list.get(i-1).v < entry.v ) swap(i-1, i);
				else break;
				--i;
			}
		}
		
		private void swap( int i, int j ) {
			key2posMap.put(list.get(i).k, j);
			key2posMap.put(list.get(j).k, i);
			IntDouble tmp = list.get(i);
			list.set(i, list.get(j));
			list.set(j, tmp);
		}
		
		public String toString() {
			StringBuilder strbld = new StringBuilder();
			for ( IntDouble entry : list ) strbld.append(key2posMap.get(entry.k)+":"+entry+", ");
			return strbld.toString();
		}
	}
}
