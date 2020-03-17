package snu.kdd.faerie;

import java.math.BigInteger;

public interface FaerieSynIndexInterface {
	
	FaerieSynIndexEntry getEntry(int idx);
	Iterable<FaerieSynIndexEntry> getRecordEntries(int idx);
	BigInteger diskSpaceUsage();
}
