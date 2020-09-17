package snu.kdd.faerie;

import java.math.BigInteger;

public interface FaerieIndexInterface {
	
	FaerieIndexEntry getEntry(int idx);
	BigInteger diskSpaceUsage();
}
