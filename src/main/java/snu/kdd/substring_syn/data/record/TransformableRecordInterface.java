package snu.kdd.substring_syn.data.record;

import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;

public interface TransformableRecordInterface extends RecordInterface {
	
	int getNumApplicableRules(int pos);
	int getMaxTransLength();
	int getMaxRhsSize();
	IntSet getCandTokenSet();
	Iterable<Rule> getApplicableRuleIterable();
	Iterable<Rule> getApplicableRules( int i );
	Iterable<Rule> getSuffixApplicableRules( int i );
	IntPair[] getSuffixRuleLens(int k);
}
