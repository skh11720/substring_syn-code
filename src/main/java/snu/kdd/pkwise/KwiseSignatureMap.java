package snu.kdd.pkwise;

import java.util.Map.Entry;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import snu.kdd.substring_syn.data.record.Record;

public class KwiseSignatureMap {

	private int idx;
	private final Object2IntMap<KwiseSignature> sig2keyMap;

	public KwiseSignatureMap( int startIdx ) {
		this.idx = startIdx;
		sig2keyMap = new Object2IntOpenHashMap<>();
		sig2keyMap.defaultReturnValue(-1);
	}
	
	public void put( int[] arr ) {
		KwiseSignature key = getKey(arr);
		putKey(key);
	}
	
	private void putKey( KwiseSignature key ) {
		sig2keyMap.put(key, idx);
		idx += 1;
	}
	
	public int get( int[] arr ) {
		KwiseSignature key = getKey(arr);
		if ( sig2keyMap.containsKey(key) ) return sig2keyMap.get(key);
		else return -1;
	}
	
	public int getIfExistsOrPut( int[] arr ) {
		KwiseSignature key = getKey(arr);
		if ( !sig2keyMap.containsKey(key) ) putKey(key);
		return sig2keyMap.get(key);
	}
	
	private KwiseSignature getKey( int[] arr ) {
		return new KwiseSignature(arr);
	}
	
	@Override
	public String toString() {
		StringBuilder bld = new StringBuilder(KwiseSignatureMap.class.getSimpleName()+":\n");
		for ( Entry<KwiseSignature, Integer> e : sig2keyMap.entrySet() ) {
			KwiseSignature key = e.getKey();
			StringBuilder bld1 = new StringBuilder();
			for ( int idx : key.getValues() ) bld1.append(Record.tokenIndex.getToken(idx)+", ");
			bld.append(String.format("  [%s (%s), %d]\n", key, bld1.toString(), e.getValue()));
		}
		return bld.toString();
	}
}
