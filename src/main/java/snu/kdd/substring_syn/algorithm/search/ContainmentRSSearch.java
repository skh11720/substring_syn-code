package snu.kdd.substring_syn.algorithm.search;

import snu.kdd.substring_syn.algorithm.validator.GreedyQueryContainmentValidator;
import snu.kdd.substring_syn.data.Dataset;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;
import snu.kdd.substring_syn.utils.Stat;

public class ContainmentRSSearch extends AbstractIndexBasedSearch {

	private final GreedyQueryContainmentValidator validator;
	
	public ContainmentRSSearch( double theta, IndexChoice indexChoice ) {
		super(theta, indexChoice);
		assert ( indexChoice == IndexChoice.None || indexChoice == IndexChoice.Count || indexChoice == IndexChoice.Naive );
		param.put("index_impl", indexChoice.toString());
		validator = new GreedyQueryContainmentValidator(theta, statContainer);
	}
	
	@Override
	protected final void prepareSearchGivenQuery(Record query) {
		super.prepareSearchGivenQuery(query);
	}
	
	@Override
	protected void searchRecordQuerySide( Record query, RecordInterface rec ) {
		statContainer.startWatch(Stat.Time_QS_Validation);
		boolean isSim = verifyQuerySide(query, rec);
		statContainer.stopWatch(Stat.Time_QS_Validation);
		if ( isSim ) addResultQuerySide(query, rec);
	}

	protected boolean verifyQuerySide( Record query, RecordInterface rec ) {
		double sim = validator.simQuerySide(query, rec);
		return sim >= theta;
	}
	
	@Override
	protected void searchRecordTextSide( Record query, TransformableRecordInterface rec ) {
		statContainer.startWatch(Stat.Time_TS_Validation);
		boolean isSim = verifyTextSide(query, rec);
		statContainer.stopWatch(Stat.Time_TS_Validation);
		if ( isSim ) addResultTextSide(query, rec);
	}

	protected boolean verifyTextSide( Record query, TransformableRecordInterface rec ) {
		double sim = validator.simTextSide(query, rec);
		return sim >= theta;
	}

	@Override
	public String getOutputName( Dataset dataset ) {
		return String.join( "_", getName(), getVersion(), indexChoice.toString(), String.format("%.2f", theta), dataset.name);
	}
	
	@Override
	public String getName() {
		return "ContainmentRSSearch";
	}

	@Override
	public String getVersion() {
		/*
		 * 1.00: initial version
		 * 1.01: enable skip in data-side
		 */
		return "1.01";
	}
}
