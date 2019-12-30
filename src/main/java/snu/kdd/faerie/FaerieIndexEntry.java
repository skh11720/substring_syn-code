package snu.kdd.faerie;

import java.io.Serializable;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import snu.kdd.substring_syn.data.record.Record;

public class FaerieIndexEntry implements Serializable {
	
	final Int2ObjectMap<IntList> tok2posListMap;
	
	public FaerieIndexEntry(Record rec) {
		tok2posListMap = FaerieUtils.getInvIndex(rec);
	}
	
	@Override
	public String toString() {
		StringBuilder strbld = new StringBuilder("FaerieIndexEntry {\n");
		for ( Int2ObjectMap.Entry<IntList> entry : tok2posListMap.int2ObjectEntrySet() ) {
			strbld.append("\t"+entry.getIntKey()+":"+entry.getValue()+"\n");
		}
		strbld.append("}");
		return strbld.toString();
	}

	private static final long serialVersionUID = 34239854729843L;
}
