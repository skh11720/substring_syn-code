package vldb18;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntList;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.RecordInterface;

public class PkduckDP {
	
	protected final int maxTransLen;
	protected final RecordInterface rec;
	protected final double theta;
	protected final IntList tokenList;
	
	
	public PkduckDP( RecordInterface rec, double theta ) {
		this.rec = rec;
		this.theta = theta;
		this.tokenList = rec.getTokenList();
		this.maxTransLen = rec.getMaxTransLength();
	}

	public Boolean isInSigU( int target ) {
		/*
		 * Compute g[o][i][l] for o=0,1, i=0~|rec|, l=0~max(|recS|).
		 * g[1][i][l] is X_l in the MIT paper.
		 */
		
		int[][][] g = initEntries();

		for (int l=1; l<=maxTransLen; l++) {
			for (int i=1; i<=rec.size(); i++) {
				for (Rule rule : rec.getSuffixApplicableRules( i-1 )) {
					int num_smaller = 0;
					Boolean isValid = true;
					for ( int tokenInRhs : rule.getRhs() ) {
						isValid &= (tokenInRhs != target);
						num_smaller += (tokenInRhs < target)?1:0;
					}
					if (isValid && i-rule.lhsSize() >= 0 && l-rule.rhsSize() >= 0) 
						g[0][i][l] = Math.min( g[0][i][l], g[0][i-rule.lhsSize()][l-rule.rhsSize()]+num_smaller );
				}
			}
		}
		
		for (int l=1; l<=maxTransLen; l++) {
			for (int i=1; i<=rec.size(); i++ ) {
				for (Rule rule : rec.getSuffixApplicableRules( i-1 )) {
					int num_smaller = 0;
					Boolean isValid = false;
					for ( int tokenInRhs : rule.getRhs() ) {
						isValid |= (tokenInRhs == target);
						num_smaller += (tokenInRhs < target)?1:0;
					}
					if ( i-rule.lhsSize() >= 0 && l-rule.rhsSize() >= 0) {
						g[1][i][l] = Math.min( g[1][i][l], g[1][i-rule.lhsSize()][l-rule.rhsSize()]+num_smaller );
						if (isValid) g[1][i][l] = Math.min( g[1][i][l], g[0][i-rule.lhsSize()][l-rule.rhsSize()]+num_smaller );
					}
				}
			}
			if ( g[1][rec.size()][l] <= getPrefixLen(l)-1 ) return true;
		}
		
		return false;
	}
	
	protected int[][][] initEntries() {
		int[][][] g = new int[2][rec.size()+1][maxTransLen+1];
		for (int o=0; o<2; o++) {
			for (int i=0; i<=rec.size(); i++ ) {
				Arrays.fill( g[o][i], maxTransLen+1 );
			}
		}
		g[0][0][0] = 0;
		return g;
	}
	
	protected int getPrefixLen( int len ) {
		return len - (int)(Math.ceil(theta*len)) + 1;
	}
}
