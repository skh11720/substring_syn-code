package snu.kdd.substring_syn.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public abstract class AbstractDocDataset {
	
	Int2ObjectMap<IntPair> rid2idpairMap = null;
	
	public final Int2ObjectMap<IntPair> getRid2idpairMap() {
		return rid2idpairMap;
	}
	
	public final boolean isDocInput() {
		return rid2idpairMap != null;
	}
}
