package vldb18;

import java.util.Arrays;

import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.TransformableRecordInterface;

public class PkduckDPEx {
	
	protected final int maxTransLen;
	protected final Record query;
	protected final TransformableRecordInterface rec;
	protected final double theta;
	protected final int[][][] g;
	protected final boolean[][] b;
	
	
	public PkduckDPEx( Record query, TransformableRecordInterface rec, double theta ) {
		this.query = query;
		this.rec = rec;
		this.theta = theta;
		this.maxTransLen = rec.getMaxTransLength();
		this.g = new int[2][rec.size()+1][maxTransLen+1];
		this.b = new boolean[rec.size()+1][rec.size()+1];
		for (boolean[] bArr : b) Arrays.fill(bArr, false);
	}

	public void compute( int target ) {
		for (int i=1; i<=rec.size(); ++i) {
			// compute g[0][i][v][l].
			init();
			for (int v=1; v<=rec.size()-i+1; ++v) {
				for (int l=1; l<=maxTransLen; ++l) {
					for (Rule rule : rec.getSuffixApplicableRules( i+v-2 )) {
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
							g[0][v][l] = Math.min( g[0][v][l], g[0][v-rule.lhsSize()][l-rule.rhsSize()]+num_smaller );
					}
	//				System.out.println( "g[0]["+i+"]["+l+"]: "+g[0][i][l] );
				}
			}
	//		System.out.println(Arrays.deepToString(g[0]).replaceAll( "],", "]\n" ));
		
			// compute g[1][i][l].
			for (int v=1; v<=rec.size()-i+1; ++v) {
				for (int l=1; l<=maxTransLen; ++l) {
					for (Rule rule : rec.getSuffixApplicableRules( i+v-2 )) {
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
							g[1][v][l] = Math.min( g[1][v][l], g[1][v-rule.lhsSize()][l-rule.rhsSize()]+num_smaller );
							if (isValid) g[1][v][l] = Math.min( g[1][v][l], g[0][v-rule.lhsSize()][l-rule.rhsSize()]+num_smaller );
						}
					}
	//				System.out.println( "g[1]["+i+"]["+l+"]: "+g[1][i][l] );
				}
			}
	//		System.out.println(Arrays.deepToString(g[1]).replaceAll( "],", "]\n" ));
			updateResult(i);
		} // end for i
	}
	
	protected void init() {
		for ( int o=0; o<2; ++o ) {
			for ( int v=0; v<=rec.size(); ++v ) {
				Arrays.fill( g[o][v], maxTransLen+1 );
			}
		}
		g[0][0][0] = 0;
	}
	
	protected void updateResult( int i ) {
		for ( int v=1; v<=rec.size(); ++v ) {
			b[i][v] = computeIsInSigU(v);
		}
	}
	
	protected boolean computeIsInSigU( int v ) {
		for (int l=1; l<=maxTransLen; ++l) {
			if ( g[1][v][l] <= getPrefixLen(l)-1 ) return true;
		}
		return false;
	}
	
	protected int getPrefixLen( int len ) {
		return len - (int)(Math.ceil(theta*len)) + 1;
	}
	
	public boolean isInSigU( int i, int v ) {
		/*
		 * i: 0-based
		 * b[][]: 1-based
		 */
		return b[i+1][v];
	}
}
