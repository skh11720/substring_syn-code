package vldb18;

import java.util.Arrays;

import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Rule;

public class PkduckDPEx {
	
	protected final int maxTransLen;
	protected final Record rec;
	protected final double theta;
	protected final int[] tokens;
	protected final int[][][] g;
	protected final boolean[][] b;
	protected final int[] lmin;
	protected final int qlen;
	
	
	public PkduckDPEx( Record rec, double theta, int qlen ) {
		this.rec = rec;
		this.theta = theta;
		this.tokens = rec.getTokenArray();
		this.maxTransLen = rec.getMaxTransLength();
		this.g = new int[2][rec.size()+1][maxTransLen+1];
		this.b = new boolean[rec.size()+1][rec.size()+1];
		for (boolean[] bArr : b) Arrays.fill(bArr, false);
		this.lmin = new int[rec.size()+1];
		Arrays.fill(lmin, Integer.MAX_VALUE);
		this.lmin[0] = 0;
		this.qlen = qlen;
	}

	public void compute( int target ) {
		for (int i=1; i<=rec.size(); ++i) {
			// compute g[0][i][v][l].
			init();
			for (int v=1; v<=rec.size()-i+1; ++v) {
				for (int l=1; l<=maxTransLen; ++l) {
					for (Rule rule : rec.getSuffixApplicableRules( i+v-2 )) {
	//					System.out.println( rule );
						if ( rule.lhsSize() <= v ) lmin[v] = Math.min(lmin[v], lmin[v-rule.lhsSize()]+rule.rhsSize());
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
				if ( theta*lmin[v] > qlen ) break;
			}
	//		System.out.println(Arrays.deepToString(g[1]).replaceAll( "],", "]\n" ));
			updateResult(i);
		} // end for i
	}
	
	protected void init() {
		init_g();
		init_lmin();
	}

	protected void init_g() {
		for ( int o=0; o<2; ++o ) {
			for ( int v=0; v<=rec.size(); ++v ) {
				Arrays.fill( g[o][v], maxTransLen+1 );
			}
		}
		g[0][0][0] = 0;
	}
	
	protected void init_lmin() {
		for ( int v=0; v<=rec.size(); ++v ) {
			lmin[v] = v;
		}
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
