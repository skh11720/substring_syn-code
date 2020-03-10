package snu.kdd.substring_syn.data.record;

import java.util.Arrays;
import java.util.Iterator;

import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;

public class ReusableRecord extends Record {
	
	int size = 0;
	int[] nApp;
	int[] nSapp;
	int[] nSRL;

	public ReusableRecord(int id, int[] tokens) {
		super(id, tokens);
		size = tokens.length;
		nApp = new int[size];
		nSapp = new int[size];
		nSRL = new int[size];
		for ( int i=0; i<tokens.length; ++i ) this.tokens[i] = tokens[i];
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public void setHash() {
		this.hash = getHash();
	}
	
	public void fit(int size) {
		while ( tokens.length < size) doubleSize();
		this.size = size;
	}
	
	public void fitApp(int i, int n) {
		while ( applicableRules[i].length < n ) applicableRules[i] = doubleRuleArray(applicableRules[i]); 
		nApp[i] = n;
	}
	
	public void fitSapp(int i, int n) {
		while ( suffixApplicableRules[i].length < n ) suffixApplicableRules[i] = doubleRuleArray(suffixApplicableRules[i]);
		nSapp[i] = n;
	}
	
	public void fitSRL(int i, int n) {
		while ( suffixRuleLenPairs[i].length < n ) suffixRuleLenPairs[i] = doubleIntPairArray(suffixRuleLenPairs[i]);
		nSRL[i] = n;
	}
	
	private void doubleSize() {
		doubleTokenArray();
		doubleAppRulesArray();
		doubleSappRulesArray();
		doubleTransformLengthsArray();
		doubleSRLPairsArray();
	}
	
	private void doubleTokenArray() {
		int[] tokens0 = Arrays.copyOf(tokens, 2*tokens.length);
		tokens = tokens0;
	}
	
	private void doubleAppRulesArray() {
		Rule[][] app0 = Arrays.copyOf(applicableRules, 2*applicableRules.length);
		applicableRules = app0;
		int[] nApp0 = Arrays.copyOf(nApp, 2*nApp.length);
		nApp = nApp0;
	}
	
	private void doubleSappRulesArray() {
		Rule[][] sapp0 = Arrays.copyOf(suffixApplicableRules, 2*suffixApplicableRules.length);
		suffixApplicableRules = sapp0;
		int[] nSapp0 = Arrays.copyOf(nSapp, 2*nSapp.length);
		nSapp = nSapp0;
	}
	
	private void doubleTransformLengthsArray() {
		int[][] tr0 = Arrays.copyOf(transformLengths, 2*transformLengths.length);
		transformLengths = tr0;
	}
	
	private void doubleSRLPairsArray() {
		IntPair[][] srl0 = Arrays.copyOf(suffixRuleLenPairs, 2*suffixRuleLenPairs.length);
		suffixRuleLenPairs = srl0;
		int[] nSRL0 = Arrays.copyOf(nSRL, 2*nSRL.length);
		nSRL = nSRL0;
	}
	
	private Rule[] doubleRuleArray(Rule[] arr) {
		return Arrays.copyOf(arr, 2*arr.length);
	}

	private IntPair[] doubleIntPairArray(IntPair[] arr) {
		return Arrays.copyOf(arr, 2*arr.length);
	}
	
	@Override
	public void preprocessTransformLength() {

		for( int i = 0; i < size; ++i ) {
			if ( transformLengths[i] == null ) transformLengths[i] = new int[2];
			transformLengths[ i ][ 0 ] = transformLengths[ i ][ 1 ] = i + 1;
		}

		for( int j=0; j<nApp[0]; ++j ) {
			int fromSize = applicableRules[0][j].lhsSize();
			int toSize = applicableRules[0][j].rhsSize();
			if( fromSize > toSize ) {
				transformLengths[ fromSize - 1 ][ 0 ] = Math.min( transformLengths[ fromSize - 1 ][ 0 ], toSize );
			}
			else if( fromSize < toSize ) {
				transformLengths[ fromSize - 1 ][ 1 ] = Math.max( transformLengths[ fromSize - 1 ][ 1 ], toSize );
			}
		}
		for( int i = 1; i < size; ++i ) {
			transformLengths[ i ][ 0 ] = Math.min( transformLengths[ i ][ 0 ], transformLengths[ i - 1 ][ 0 ] + 1 );
			transformLengths[ i ][ 1 ] = Math.max( transformLengths[ i ][ 1 ], transformLengths[ i - 1 ][ 1 ] + 1 );
			for ( int j=0; j<nApp[i]; ++j ) {
				int fromSize = applicableRules[i][j].lhsSize();
				int toSize = applicableRules[i][j].rhsSize();
				if( fromSize > toSize ) {
					transformLengths[ i + fromSize - 1 ][ 0 ] = Math.min( transformLengths[ i + fromSize - 1 ][ 0 ],
							transformLengths[ i - 1 ][ 0 ] + toSize );
				}
				else if( fromSize < toSize ) {
					transformLengths[ i + fromSize - 1 ][ 1 ] = Math.max( transformLengths[ i + fromSize - 1 ][ 1 ],
							transformLengths[ i - 1 ][ 1 ] + toSize );
				}
			}
		}
	}
	
	@Override
	public int getNumApplicableRules() {
		return Arrays.stream(nApp).sum();
	}

	@Override
	public Iterable<Rule> getApplicableRuleIterable() {
		return new Iterable<Rule>() {
			@Override
			public Iterator<Rule> iterator() {
				return new RuleIterator();
			}
		};
	}






	private class RuleIterator implements Iterator<Rule> {
		int k = 0;
		int i = 0;

		@Override
		public boolean hasNext() {
			return (k < applicableRules.length);
		}

		@Override
		public Rule next() {
			Rule rule = applicableRules[k][i++];
			if ( i >= applicableRules[k].length ) {
				++k;
				i = 0;
			}
			return rule;
		}
	}
}
