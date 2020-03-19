package snu.kdd.substring_syn.data.record;

import it.unimi.dsi.fastutil.ints.IntSet;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;

public interface TransformableRecordInterface extends RecordInterface {
	
	int getNumApplicableRules(int i);
	int getNumSuffixApplicableRules(int i);
	int getNumSuffixRuleLens(int i);
	int getMaxTransLength();
	int getMaxRhsSize();
	IntSet getCandTokenSet();
	Iterable<Rule> getApplicableRuleIterable();
	Iterable<Rule> getApplicableRules( int i );
	Iterable<Rule> getSuffixApplicableRules( int i );
	Iterable<IntPair> getSuffixRuleLens(int i);
}
