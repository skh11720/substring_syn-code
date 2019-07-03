package snu.kdd.substring_syn.algorithm.index;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import snu.kdd.substring_syn.data.Record;

public class IndexBasedFilter {

	protected final InvertedIndex index;
	protected final double theta;
	
	public IndexBasedFilter( InvertedIndex index, double theta ) {
		this.index = index;
		this.theta = theta;
	}
	
	public ObjectList<Record> querySideCountFilter( Record query ) {
		int minCount = (int)Math.ceil(theta*query.getTransSetLB());
		Object2IntOpenHashMap<Record> counter = new Object2IntOpenHashMap<>();
		counter.defaultReturnValue(0);
		IntSet candTokenSet = query.getCandTokenSet();
		for ( int token : candTokenSet ) {
			ObjectList<Record> invList = index.getInvList(token);
			if ( invList == null ) continue;
			for ( Record rec : invList ) counter.addTo(rec, 1);
		}

		ObjectList<Record> candRecordList = new ObjectArrayList<>();
		for ( Object2IntMap.Entry<Record> entry : counter.object2IntEntrySet() ) {
			Record rec = entry.getKey();
			int count = entry.getIntValue();
			if ( count >= minCount ) candRecordList.add(rec);
		}
		return candRecordList;
	}
}
