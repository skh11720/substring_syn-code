package snu.kdd.substring_syn.data;

import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.Log;
import snu.kdd.substring_syn.utils.Stat;
import snu.kdd.substring_syn.utils.StatContainer;

public abstract class Dataset extends AbstractDocDataset {
	
	public final DatasetParam param;
	public final String name;
	public final int size;
	public final Ruleset ruleset;
	public final StatContainer statContainer;



	protected Dataset(StatContainer statContainer, DatasetParam param, Ruleset ruleset) {
		Log.log.trace("Dataset.constructor");
		this.param = param;
		this.name = param.getDatasetName();
		this.size = Integer.parseInt(param.size);
		this.ruleset = ruleset;
		this.statContainer = statContainer;
		statContainer.setStat(Stat.Dataset_Name, name);
		statContainer.setStat(Stat.Dataset_nt, param.size);
		statContainer.setStat(Stat.Dataset_nr, param.nr);
		statContainer.setStat(Stat.Dataset_qlen, param.qlen);
		statContainer.setStat(Stat.Dataset_lr, param.lenRatio);
		statContainer.setStat(Stat.Dataset_nar, param.nar);
	}
	
	public void addStat() {
		Iterable<Record> searchedList = getSearchedList();
		statContainer.setStat(Stat.Dataset_numSearched, Integer.toString(getSize(searchedList)));
		statContainer.setStat(Stat.Len_SearchedAll, Long.toString(getLengthSum(searchedList)));
		statContainer.setStat(Stat.Dataset_numRule, Integer.toString(ruleset.size()));
	}
	
	protected final long getLengthSum( Iterable<Record> recordList ) {
		long sum = 0;
		for ( Record rec : recordList ) sum += rec.size();
		return sum;
	}
	
	protected final int getSize( Iterable<Record> recordList ) {
		int n = 0;
		for ( @SuppressWarnings("unused") Record rec : recordList ) ++n;
		return n;
	}

	public final Iterable<Record> getSearchedList() {
		return DatasetFactory.searchedRecords();
	}

	public abstract Iterable<TransformableRecordInterface> getIndexedList();

	public abstract Record getRawRecord(int idx);
	
	public abstract TransformableRecordInterface getRecord(int idx);
}
