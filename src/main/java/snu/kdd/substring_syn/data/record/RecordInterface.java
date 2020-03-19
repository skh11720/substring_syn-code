package snu.kdd.substring_syn.data.record;

import it.unimi.dsi.fastutil.ints.IntList;

public interface RecordInterface {
	int getIdx();
	int getID();
	int size();

	int getToken(int i);
	int[] getTokenArray();
	IntList getTokenList();
	
	String toString();
	String toOriginalString();
	String toStringDetails();
}
