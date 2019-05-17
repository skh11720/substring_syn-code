package snu.kdd.etc;

import java.util.LinkedList;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntCollections;

public class ContainerEfficiencyTest {

	Random rn = new Random(0);

	@Test
	public void testIntArrayList() {
		System.out.println("testIntArrayList");
		appendIntArrayList();
		insertIntArrayList();
		removeIntArrayList();
		binarySearchIntArrayList();
		testBS();
	}
	
	public void appendIntArrayList() {
		IntArrayList list = new IntArrayList();
		int n = 1000000;
		int m = 100000;
		long ts = System.nanoTime();
		for ( int i=0; i<n; ++i ) {
			list.add( rn.nextInt() );
			if ( (i+1)%m == 0 ) {
				System.out.println("Append: "+(System.nanoTime() - ts)/1e6);
				ts = System.nanoTime();
			}
		}
	}
	
	public void insertIntArrayList() {
		IntArrayList list = new IntArrayList();
		int n = 100000;
		int m = 10000;
		long ts = System.nanoTime();
		for ( int i=0; i<n; ++i ) {
			int k = rn.nextInt();
			int idx = rn.nextInt(list.size()+1);
			list.add( idx, k );
			if ( (i+1)%m == 0 ) {
				System.out.println("Insert: "+(System.nanoTime() - ts)/1e6);
				ts = System.nanoTime();
			}
		}
	}

	public void removeIntArrayList() {
		IntArrayList list = new IntArrayList();
		int n = 100000;
		int m = 10000;
		for ( int i=0; i<n; ++i ) list.add( rn.nextInt() );

		long ts = System.nanoTime();
		for ( int i=0; i<n; ++i ) {
			int idx = rn.nextInt(list.size());
			list.removeInt(idx);
			if ( (i+1)%m == 0 ) {
				System.out.println("Remove: "+(System.nanoTime() - ts)/1e6);
				ts = System.nanoTime();
			}
		}
	}
	
	public void binarySearchIntArrayList() {
		int[] arr = { 100, 200, 300, 400, 500, 600 };
		IntArrayList list = new IntArrayList(arr);
		
		for ( int key : new int[] {99, 100, 101, 102, 350, 599, 600, 601, 602 }) {
			int idx = IntArrays.binarySearch(list.toIntArray(), key);
			System.out.println( key+": "+idx );
			if ( idx < 0 ) list.add(-idx-1, key);
			else list.add(idx, key);
			System.out.println(list);
		}
	}
	
	@Ignore
	public void testLinkedList() {
		System.out.println("testLinkedList");
		appendLinkedList();
		insertLinkedList();
		removeLinkedList();
	}

	public void appendLinkedList() {
		LinkedList<Integer> list = new LinkedList<>();
		int n = 1000000;
		int m = 100000;
		long ts = System.nanoTime();
		for ( int i=0; i<n; ++i ) {
			list.add( rn.nextInt() );
			if ( (i+1)%m == 0 ) {
				System.out.println("Append: "+(System.nanoTime() - ts)/1e6);
				ts = System.nanoTime();
			}
		}
	}

	public void insertLinkedList() {
		LinkedList<Integer> list = new LinkedList<>();
		int n = 100000;
		int m = 10000;
		long ts = System.nanoTime();
		for ( int i=0; i<n; ++i ) {
			int k = rn.nextInt();
			int idx = rn.nextInt(list.size()+1);
			list.add( idx, k );
			if ( (i+1)%m == 0 ) {
				System.out.println("Insert: "+(System.nanoTime() - ts)/1e6);
				ts = System.nanoTime();
			}
		}
	}

	public void removeLinkedList() {
		LinkedList<Integer> list = new LinkedList<>();
		int n = 100000;
		int m = 10000;
		for ( int i=0; i<n; ++i ) list.add( rn.nextInt() );

		long ts = System.nanoTime();
		for ( int i=0; i<n; ++i ) {
			int idx = rn.nextInt(list.size());
			list.remove(idx);
			if ( (i+1)%m == 0 ) {
				System.out.println("Remove: "+(System.nanoTime() - ts)/1e6);
				ts = System.nanoTime();
			}
		}
	}
	
	@Test
	public void testBS() {
		int[] arr = { 100, 200, 300, 400, 500, 600 };
		IntArrayList list = new IntArrayList(arr);
		
		for ( int key : new int[] {99, 100, 101, 102, 350, 599, 600, 601, 602 }) {
			int idx = search(list.toIntArray(), key);
			System.out.println( key+": "+idx );
			if ( idx < 0 ) list.add(-idx-1, key);
			else list.add(idx, key);
			System.out.println(list);
		}
	}
	
	public int search( int[] arr, int key ) {
		int l = 0; 
		int r = arr.length;
		while ( l < r ) {
			int m = (l+r)/2;
			if ( key < arr[m] ) r = m;
			else if ( key > arr[m] ) l = m+1;
			else return m;
		}
		return -l-1;
	}
}
