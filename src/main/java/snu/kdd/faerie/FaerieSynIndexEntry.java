package snu.kdd.faerie;

import java.io.Serializable;
import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.IntList;
import snu.kdd.substring_syn.data.record.Record;

public class FaerieSynIndexEntry implements Serializable {
	
	final int[] recExpTokenArr;
	final Int2ObjectMap<IntList> invIndex;
	
	public FaerieSynIndexEntry(Record recExp) {
		this.recExpTokenArr = recExp.getTokenArray();
		invIndex = FaerieUtils.getInvIndex(recExp);
	}
	
	@Override
	public String toString() {
		StringBuilder strbld = new StringBuilder("FaerieSynIndexEntry {\n");
		strbld.append("\trecExp="+Arrays.toString(recExpTokenArr)+"\n");
		for ( Entry<IntList> entry : invIndex.int2ObjectEntrySet() ) {
			strbld.append("\t"+entry.getIntKey()+":"+entry.getValue()+"\n");
		}
		strbld.append("}");
		return strbld.toString();
	}

	private static final long serialVersionUID = 34239854729843L;
}
