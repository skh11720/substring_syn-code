package snu.kdd.substring_syn.utils;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import snu.kdd.substring_syn.data.ACAutomataR;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.Record;

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

	public static Dataset getDataset( String name, long size ) throws IOException {

		String osName = System.getProperty( "os.name" );
		String prefix = null;
		String sep = null;
		if ( osName.startsWith( "Windows" ) ) {
			prefix = "D:\\ghsong\\data\\synonyms\\";
			sep = "\\\\";
		}
		else if ( osName.startsWith( "Linux" ) ) {
			prefix = "data_store/";
			sep = "/";
		}
		
		String searchedPath, indexedPath, rulePath;
		if ( name.equals( "AOL" )) {
			searchedPath = prefix + String.format( "aol"+sep+"splitted"+sep+"aol_%d_data.txt", size );
			indexedPath = prefix + String.format( "aol"+sep+"splitted"+sep+"aol_%d_data.txt", size );
			rulePath = prefix + "wordnet"+sep+"rules.noun";
		}
		else if ( name.equals( "AOL_1K" )) {
			searchedPath = prefix + String.format( "aol"+sep+"splitted"+sep+"aol_1000_data.txt" );
			indexedPath = prefix + String.format( "aol"+sep+"splitted"+sep+"aol_1000_data.txt" );
			rulePath = prefix + "wordnet"+sep+"rules.noun";
		}
		else if ( name.equals( "SPROT" ) ) {
			searchedPath = prefix + String.format( "sprot"+sep+"splitted"+sep+"SPROT_two_%d.txt", size );
			indexedPath = prefix + String.format( "sprot"+sep+"splitted"+sep+"SPROT_two_%d.txt", size );
			rulePath = prefix + "sprot"+sep+"rule.txt";
		}
		else if ( name.equals( "USPS" ) ) {
			searchedPath = prefix + String.format( "JiahengLu"+sep+"splitted"+sep+"USPS_%d.txt", size );
			indexedPath = prefix + String.format( "JiahengLu"+sep+"splitted"+sep+"USPS_%d.txt", size );
			rulePath = prefix + "JiahengLu"+sep+"USPS_rule.txt";
		}
		else if ( name.startsWith("NAMES") ) {
			searchedPath = prefix + String.format( name+sep+name+"_freebase.txt" );
			indexedPath = prefix + String.format( name+sep+name+"_sport.txt" );
			rulePath = prefix + "JiahengLu"+sep+"USPS_rule.txt";
		}
		else if ( name.startsWith( "UNIV" ) ) {
			searchedPath = prefix + String.format( name+sep+name+"_data.txt" );
			indexedPath = prefix + String.format( name+sep+name+"_data.txt" );
			rulePath = prefix + String.format( name+sep+name+"_rule.txt" );
		}
		else if ( name.startsWith( "CONF" ) ) {
			searchedPath = prefix + String.format( name+sep+name+"_data.txt" );
			indexedPath = prefix + String.format( name+sep+name+"_data.txt" );
			rulePath = prefix + name+sep+name+"_rule.txt";
		}
		else if ( name.startsWith("POLY") ) {
			searchedPath = prefix + String.format( name+sep+name+"_data.txt" );
			indexedPath = prefix + String.format( name+sep+name+"_data.txt" );
			rulePath = prefix + name+sep+name+"_rule.txt";
		}
		else if ( name.equals( "SPROT_long" ) ) {
			searchedPath = prefix + String.format( "sprot_long"+sep+"splitted"+sep+"SPROT_short_%d.txt", size );
			indexedPath = prefix + String.format( "sprot_long"+sep+"splitted"+sep+"SPROT_long_%d.txt", size );
			rulePath = prefix + "sprot_long"+sep+"rule.txt";
		}
		else throw new RuntimeException();

		String outputPath = "output";
		Dataset dataset = new Dataset(rulePath, searchedPath, indexedPath, outputPath);

		return dataset;
	}

	public static Dataset getDatasetWithPreprocessing( String name, int size ) throws IOException {
		Dataset dataset = getDataset(name, size);
		for( final Record record : dataset.searchedList ) {
			record.preprocessApplicableRules( dataset.getAutomataR() );
			record.preprocessSuffixApplicableRules();
			record.preprocessTransformLength();
			record.preprocessEstimatedRecords();
		}
		if ( !dataset.selfJoin ) {
			for( final Record record : dataset.indexedList ) {
				record.preprocessApplicableRules( dataset.getAutomataR() );
				record.preprocessSuffixApplicableRules();
				record.preprocessTransformLength();
				record.preprocessEstimatedRecords();
			}
		}
		return dataset;
	}

	public static String getGroundTruthPath( String name ) {

		String osName = System.getProperty( "os.name" );
		String prefix = null;
		String sep = null;
		if ( osName.startsWith( "Windows" ) ) {
			prefix = "D:\\ghsong\\data\\synonyms\\";
			sep = "\\\\";
		}
		else if ( osName.startsWith( "Linux" ) ) {
			prefix = "data_store/";
			sep = "/";
		}
		return prefix + name+sep+name+"_groundtruth.txt";
	}
	
	public static int getPrefixLength( Record rec, double theta ) {
		return rec.size() - (int)(Math.ceil(theta*rec.size())) + 1;
	}

	public static IntOpenHashSet getPrefix( Record rec, double theta ) {
		int prefixLen = getPrefixLength(rec, theta);
		return new IntOpenHashSet( rec.getTokens().stream().sorted().limit(prefixLen).iterator() );
	}
	
	public static IntOpenHashSet getExpandedPrefix( Record rec, double theta ) {
		IntOpenHashSet prefix = new IntOpenHashSet();
		for ( Record exp : rec.expandAll() ) {
			int prefixLen = getPrefixLength(exp, theta);
			exp.getTokens().stream().sorted().limit(prefixLen).forEach(t -> prefix.add(t));
		}
		return prefix;
	}
	
	public static boolean hasIntersection( IntSet set0, IntSet set1 ) {
		IntSet smallSet = set1.size() < set0.size()? set1: set0;
		IntSet largeSet = set1.size() < set0.size()? set0: set1;
		for ( int token : smallSet ) {
			if ( largeSet.contains(token) ) return true;
		}
		return false;
	}
}
