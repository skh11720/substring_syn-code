package vldb18;

import java.util.Arrays;

import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Rule;

public class PkduckDP {
	
	protected final int maxTransLen;
	protected final Record rec;
	protected final double theta;
	protected final int[] tokens;
	
	
	public PkduckDP( Record rec, double theta ) {
		this.rec = rec;
		this.theta = theta;
		this.tokens = rec.getTokensArray();
		this.maxTransLen = rec.getMaxTransLength();
	}

	public Boolean isInSigU( int target ) {
		/*
		 * Compute g[o][i][l] for o=0,1, i=0~|rec|, l=0~max(|recS|).
		 * g[1][i][l] is X_l in the MIT paper.
		 */
//		System.out.println( "PkduckIndex.isInSigU, "+target_qgram+", "+k );
		
		int[][][] g = initEntries();

		// compute g[0][i][l].
		for (int i=1; i<=rec.size(); i++) {
			int token = tokens[i-1];
			for (int l=1; l<=maxTransLen; l++) {
				int comp = Integer.compare( token, target );
//				System.out.println( "comp: "+comp );
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
				if ( comp != 0 ) g[0][i][l] = Math.min( g[0][i][l], g[0][i-1][l-1] + (comp<0?1:0) );
//				System.out.println( "g[0]["+(i-1)+"]["+(l-1)+"]: "+g[0][i-1][l-1] );
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
				for (Rule rule : rec.getSuffixApplicableRules( i-1 )) {
//					System.out.println( rule );
					int num_smaller = 0;
					Boolean isValid = true;
					for ( int tokenInRhs : rule.getRhs() ) {
						// check whether the rule does not generate [target_token, k].
						isValid &= (tokenInRhs != target);
						num_smaller += (tokenInRhs < target)?1:0;
					}
//					System.out.println( "isValid: "+isValid );
//					System.out.println( "num_smaller: "+num_smaller );
					if (isValid && i-rule.lhsSize() >= 0 && l-rule.rhsSize() >= 0) 
						g[0][i][l] = Math.min( g[0][i][l], g[0][i-rule.lhsSize()][l-rule.rhsSize()]+num_smaller );
				}
//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
			}
		}
//		System.out.println(Arrays.deepToString(g[0]).replaceAll( "],", "]\n" ));
		
		// compute g[1][i][l].
		for (int i=1; i<=rec.size(); i++ ) {
			int token = tokens[i-1];
			for (int l=1; l<=maxTransLen; l++) {
				int comp = Integer.compare( token, target );
//				System.out.println( "comp: "+comp );
				if ( comp != 0 ) g[1][i][l] = Math.min( g[1][i][l], g[1][i-1][l-1]+(comp<0?1:0));
				else g[1][i][l] = Math.min( g[1][i][l], g[0][i-1][l-1] );
//				System.out.println( "g[1]["+i+"]["+l+"]: "+g[1][i][l] );
				for (Rule rule : rec.getSuffixApplicableRules( i-1 )) {
//					System.out.println( rule );
					int num_smaller = 0;
					Boolean isValid = false;
					for ( int tokenInRhs : rule.getRhs() ) {
						// check whether the rule generates [target_token, k].
						isValid |= (tokenInRhs == target);
						num_smaller += (tokenInRhs < target)?1:0;
					}
//					System.out.println( "isValid: "+isValid );
//					System.out.println( "num_smaller: "+num_smaller );
					if ( i-rule.lhsSize() >= 0 && l-rule.rhsSize() >= 0) {
						g[1][i][l] = Math.min( g[1][i][l], g[1][i-rule.lhsSize()][l-rule.rhsSize()]+num_smaller );
						if (isValid) g[1][i][l] = Math.min( g[1][i][l], g[0][i-rule.lhsSize()][l-rule.rhsSize()]+num_smaller );
					}
				}
//				System.out.println( "g[1]["+i+"]["+l+"]: "+g[1][i][l] );
			}
		}
//		System.out.println(Arrays.deepToString(g[1]).replaceAll( "],", "]\n" ));

		for (int l=1; l<=maxTransLen; l++) {
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
