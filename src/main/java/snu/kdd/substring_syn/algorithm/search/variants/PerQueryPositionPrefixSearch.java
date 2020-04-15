package snu.kdd.substring_syn.algorithm.search.variants;

import java.util.Set;

import snu.kdd.substring_syn.algorithm.search.PositionPrefixSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;

public class PerQueryPositionPrefixSearch extends PositionPrefixSearch{
	
	

	public PerQueryPositionPrefixSearch(Dataset dataset, double theta, boolean bLF, boolean bPF, IndexChoice indexChoice) {
		super(theta, bLF, bPF, indexChoice);
		this.dataset = dataset;
		buildIndex(dataset);
	}

	public final Set<IntPair> searchTextSideGivenQuery(Record query) {
		rsltTextSide.clear();
//		long ts = System.nanoTime();
		searchTextSide(query, dataset);
//		double searchTime = (System.nanoTime()-ts)/1e6;
//		Log.log.info("search(query=%d, ...)\t%.3f ms", ()->query.getID(), ()->searchTime);
		return rsltTextSide;
	}
}
