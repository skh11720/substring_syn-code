package snu.kdd.faerie;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import snu.kdd.substring_syn.data.record.Record;

public class FaerieUtils {

	public static Int2ObjectMap<IntList> getInvIndex(Record rec) {
		Int2ObjectMap<IntList> tok2posListMap = new Int2ObjectOpenHashMap<IntList>();
		for ( int i=0; i<rec.size(); ++i ) {
			int token = rec.getToken(i);
			if ( !tok2posListMap.containsKey(token) ) tok2posListMap.put(token, new IntArrayList());
			tok2posListMap.get(token).add(i);
		}
		return tok2posListMap;
	}
}
