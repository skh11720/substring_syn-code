package snu.kdd.substring_syn.data;

import it.unimi.dsi.fastutil.ints.IntList;

public interface RecordInterface {

	IntList getTokenList();
	int getMaxTransLength();
	int size();
	Rule[] getSuffixApplicableRules(int i);
	public String toString();
	public String toOriginalString();
}
