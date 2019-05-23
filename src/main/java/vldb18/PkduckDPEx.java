package vldb18;

import java.util.Arrays;

import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Rule;

public class PkduckDPEx {
	
	protected final int maxTransLen;
	protected final Record rec;
	protected final double theta;
	protected final int[] tokens;
	protected int[][][][] g;
	
	
	public PkduckDPEx( Record rec, double theta ) {
		this.rec = rec;
		this.theta = theta;
		this.tokens = rec.getTokenArray();
		this.maxTransLen = rec.getMaxTransLength();
	}

	public void compute( int target ) {
		g = initEntries();

		// compute g[0][i][v][l].
		for (int i=1; i<=rec.size(); ++i) {
			for (int v=1; v<=rec.size()-i+1; ++v) {
				for (int l=1; l<=maxTransLen; ++l) {
					for (Rule rule : rec.getSuffixApplicableRules( i+v-2 )) {
						if ( rule.lhsSize() > v ) continue;
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
						if ( isValid && v-rule.lhsSize() >= 0 && l-rule.rhsSize() >= 0 )
							g[0][i][v][l] = Math.min( g[0][i][v][l], g[0][i][v-rule.lhsSize()][l-rule.rhsSize()]+num_smaller );
					}
	//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
				}
			}
	//		System.out.println(Arrays.deepToString(g[0]).replaceAll( "],", "]\n" ));
		}
		
		// compute g[1][i][l].
		for (int i=1; i<=rec.size(); ++i) {
			for (int v=1; v<=rec.size()-i+1; ++v) {
				for (int l=1; l<=maxTransLen; ++l) {
					for (Rule rule : rec.getSuffixApplicableRules( i+v-2 )) {
						if ( rule.lhsSize() > v ) continue;
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
						if ( v-rule.lhsSize() >= 0 && l-rule.rhsSize() >= 0 ) {
							g[1][i][v][l] = Math.min( g[1][i][v][l], g[1][i][v-rule.lhsSize()][l-rule.rhsSize()]+num_smaller );
							if (isValid) g[1][i][v][l] = Math.min( g[1][i][v][l], g[0][i][v-rule.lhsSize()][l-rule.rhsSize()]+num_smaller );
						}
					}
	//				System.out.println( "g[1]["+i+"]["+l+"]: "+g[1][i][l] );
				}
			}
	//		System.out.println(Arrays.deepToString(g[1]).replaceAll( "],", "]\n" ));
		}
	}
	
	public boolean isInSigU( int i, int v ) {
		for (int l=1; l<=maxTransLen; ++l) {
			if ( g[1][i+1][v][l] <= getPrefixLen(l)-1 ) return true;
		}
		return false;
	}
	
	protected int[][][][] initEntries() {
		int[][][][] g = new int[2][rec.size()+1][rec.size()+1][maxTransLen+1];
		for ( int o=0; o<2; ++o ) {
			for ( int i=0; i<=rec.size(); ++i ) {
				for ( int v=0; v<=rec.size(); ++v ) {
					Arrays.fill( g[o][i][v], maxTransLen+1 );
				}
			}
		}
		for ( int i=0; i<=rec.size(); ++i ) {
			g[0][i][0][0] = 0;
		}
		return g;
	}
	
	protected int getPrefixLen( int len ) {
		return len - (int)(Math.ceil(theta*len)) + 1;
	}
}
