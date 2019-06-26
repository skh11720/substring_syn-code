package vldb18;

import snu.kdd.substring_syn.algorithm.filter.TransSetBoundCalculator3;
import snu.kdd.substring_syn.data.Record;
import snu.kdd.substring_syn.data.Rule;

public class PkduckDPEx2 extends PkduckDPEx {
	
	protected final TransSetBoundCalculator3 boundCalculator;
	
	public PkduckDPEx2( Record rec, TransSetBoundCalculator3 boundCalculator, double theta, int qlen ) {
		super(rec, theta, qlen);
		this.boundCalculator = boundCalculator;
	}

	@Override
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
                if ( theta*boundCalculator.getLBMono(i-1, v-1) > qlen ) break;
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
                if ( theta*boundCalculator.getLBMono(i-1, v-1) > qlen ) break;
			}
	//		System.out.println(Arrays.deepToString(g[1]).replaceAll( "],", "]\n" ));
			updateResult(i);
		} // end for i
	}
}
