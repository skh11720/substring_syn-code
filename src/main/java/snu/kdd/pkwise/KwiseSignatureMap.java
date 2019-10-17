package snu.kdd.pkwise;

import java.util.Map.Entry;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import snu.kdd.substring_syn.data.record.Record;

public class KwiseSignatureMap {

	private final int sidx;
	private int idx;
	private final Object2IntMap<KwiseSignature> sig2keyMap;
	private final Int2ObjectMap<KwiseSignature> key2sigMap;

	public KwiseSignatureMap() {
		sidx = Record.tokenIndex.getMaxID()+1;
		idx = sidx;
		sig2keyMap = new Object2IntOpenHashMap<>();
		sig2keyMap.defaultReturnValue(-1);
		key2sigMap = new Int2ObjectOpenHashMap<>();
		key2sigMap.defaultReturnValue(null);
	}
	
	public KwiseSignature get( int key ) {
		return key2sigMap.get(key);
	}
	
	public void put( int[] arr ) {
		KwiseSignature key = getKey(arr);
		putKey(key);
	}
	
	private void putKey( KwiseSignature key ) {
		sig2keyMap.put(key, idx);
		key2sigMap.put(idx, key);
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
