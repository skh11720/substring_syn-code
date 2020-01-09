package snu.kdd.substring_syn.utils;

import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.io.FileUtils;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntIterators;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.Records;

public class Util {
	public static final int bigprime = 1645333507;

	public static void printLog( String message ) {
		System.out.println( toLogString( message ) );
	}

	public static void printErr( String message ) {
		System.err.println( toLogString( message ) );
	}

	public static String toLogString( String message ) {
		// print log messages
		StackTraceElement[] tr = new Throwable().getStackTrace();
		String className = tr[ 2 ].getClassName();

		final StringBuilder bld = new StringBuilder();
		bld.append( new java.text.SimpleDateFormat( "yyyy/MM/dd HH:mm:ss " ).format( new java.util.Date() ) );
		bld.append( '[' );
		bld.append( className.substring( className.lastIndexOf( '.' ) + 1, className.length() ) );
		bld.append( '.' );
		bld.append( tr[ 2 ].getMethodName() );
		bld.append( ']' );
		bld.append( ' ' );
		bld.append( message );
		return bld.toString();
	}

	public static void printArgsError( CommandLine cmd ) {
		Iterator<Option> itr = cmd.iterator();

		int maxRowSize = 0;
		int maxSize = 0;
		while( itr.hasNext() ) {
			Option opt = itr.next();
			int size = opt.getOpt().length();
			if( maxSize < size ) {
				maxSize = size;
			}

			if( opt.getValue() != null ) {
				int valueSize = opt.getValue().length() + 3;

				if( maxRowSize < valueSize ) {
					maxRowSize = valueSize;
				}
			}
		}
		maxRowSize += maxSize + 3;

		StringBuilder bld = new StringBuilder();
		int halfRowSize = ( maxRowSize - 16 ) / 2;
		for( int i = 0; i < halfRowSize; i++ ) {
			bld.append( "=" );
		}
		bld.append( "[printArgsError]" );
		for( int i = 0; i < halfRowSize; i++ ) {
			bld.append( "=" );
		}
		String index = bld.toString();
		System.err.println( index );

		itr = cmd.iterator();
		while( itr.hasNext() ) {
			Option opt = itr.next();
			int size = opt.getOpt().length();
			System.err.print( opt.getOpt() );
			for( int i = size; i < maxSize; i++ ) {
				System.err.print( " " );
			}
			System.err.println( " : " + opt.getValue() );
		}

		System.err.println( new String( new char[ index.length() ] ).replace( "\0", "=" ) );
	}

	public static void printGCStats() {
		long totalGarbageCollections = 0;
		long garbageCollectionTime = 0;

		for( GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans() ) {

			long count = gc.getCollectionCount();

			if( count >= 0 ) {
				totalGarbageCollections += count;
			}

			long time = gc.getCollectionTime();

			if( time >= 0 ) {
				garbageCollectionTime += time;
			}
		}
		printLog( "Total Garbage Collections: " + totalGarbageCollections );
		printLog( "Total Garbage Collection Time (ms): " + garbageCollectionTime );
	}

	public static void print2dArray( int[][] arr ) {
		for ( int i=0; i<arr.length; ++i ) {
			for ( int j=0; j<arr[0].length; ++j ) {
				System.out.print( String.format("%3d", arr[i][j]) );
			}
			System.out.println();
		}
	}
	
	public static void print3dArray( boolean[][][] arr ) {
		for ( int j=0; j<arr[0].length; ++j ) {
			for ( int i=0; i<arr.length; ++i ) {
				for ( int k=0; k<arr[0][0].length; ++k ) {
					System.out.print( String.format("%3d", arr[i][j][k]? 1: 0 ) );
				}
				System.out.print("\t");
			}
			System.out.println();
		}
	}

	public static void print3dArray( int[][][] arr ) {
		for ( int j=0; j<arr[0].length; ++j ) {
			for ( int i=0; i<arr.length; ++i ) {
				for ( int k=0; k<arr[0][0].length; ++k ) {
					System.out.print( String.format("%3d", arr[i][j][k] ) );
				}
				System.out.print("\t");
			}
			System.out.println();
		}
	}

	public static boolean equalsToSubArray( int[] a, int start, int end, int[] b ) {
		// return true if a[start:end] is equal to b; otherwise false.
		if ( b.length != end - start ) return false;
		for ( int i=0; i<b.length; i++ ) {
			if ( a[start+i] != b[i] ) return false;
		}
		return true;
	}
	
	public static int[] pad( int[] a, int len, int padding ) {
		/*
		 * Return the padded array of length len.
		 * padding: the token used to pad.
		 * 	- Integer.MAX_VALUE is the dummy token.
		 * 	- -1: the wildcard.
		 */
		if ( a.length > len ) throw new RuntimeException( "the resulting length must be larger than the length of input array." );
		int[] a_padded = new int[len];
		int i;
		for ( i=0; i<a.length; ++i ) a_padded[i] = a[i];
		for ( ; i<len; ++i ) a_padded[i] = padding;
		return a_padded;
	}

	public static List<IntArrayList> getCombinations( int n, int k ) {
		/*
		 * Return all combinations of n choose k.
		 * TODO slow when k is large...!
		 */
		List<IntArrayList> combList = new ObjectArrayList<IntArrayList>();

		ObjectArrayFIFOQueue<IntArrayList> stack_x_errors = new ObjectArrayFIFOQueue<IntArrayList>();
		stack_x_errors.enqueue( new IntArrayList() );
		
		while ( !stack_x_errors.isEmpty() ) {
			IntArrayList comb = stack_x_errors.dequeue();
			if ( comb.size() == k ) combList.add( comb );
			else {
				int max = comb.size() > 0 ? Collections.max( comb ) : -1;
				for ( int i=max+1; i<n; ++i ) {
					if ( !comb.contains( i )) {
						IntArrayList comb2 = new IntArrayList( comb );
						comb2.add( i );
						stack_x_errors.enqueue( comb2 );
					}
				}
			}
		}
		return combList;
	}

	public static List<IntArrayList> getCombinationsAll( int n, int k ) {
		/*
		 * Return all combinations of n choose k' for all k'<=k.
		 */
		List<IntArrayList> combList = new ObjectArrayList<IntArrayList>();

		ObjectArrayFIFOQueue<IntArrayList> stack_x_errors = new ObjectArrayFIFOQueue<IntArrayList>();
		stack_x_errors.enqueue( new IntArrayList() );
		
		while ( !stack_x_errors.isEmpty() ) {
			IntArrayList comb = stack_x_errors.dequeue();
			combList.add( comb );
			if ( comb.size() < k ) {
				int max = comb.size() > 0 ? Collections.max( comb ) : -1;
				for ( int i=max+1; i<n; ++i ) {
					if ( !comb.contains( i )) {
						IntArrayList comb2 = new IntArrayList( comb );
						comb2.add( i );
						stack_x_errors.enqueue( comb2 );
					}
				}
			}
		}
		return combList;
	}
	
	public static List<List<IntArrayList>> getCombinationsAllByDelta( int n, int k) {
		/*
		 * Return a list of length (k+1) whose elements are
		 * 		lists of n choose 0
		 * 		lists of n choose 1
		 *      ...
		 * 		lists of n choose k
		 */
		List<List<IntArrayList>> combDeltaList = new ObjectArrayList<>();
		for ( int d=0; d<=k; ++d ) combDeltaList.add(new ObjectArrayList<>());

		ObjectArrayFIFOQueue<IntArrayList> stack_x_errors = new ObjectArrayFIFOQueue<IntArrayList>();
		stack_x_errors.enqueue( new IntArrayList() );
		
		while ( !stack_x_errors.isEmpty() ) {
			IntArrayList comb = stack_x_errors.dequeue();
			combDeltaList.get(comb.size()).add( comb );
			if ( comb.size() < k ) {
				int max = comb.size() > 0 ? Collections.max( comb ) : -1;
				for ( int i=max+1; i<n; ++i ) {
					if ( !comb.contains( i )) {
						IntArrayList comb2 = new IntArrayList( comb );
						comb2.add( i );
						stack_x_errors.enqueue( comb2 );
					}
				}
			}
		}
		return combDeltaList;
	}

	public static int[] getSubsequence( int[] arr, IntArrayList idxList ) {
		/*
		 * Return the subsequence of arr with indexes in idxList.
		 */
		if ( idxList.size() == 0 ) return null;
		else {
			int[] out = new int[idxList.size()];
			int i = 0;
			for ( int idx : idxList ) out[i++] = arr[idx];
			return out;
		}
	}
	
	public static int[] getSubsequenceNotIn( int[] arr, IntArrayList notInIdxList ) {
		/*
		 * Return the subsequence of arr with indexes in idxList.
		 */
		if ( notInIdxList.size() == arr.length ) return null;
		else {
			int[] out = new int[arr.length - notInIdxList.size()];
			for ( int i=0, j=0; i<arr.length; ++i ) {
				if ( j < notInIdxList.size() && i == notInIdxList.getInt(j) ) ++j;
				else out[i-j] = arr[i];
			}
			return out;
		}
	}
	
	public static int lcs( int[] x, int[] y ) {
		/*
		 * Compute the length of the LCS.
		 * Note that the return value is NOT the LCS distance value.
		 */
		int[] L = new int[x.length+1];
		int[] L_prev = new int[x.length+1];
		Arrays.fill( L, 0 );
		for ( int j=0; j<y.length; ++j ) {
			// swap tables
			int[] tmp = L_prev;
			L_prev = L;
			L = tmp;

			for ( int i=1; i<=x.length; ++i ) {
				L[i] = Math.max( L[i-1], L_prev[i] );
				if ( x[i-1] == y[j] ) L[i] = Math.max( L[i], L_prev[i-1]+1 );
				else L[i] = Math.max( L[i], L_prev[i-1] );
			}
		}
		return L[x.length];
	}

	@Deprecated // since TOOOOO SLOW compared to edit(int[], int[], int, int, int, int, int)
	public static int edit( int[] x, int[] y ) {
		/*
		 * Compute and return the edit distance between x and y.
		 */
		return edit_all( x, y )[y.length];
	}
	
	public static int lcs( int[] x, int[] y, int threshold ) {
		/*
		 * Compute and return the exact lcs distance between x and y if the value is at most threshold.
		 * Otherwise, it returns any value larger then the threshold.
		 */
		return lcs(x, y, threshold, 0, 0, x.length, y.length );
	}

	public static int edit( int[] x, int[] y, int threshold ) {
		/*
		 * Compute and return the exact edit distance between x and y if the value is at most threshold.
		 * Otherwise, it returns any value larger then the threshold.
		 */
		return edit(x, y, threshold, 0, 0, x.length, y.length );
	}
	
	public static int[] edit_all( int[] x, int[] y ) {
		/*
		 * Compute and return the edit distances between x and y[:1], y[:2], ... y[:y.length].
		 */
		int[] D = new int[y.length+1];
		int[] D_prev = new int[y.length+1];
		for ( int j=0; j<=y.length; ++j ) D[j] = j;
		for ( int i=0; i<x.length; ++i ) {
			// swap tables
			int[] tmp = D_prev;
			D_prev = D;
			D = tmp;

			D[0] = i+1;
			for ( int j=0; j<y.length; ++j ) {
				D[j+1] = Math.min( D[j], D_prev[j+1] )+1;
				if ( y[j] == x[i] ) D[j+1] = Math.min( D[j+1], D_prev[j] );
				else D[j+1] = Math.min( D[j+1], D_prev[j]+1 );
			}
		}
		return D;
	}

	public static int[] lcs_all( int[] x, int[] y, int j0 ) {
		/*
		 * Compute and return the lcs distances between x and y[j0:j0], y[j0:j0+1], y[j0:j0+2], ..., y[j0:y.length].
		 * The range of j0 is 0 <= j0 <= y.length.
		 */
		int[] D = new int[y.length+1];
		int[] D_prev = new int[y.length+1];
		for ( int j=j0; j<=y.length; ++j ) D[j] = j-j0;
		for ( int i=0; i<x.length; ++i ) {
			// swap tables
			int[] tmp = D_prev;
			D_prev = D;
			D = tmp;

			D[j0] = i+1;
			for ( int j=j0; j<y.length; ++j ) {
				D[j+1] = Math.min( D[j], D_prev[j+1] )+1;
				if ( y[j] == x[i] ) D[j+1] = Math.min( D[j+1], D_prev[j] );
			}
		}
		return D;
	}

	public static int[] edit_all( int[] x, int[] y, int j0 ) {
		/*
		 * Compute and return the edit distances between x and y[j0:j0], y[j0:j0+1], y[j0:j0+2], ..., y[j0:y.length].
		 * The range of j0 is 0 <= j0 <= y.length.
		 */
		int[] D = new int[y.length+1];
		int[] D_prev = new int[y.length+1];
		for ( int j=j0; j<=y.length; ++j ) D[j] = j-j0;
		for ( int i=0; i<x.length; ++i ) {
			// swap tables
			int[] tmp = D_prev;
			D_prev = D;
			D = tmp;

			D[j0] = i+1;
			for ( int j=j0; j<y.length; ++j ) {
//				 compute the edit distances between x and y[jj0:j0+1], ..., y[j0:y0.length();
				D[j+1] = Math.min( D[j], D_prev[j+1] )+1;
				if ( y[j] == x[i] ) D[j+1] = Math.min( D[j+1], D_prev[j] );
				else D[j+1] = Math.min( D[j+1], D_prev[j]+1 );
			}
		}
		return D;
	}

	public static int lcs( int[] x, int[] y, int threshold, int xpos, int ypos, int xlen, int ylen ) {
		if (xlen == -1) xlen = x.length - xpos;
		if (ylen == -1) ylen = y.length - ypos;
		if ( xlen > ylen + threshold || ylen > xlen + threshold ) return threshold+1;
		if ( xlen == 0 ) return ylen;

		int[][] matrix = new int[xlen + 1][2 * threshold + 1];
		for (int k = 0; k <= threshold; k++) matrix[0][threshold + k] = k;

		int right = (threshold + (ylen - xlen)) / 2;
		int left = (threshold - (ylen - xlen)) / 2;
		for (int i = 1; i <= xlen; i++)
		{
			boolean valid = false;
			if (i <= left)
			{
				matrix[i][threshold - i] = i;
				valid = true;
			}
			for (int j = (i - left >= 1 ? i - left : 1); j <= (i + right <= ylen ? i + right : ylen); j++)
			{
				if (x[xpos + i - 1] == y[ypos + j - 1]) matrix[i][j - i + threshold] = matrix[i - 1][j - i + threshold];
				else
					matrix[i][j - i + threshold] = Math.min(
//							matrix[i - 1][j - i + threshold], // for edit operation
							j - 1 >= i - left ? matrix[i][j - i + threshold - 1] : threshold,
							j + 1 <= i + right ? matrix[i - 1][j - i + threshold + 1] : threshold)
					+ 1;
				if (Math.abs(xlen - ylen - i + j) + matrix[i][j - i + threshold] <= threshold) valid = true;
			}
			if (!valid) return threshold + 1;
		}
		return matrix[xlen][ylen - xlen + threshold];
	}
	
	private static int[][] matrix = new int[1][1];
	
	public static int edit( int[] x, int[] y, int threshold, int xpos, int ypos, int xlen, int ylen ) {
		/*
		 *  G. Li, D. Deng, J. Wang, and J. Feng: PVLDB 2011
		 */
		if (xlen == -1) xlen = x.length - xpos;
		if (ylen == -1) ylen = y.length - ypos;
		if ( xlen > ylen + threshold || ylen > xlen + threshold ) return threshold+1;
		if ( xlen <= threshold && ylen <= threshold ) return threshold;
		if ( xlen == 0 ) return ylen;

		if ( matrix.length < xlen+1 ) matrix = new int[xlen + 1][2 * threshold + 1];
		else if ( matrix[0].length < 2*threshold+1 ) {
			for ( int i=0; i<matrix.length; ++i ) matrix[i] = new int[2*threshold+1];
		}
		for (int k = 0; k <= threshold; k++) matrix[0][threshold + k] = k;

		int right = (threshold + (ylen - xlen)) / 2;
		int left = (threshold - (ylen - xlen)) / 2;
		for (int i = 1; i <= xlen; i++)
		{
			boolean valid = false;
			if (i <= left)
			{
				matrix[i][threshold - i] = i;
				valid = true;
			}
			for (int j = (i - left >= 1 ? i - left : 1); j <= (i + right <= ylen ? i + right : ylen); j++)
			{
				if (x[xpos + i - 1] == y[ypos + j - 1]) matrix[i][j - i + threshold] = matrix[i - 1][j - i + threshold];
				else
					matrix[i][j - i + threshold] = Math.min(
							matrix[i - 1][j - i + threshold], Math.min( // for edit operation
							j - 1 >= i - left ? matrix[i][j - i + threshold - 1] : threshold,
							j + 1 <= i + right ? matrix[i - 1][j - i + threshold + 1] : threshold))
					+ 1;
				if (Math.abs(xlen - ylen - i + j) + matrix[i][j - i + threshold] <= threshold) valid = true;
			}
			if (!valid) return threshold + 1;
		}
		return matrix[xlen][ylen - xlen + threshold];
	}
	
	public static double jaccard( int[] x, int[] y ) {
		// consider x and y as sets, not multisets
		int[] shorter = x.length <= y.length? x: y;
		int[] longer = x.length <= y.length? y: x;
		IntOpenHashSet setLonger = new IntOpenHashSet(longer);
		IntOpenHashSet setShorter = new IntOpenHashSet(shorter);
		int common = 0;
		for ( int token : setShorter ) if (setLonger.contains(token)) ++common;
		double sim = 1.0*common/(setLonger.size() + setShorter.size() - common);
		return sim;
	}

	public static double jaccard( IntList x, IntList y ) {
		// consider x and y as sets, not multisets
		IntSet xSet = new IntOpenHashSet(x);
		IntSet ySet = new IntOpenHashSet(y);
		IntSet smaller = xSet.size() <= ySet.size()? xSet: ySet;
		IntSet larger = xSet.size() <= ySet.size()? ySet: xSet;
		int common = 0;
		for ( int token : smaller ) if (larger.contains(token)) ++common;
		double sim = 1.0*common/(larger.size() + smaller.size() - common);
		return sim;
	}

	public static double jaccardM( int[] x, int[] y ) {
		Int2IntOpenHashMap xCounter = new Int2IntOpenHashMap();
		for ( int token : x ) xCounter.addTo(token, 1);
		Int2IntOpenHashMap yCounter = new Int2IntOpenHashMap();
		for ( int token : y ) yCounter.addTo(token, 1);
		return jaccardM(xCounter, yCounter);
	}

	public static double jaccardM( Int2IntOpenHashMap x, Int2IntOpenHashMap y ) {
		IntSet tokenSet = new IntOpenHashSet(x.keySet());
		tokenSet.addAll(y.keySet());
		int num = 0;
		int denum = 0;
		for ( int token : tokenSet ) {
			num += Math.min(x.get(token), y.get(token));
			denum += Math.max(x.get(token), y.get(token));
		}
		return (double)num/denum;
	}

	public static double jaccardM( IntList xList, IntList yList ) {
		int num = 0;
		int denum = 0;
		IntIterator ix = IntIterators.asIntIterator(xList.stream().sorted().iterator());
		IntIterator iy = IntIterators.asIntIterator(yList.stream().sorted().iterator());
		int x = ix.nextInt();
		int y = iy.nextInt();
		while (true) {
			++denum;
			if ( x == y ) {
				++num;
				if ( !ix.hasNext() || !iy.hasNext() ) break;
				x = ix.nextInt();
				y = iy.nextInt();
			}
			else if ( x < y ) {
				if ( !ix.hasNext() ) {
					++denum;
					break;
				}
				x = ix.nextInt();
			}
			else {
				if ( !iy.hasNext() ) {
					++denum;
					break;
				}
				y = iy.nextInt();
			}
		}
		while ( ix.hasNext() ) {
			ix.nextInt();
			++denum;
		}
		while ( iy.hasNext() ) {
			iy.nextInt();
			++denum;
		}
		return (double)num/denum;
	}

	public static double subJaccard0( int[] q, int[] t ) {
		double simMax = 0;
		for ( int i=0; i<t.length; ++i ) {
			for ( int j=i; j<t.length; ++j ) {
				double sim = Util.jaccard(q, Arrays.copyOfRange(t, i, j+1));
				simMax = Math.max(simMax, sim);
			}
		}
		return simMax;
	}

	public static double subJaccard1( IntList q, IntList t ) {
		double simMax = 0;
		for ( int i=0; i<t.size(); ++i ) {
			for ( int j=i; j<t.size(); ++j ) {
				double sim = Util.jaccard(q, t.subList(i, j+1));
				simMax = Math.max(simMax, sim);
			}
		}
		return simMax;
	}

	public static double subJaccard( IntList q, IntList t ) {
		double simMax = 0;
		IntList idxList = new IntArrayList();
		IntSet queryTokenSet = new IntOpenHashSet(q);
		ObjectList<IntSet> segList = new ObjectArrayList<>();
		IntSet lastSeg = new IntOpenHashSet();

		for ( int i=0; i<t.size(); ++i ) {
			int token = t.get(i);
			if ( queryTokenSet.contains(token) ) {
				lastSeg.add(token);
				idxList.add(i);
				segList.add(lastSeg);
				lastSeg = new IntOpenHashSet();
			}
			else lastSeg.add(token);
		}

		for ( int i=0; i<idxList.size(); ++i ) {
			IntSet cap = new IntOpenHashSet();
			IntSet cup = new IntOpenHashSet(q);
			cap.add(t.get(idxList.get(i)));
			simMax = Math.max(simMax, 1.0*cap.size()/cup.size());
			for ( int j=i+1; j<idxList.size(); ++j ) {
				cap.add(t.get(idxList.get(j)));
				cup.addAll(segList.get(j));
				simMax = Math.max(simMax, 1.0*cap.size()/cup.size());
			}
		}

		return simMax;
	}
	
	public static double subJaccardM( int[] q, int[] t ) {
		return subJaccardM(IntArrayList.wrap(q), IntArrayList.wrap(t));
	}

	public static double subJaccardM( IntList q, IntList t ) {
		double simMax = 0;
		IntList idxList = new IntArrayList();
		Int2IntOpenHashMap qCounter = new Int2IntOpenHashMap();
		for ( int token : q ) qCounter.addTo(token, 1);
		ObjectList<Int2IntOpenHashMap> counterList = new ObjectArrayList<>();
		Int2IntOpenHashMap lastCounter = new Int2IntOpenHashMap();

		for ( int i=0; i<t.size(); ++i ) {
			int token = t.get(i);
			if ( qCounter.keySet().contains(token) ) {
				lastCounter.addTo(token, 1);
				idxList.add(i);
				counterList.add(lastCounter);
				lastCounter = new Int2IntOpenHashMap();
			}
			else lastCounter.addTo(token, 1);
		}
		
		if ( idxList.size() > 0 ) simMax = 1.0/q.size();

		for ( int i=0; i<idxList.size(); ++i ) {
			int sidx = idxList.get(i);
			Int2IntOpenHashMap tCounter = new Int2IntOpenHashMap();
			tCounter.addTo(t.get(sidx), 1);
			for ( int j=i+1; j<idxList.size(); ++j ) {
				sumCounters(tCounter, counterList.get(j));
				simMax = Math.max(simMax, jaccardM(qCounter, tCounter));
			}
		}

		return simMax;
	}

	public static double subJaccardM2( IntList q, IntList t ) {
		double simMax = 0;
		IntList idxList = new IntArrayList();
		Int2IntOpenHashMap qCounter = new Int2IntOpenHashMap();
		for ( int token : q ) qCounter.addTo(token, 1);
		ObjectList<Int2IntOpenHashMap> counterList = new ObjectArrayList<>();
		Int2IntOpenHashMap lastCounter = new Int2IntOpenHashMap();

		for ( int i=0; i<t.size(); ++i ) {
			int token = t.get(i);
			if ( qCounter.keySet().contains(token) ) {
				lastCounter.addTo(token, 1);
				idxList.add(i);
				counterList.add(lastCounter);
				lastCounter = new Int2IntOpenHashMap();
			}
			else lastCounter.addTo(token, 1);
		}
		
		if ( idxList.size() > 0 ) simMax = 1.0/q.size();

		for ( int i=0; i<idxList.size(); ++i ) {
			int sidx = idxList.get(i);
			Int2IntOpenHashMap tCounter = new Int2IntOpenHashMap();
			tCounter.addTo(t.get(sidx), 1);
			for ( int j=i+1; j<idxList.size(); ++j ) {
				sumCounters(tCounter, counterList.get(j));
				simMax = Math.max(simMax, jaccardM(qCounter, tCounter));
			}
		}

		return simMax;
	}
	
	public static void sumCounters( Int2IntOpenHashMap thisCounter, Int2IntOpenHashMap other ) {
		for ( Int2IntMap.Entry entry : other.int2IntEntrySet() ) {
			int key = entry.getIntKey();
			int val = entry.getIntValue();
			thisCounter.addTo(key, val);
		}
	}

//	public static Dataset getDatasetWithPreprocessing( String name, String size ) throws IOException {
//		return Dataset.createInstanceByName(name, size);
//	}

	public static String getGroundTruthPath( String name ) {

		String osName = System.getProperty( "os.name" );
		String prefix = null;
		String sep = null;
		if ( osName.startsWith( "Windows" ) ) {
			prefix = "D:\\ghsong\\data\\synonyms\\";
			sep = "\\\\";
		}
		else if ( osName.startsWith( "Linux" ) ) {
			prefix = "data/";
			sep = "/";
		}
		return prefix + name+sep+name+"_groundtruth.txt";
	}
	
	public static int getPrefixLength( int len, double theta ) {
		return len - (int)(Math.ceil(theta*len)) + 1;
	}

	public static int getPrefixLength( RecordInterface rec, double theta ) {
		return rec.size() - (int)(Math.ceil(theta*rec.size())) + 1;
	}

	public static IntOpenHashSet getPrefix( RecordInterface rec, double theta ) {
		int prefixLen = getPrefixLength(rec, theta);
		return new IntOpenHashSet( rec.getTokenList().stream().sorted().limit(prefixLen).iterator() );
	}

	public static double getModifiedTheta( Record query, RecordInterface rec, double theta ) {
		return theta * query.size() / (query.size() + 2*(rec.getMaxRhsSize()-1));
	}
	
	public static double getModifiedTheta( int qlen, RecordInterface rec, double theta ) {
		return theta * qlen / (qlen + 2*(rec.getMaxRhsSize()-1));
	}
	
	public static IntOpenHashSet getExpandedPrefix( Record rec, double theta ) {
		IntOpenHashSet prefix = new IntOpenHashSet();
		for ( Record exp : Records.expandAll(rec) ) {
			int prefixLen = getPrefixLength(exp, theta);
			exp.getTokens().stream().sorted().limit(prefixLen).forEach(t -> prefix.add(t));
		}
		return prefix;
	}
	
	public static boolean hasIntersection( IntCollection set0, IntCollection set1 ) {
		IntCollection smallSet = set1.size() < set0.size()? set1: set0;
		IntCollection largeSet = set1.size() < set0.size()? set0: set1;
		for ( int token : smallSet ) {
			if ( largeSet.contains(token) ) return true;
		}
		return false;
	}

	public static int sumWindowSize( RecordInterface rec ) {
		int n = rec.size();
		return n*(n+1)*(n+1)/2 - n*(n+1)*(2*n+1)/6;
	}
	
	public static String toFormattedString( double[] arr ) {
		StringBuilder strbld = new StringBuilder("[");
		for ( int i=0; i<arr.length; ++i ) {
			if ( i > 0 ) strbld.append(", ");
			strbld.append(String.format("%.3f", arr[0]));
		}
		strbld.append("]");
		return strbld.toString();
	}
	
	public static Int2IntOpenHashMap getCounter( int[] arr ) {
		Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
		for ( int token : arr ) counter.addTo(token, 1);
		return counter;
	}

	public static Int2IntOpenHashMap getCounter( IntIterable iter ) {
		Int2IntOpenHashMap counter = new Int2IntOpenHashMap();
		for ( int token : iter ) counter.addTo(token, 1);
		return counter;
	}
	
	public static double getMemoryUsage() {
		// return in MB
		Runtime inst = Runtime.getRuntime();
		return (inst.totalMemory()-inst.freeMemory())/1e6;
	}
	
	public static BigInteger getSpaceUsage(String path) {
		return FileUtils.sizeOfAsBigInteger(new File(path));
	}
	
	public static void unzip( ObjectList<IntPair> pairList, IntList list1, IntList list2 ) {
		for ( IntPair pair : pairList ) {
			list1.add(pair.i1);
			list2.add(pair.i2);
		}
	}
	
	public static void addToIntList( IntList list, int c ) {
		for ( int i=0; i<list.size(); ++i ) list.set(i, list.get(i)+c);
	}
	
	public static IntList mergeSortedIntLists( ObjectList<IntList> intLists ) {
		int[] cur = new int[intLists.size()];
		IntBinaryHeap heap = new IntBinaryHeap(intLists.size());
		for ( int lidx=0; lidx<intLists.size(); ++lidx ) {
			IntList list = intLists.get(lidx);
			if ( list != null && list.size() > 0 ) {
				heap.insert(intLists.get(lidx).getInt(0), lidx);
			}
		}
		IntList mergedList = new IntArrayList();
		while ( !heap.isEmpty() ) {
			IntPair minPair = heap.poll();
			int pos = minPair.i1;
			int lidx = minPair.i2;
			mergedList.add(pos);
			cur[lidx] += 1;
			if ( cur[lidx] < intLists.get(lidx).size() ) heap.insert(intLists.get(lidx).getInt(cur[lidx]), lidx);
		}
		return mergedList;
	}
}
