package snu.kdd.faerie;

import java.io.Serializable;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.Records;

public class FaerieSynIndexEntry implements Serializable {
	
	final ObjectList<Int2ObjectMap<IntList>> invIndexList;
	
	public FaerieSynIndexEntry(Record rec) {
		rec.preprocessAll();
		invIndexList = new ObjectArrayList<>();
		for ( Record exp : Records.expands(rec) ) {
			invIndexList.add(FaerieUtils.getInvIndex(exp));
		}
	}
	
	@Override
	public String toString() {
		StringBuilder strbld = new StringBuilder("FaerieSynIndexEntry {\n");
		for ( int i=0; i<invIndexList.size(); ++i ) {
			Int2ObjectMap<IntList> invIndex = invIndexList.get(i);
			strbld.append("\t"+i+" [\n");
			for ( Entry<IntList> entry : invIndex.int2ObjectEntrySet() ) {
				strbld.append("\t\t"+entry.getIntKey()+":"+entry.getValue()+"\n");
			}
			strbld.append("\t],\n");
		}
		strbld.append("}");
		return strbld.toString();
	}

	private static final long serialVersionUID = 34239854729843L;
}
