package snu.kdd.faerie;

import java.math.BigInteger;

public interface FaerieIndexInterface {
	
	FaerieIndexEntry getEntry(int id);
	BigInteger diskSpaceUsage();
}
