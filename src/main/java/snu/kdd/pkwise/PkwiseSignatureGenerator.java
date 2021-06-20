package snu.kdd.pkwise;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntList;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.utils.Util;

public class PkwiseSignatureGenerator {

	private final TokenPartitioner partitioner;
	private final KwiseSignatureMap sigMap;
	private final int kmax;
	private final int[] nClassToken;
	
	public PkwiseSignatureGenerator( TokenPartitioner partitioner, KwiseSignatureMap sigMap, int kmax ) {
		this.partitioner = partitioner;
		PrefixWrapper.partitioner = partitioner;
		this.sigMap = sigMap;
		this.kmax = kmax;
		nClassToken = new int[kmax];
	}
	
	public final KwiseSignatureMap getSigMap() { return sigMap; }

	public final TokenPartitioner getPartitioner() { return partitioner; }
	
	public IntArrayList genSignature( RecordInterface rec, int maxDiff, boolean indexing ) {
		int l = getPrefixLength(rec, maxDiff);
		IntList prefix = new IntArrayList( rec.getTokenList().stream().sorted().limit(l).iterator() );
		return genSignature(prefix, indexing);
	}
	
	public IntArrayList genSignature ( IntList prefix, boolean indexing ) {
		IntArrayList sig = new IntArrayList();
		int l = prefix.size();
		int sidx = 0;
		int eidx = 0;
		for ( int cidx=0; cidx<kmax; ++cidx ) {
			while ( eidx < l && partitioner.getTokenClass(prefix.get(eidx)) == cidx ) eidx += 1;
			IntCollection ksig;
			if ( cidx == 0 ) ksig = prefix.subList(sidx, eidx);
			else ksig = genKwiseSignature(prefix.subList(sidx, eidx), cidx, indexing);
			if ( ksig != null ) sig.addAll(ksig);
			sidx = eidx;
		}
		sig.sort(Integer::compare);
		return sig;
	}
	
	public IntList genKwiseSignature( IntList list, int cidx, boolean indexing ) {
		if ( list.size() < cidx+1 ) return null;
		int[] arr = new int[cidx+1];
		IntList sig = new IntArrayList();
		List<IntArrayList> combList = Util.getCombinations(list.size(), cidx+1);
		for ( IntArrayList comb : combList ) {
			for ( int i=0; i<cidx+1; ++i ) {
				arr[i] = list.get(comb.getInt(i));
			}
			int key;
			if (indexing) key = sigMap.getIfExistsOrPut(arr);
			else key = sigMap.get(arr);
			sig.add(key);
		}
		return sig;
	}
	
	public int getPrefixLength( RecordInterface rec, int maxDiff ) {
		int cov = 0;
		int l = 0;
		Arrays.fill(nClassToken, 0);
		Iterator<Integer> iter = rec.getTokenList().stream().sorted().iterator();
		while ( iter.hasNext() ) {
			l += 1;
			int token = iter.next();
			int cid = partitioner.getTokenClass(token);
			nClassToken[cid] += 1;
			if ( nClassToken[cid] >= cid+1 ) {
				cov += 1;
				if ( cov == maxDiff ) break;
			}
		}
		return l;
	}
	
	public PrefixWrapper wrapPrefix( IntArrayList prefix ) {
		PrefixWrapper wprefix = new PrefixWrapper();
		wprefix.prefix = prefix;
		wprefix.cov = getCov(prefix);
		wprefix.nClassToken = Arrays.copyOf(nClassToken, kmax);
		return wprefix;
	}

	public int getCov( IntArrayList prefix ) {
		int cov = 0;
		Arrays.fill(nClassToken, 0);
		for ( int token : prefix ) {
			int cid = partitioner.getTokenClass(token);
			nClassToken[cid] += 1;
			if ( nClassToken[cid] >= cid+1 ) cov += 1;
		}
		return cov;
	}
	
	public int getCov( IntArrayList prefix, int ignored ) {
		int cov = 0;
		Arrays.fill(nClassToken, 0);
		for ( int token : prefix ) {
			if ( token == ignored ) continue;
			int cid = partitioner.getTokenClass(token);
			nClassToken[cid] += 1;
			if ( nClassToken[cid] >= cid+1 ) cov += 1;
		}
		return cov;
	}
	
	public void removeTrailingNonCoveringTokens( PrefixWrapper wprefix ) {
		for ( int i=wprefix.size()-1; i>=0; --i ) {
			int token = wprefix.prefix.getInt(i);
			if ( isNonCoveringToken(wprefix, token) ) wprefix.prefix.removeInt(i);
		}
	}
	
	public boolean isNonCoveringToken( PrefixWrapper wprefix, int token ) {
		int cid = partitioner.getTokenClass(token);
		if ( wprefix.nClassToken[cid] >= cid+1 ) return false;
		else return true;
	}
	
	public IntArrayList expandPrefix( PrefixWrapper wprefix, RecordInterface window ) {
		Iterator<Integer> iter = window.getTokenList().stream().sorted().skip(wprefix.size()).iterator();
		IntArrayList diffPrefix = new IntArrayList();
		int cov0 = wprefix.cov;
		while ( cov0 == wprefix.cov && iter.hasNext() ) {
			int token = iter.next();
			wprefix.addToPrefix(token);
			diffPrefix.add(token);
		}
		return diffPrefix;
	}

	public IntArrayList shrinkPrefix( PrefixWrapper wprefix ) {
		IntArrayList diffPrefix = new IntArrayList();
		int cov0 = wprefix.cov;
		for ( int i=wprefix.size()-1; cov0 == wprefix.cov; --i ) {
			int token = wprefix.prefix.getInt(i);
			wprefix.removeFromPrefix(token);
			diffPrefix.add(token);
		}
		return diffPrefix;
	}
}
