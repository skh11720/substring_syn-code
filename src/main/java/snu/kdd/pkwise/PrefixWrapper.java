package snu.kdd.pkwise;

import java.util.Collections;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class PrefixWrapper {
	public static TokenPartitioner partitioner;
	public IntArrayList prefix;
	public int[] nClassToken;
	public int cov;

	public void addToPrefix( int token ) {
		int pos = Collections.binarySearch(prefix, token);
		if ( pos >= 0 ) prefix.add(pos, token);
		else {
			pos = -pos-1;
			if ( pos >= prefix.size() ) prefix.add(token);
			else prefix.add(pos, token);
		}
		int cid = partitioner.getTokenClass(token);
		nClassToken[cid] += 1;
		if ( nClassToken[cid] >= cid+1 ) cov += 1;
	}
	
	public void removeFromPrefix( int token ) {
		int pos = Collections.binarySearch(prefix, token);
		if ( pos >= 0 ) {
			prefix.removeInt(pos);
			int cid = partitioner.getTokenClass(token);
			if ( nClassToken[cid] >= cid+1 ) cov -= 1;
			nClassToken[cid] = Math.max(nClassToken[cid]-1, 0);
		}
	}
	
	public final int get( int i ) {
		return prefix.getInt(i);
	}
	
	public final int size() {
		return prefix.size();
	}
}
