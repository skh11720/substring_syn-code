package snu.kdd.substring_syn.data.record;

import snu.kdd.substring_syn.data.Rule;

public interface TransformableRecordInterface extends RecordInterface {
	
	Iterable<Rule> getApplicableRuleIterable();
	Iterable<Rule> getApplicableRules( int i );
	Iterable<Rule> getSuffixApplicableRules( int i );
	int getNumApplicableRules(int pos);
	int getMaxTransLength();
}
