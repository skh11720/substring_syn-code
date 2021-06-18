package snu.kdd.substring_syn.algorithm.search.variants;

import java.util.Set;

import snu.kdd.substring_syn.algorithm.search.ExactPositionRSSearch;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;

public class PerQueryExactPositionRSSearch extends ExactPositionRSSearch {

	public PerQueryExactPositionRSSearch(Dataset dataset, double theta, boolean bLF, boolean bPF, IndexChoice indexChoice) {
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
	
	@Override
	protected void addResultTextSide(Record query, RecordInterface rec) {
		/*
		 * NOTE: an element of the returned set is a pair of indices, not ids
		 */
		if (dataset.isDocInput()) rsltTextSide.add(new IntPair(query.getIdx(), dataset.getRid2idpairMap().get(rec.getIdx()).i1));
//		else rsltTextSide.add(new IntPair(query.getID(), rec.getID()));
		else rsltTextSide.add(new IntPair(query.getIdx(), rec.getIdx()));
	}
}
