package vldb18;

import snu.kdd.substring_syn.algorithm.filter.TransSetBoundCalculatorInterface;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.record.Record;
import snu.kdd.substring_syn.data.record.RecordInterface;

public class PkduckDPExWIthLF extends PkduckDPEx {
	
	protected final TransSetBoundCalculatorInterface boundCalculator;
	
	public PkduckDPExWIthLF( Record query, RecordInterface rec, TransSetBoundCalculatorInterface boundCalculator, double theta ) {
		super(query, rec, theta);
		this.boundCalculator = boundCalculator;
	}

	@Override
	public void compute( int target ) {
		for (int i=1; i<=rec.size(); ++i) {
			// compute g[0][i][v][l].
			init();
			for (int v=1; v<=rec.size()-i+1; ++v) {
                if ( boundCalculator.getLFLBMono(i-1, i+v-2) > qSetSize ) break;
				for (int l=1; l<=boundCalculator.getUB(i-1, i+v-2); ++l) {
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
                if ( boundCalculator.getLFLBMono(i-1, i+v-2) > qSetSize ) break;
				for (int l=1; l<=boundCalculator.getUB(i-1, i+v-2); ++l) {
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

	@Override
	protected void updateResult( int i ) {
		for ( int v=1; v<=rec.size()-i+1; ++v ) {
			if ( boundCalculator.getLFLBMono(i-1, i+v-2) > qSetSize ) break;
			b[i][v] = computeIsInSigU(i, v);
		}
	}

	protected boolean computeIsInSigU( int i, int v ) {
		for (int l=boundCalculator.getLB(i-1, i+v-2); l<=boundCalculator.getUB(i-1, i+v-2); ++l) {
			if ( g[1][v][l] <= getPrefixLen(l)-1 ) return true;
		}
		return false;
	}
}
