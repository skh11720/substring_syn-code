package snu.kdd.substring_syn.data.record;

import java.util.Arrays;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.algorithm.filter.TransLenLazyCalculator;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;

public class ReusableRecord extends AbstractTransformableRecord {
	
	private static int INIT_SIZE = 256;
	private static int INIT_NUM_RULE = 1;
	
	int idx;
	int id;
	int[] tokens = new int[INIT_SIZE];
	int hash;
	
	int size = 0;
	int[] nApp = new int[INIT_SIZE];
	int[] nSapp = new int[INIT_SIZE];
	int[] nSRL = new int[INIT_SIZE];

	Rule[][] applicableRules = new Rule[INIT_SIZE][INIT_NUM_RULE];
	Rule[][] suffixApplicableRules = new Rule[INIT_SIZE][INIT_NUM_RULE];
	IntPair[][] suffixRuleLenPairs = new IntPair[INIT_SIZE][INIT_NUM_RULE];

	int maxTransLen = 0;
	int minTransLen = 0;
	int maxRhsSize = 0;


	public ReusableRecord() {
	}
	
	public void set(int idx, int id, int[] arr, int size) {
		reset();
		setIdx(idx);
		setID(id);
		setTokens(arr, size);
	}
	
	private void reset() {
		maxTransLen = 0;
		minTransLen = 0;
		maxRhsSize = 0;
	}
	
	private void setIdx(int idx) {
		this.idx = idx;
	}
	
	private void setID(int id) {
		this.id = id;
	}

	private void setTokens(int[] arr, int size) {
		setSize(size);
		for ( int i=0; i<size; ++i ) {
			tokens[i] = arr[i];
			nApp[i] = nSapp[i] = nSRL[i] = 0;
		}
		setHash();
	}
	
	public void setMaxRhsSize(int maxRhsSize) {
		this.maxRhsSize = maxRhsSize;
	}
	
	private void setSize(int size) {
		while ( tokens.length < size ) doubleSize();
		this.size = size;
	}
	
	private void setHash() {
		hash = Record.getHash(idx, tokens, size);
	}
	
	public void addApplicableRule(int i, Rule r) {
//		System.out.print(size);
//		System.out.print("\t"+nApp.length);
//		System.out.print("\t"+applicableRules.length);
//		System.out.print("\t"+i);
//		System.out.println("\t"+applicableRules[i].length);
		if ( nApp[i] >= applicableRules[i].length ) applicableRules[i] = doubleRuleArray(applicableRules[i]);
		applicableRules[i][nApp[i]] = r;
		nApp[i] += 1;
	}

	public void addSuffixApplicableRule(int i, Rule r) {
		if ( nSapp[i] >= suffixApplicableRules[i].length ) suffixApplicableRules[i] = doubleRuleArray(suffixApplicableRules[i]);
		suffixApplicableRules[i][nSapp[i]] = r;
		nSapp[i] += 1;
	}

	public void addSuffixRuleLenPairs(int i, IntPair ip) {
		if ( nSRL[i] >= suffixRuleLenPairs[i].length ) suffixRuleLenPairs[i] = doubleIntPairArray(suffixRuleLenPairs[i]);
		suffixRuleLenPairs[i][nSRL[i]] = ip;
		nSRL[i] += 1;
	}
	
	@Override
	public void preprocessApplicableRules() {
		Rule.automata.computeApplicableRules(this);
	}
	
	@Override
	public void preprocessSuffixApplicableRules() {
		ObjectList<ObjectSet<IntPair>> pairList = new ObjectArrayList<>();
		for( int i = 0; i < size(); ++i ) pairList.add( new ObjectOpenHashSet<>() );
		
		for( int i = size() - 1; i >= 0; --i ) {
			for( int j=0; j<nApp[i]; ++j ) {
				Rule rule = applicableRules[i][j];
				int suffixidx = i + rule.getLhs().length - 1;
				addSuffixApplicableRule(suffixidx, rule);
				pairList.get( suffixidx ).add( new IntPair(rule.lhsSize(), rule.rhsSize()) );
			}
		}

		for( int i = 0; i < size(); ++i ) {
			for ( IntPair ip : pairList.get(i) ) {
				addSuffixRuleLenPairs(i, ip);
				maxRhsSize = Math.max(maxRhsSize, ip.i2);
			}
		}
	}
	
//	public void fit(int size) {
//		while ( tokens.length < size) doubleSize();
//	}
//	
//	public void fitApp(int i, int n) {
//		while ( applicableRules[i].length < n ) applicableRules[i] = doubleRuleArray(applicableRules[i]); 
//	}
//	
//	public void fitSapp(int i, int n) {
//		while ( suffixApplicableRules[i].length < n ) suffixApplicableRules[i] = doubleRuleArray(suffixApplicableRules[i]);
//	}
//	
//	public void fitSRL(int i, int n) {
//		while ( suffixRuleLenPairs[i].length < n ) suffixRuleLenPairs[i] = doubleIntPairArray(suffixRuleLenPairs[i]);
//	}
	
	private void doubleSize() {
		doubleTokenArray();
		doubleAppRulesArray();
		doubleSappRulesArray();
		doubleSRLPairsArray();
	}
	
	private void doubleTokenArray() {
		int[] tokens0 = Arrays.copyOf(tokens, 2*tokens.length);
		tokens = tokens0;
	}
	
	private void doubleAppRulesArray() {
		Rule[][] app0 = Arrays.copyOf(applicableRules, 2*applicableRules.length);
		for ( int i=applicableRules.length; i<2*applicableRules.length; ++i ) app0[i] = new Rule[INIT_NUM_RULE];
		applicableRules = app0;
		int[] nApp0 = Arrays.copyOf(nApp, 2*nApp.length);
		nApp = nApp0;
	}
	
	private void doubleSappRulesArray() {
		Rule[][] sapp0 = Arrays.copyOf(suffixApplicableRules, 2*suffixApplicableRules.length);
		for ( int i=suffixApplicableRules.length; i<2*suffixApplicableRules.length; ++i ) sapp0[i] = new Rule[INIT_NUM_RULE];
		suffixApplicableRules = sapp0;
		int[] nSapp0 = Arrays.copyOf(nSapp, 2*nSapp.length);
		nSapp = nSapp0;
	}
	
	private void doubleSRLPairsArray() {
		IntPair[][] srl0 = Arrays.copyOf(suffixRuleLenPairs, 2*suffixRuleLenPairs.length);
		for ( int i=suffixRuleLenPairs.length; i<2*suffixRuleLenPairs.length; ++i ) srl0[i] = new IntPair[INIT_NUM_RULE];
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
	public int getIdx() {
		return idx;
	}

	@Override
	public int getID() {
		return id;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public int getToken(int i) {
		return tokens[i];
	}

	@Override
	public int[] getTokenArray() {
		return getTokenList().toIntArray();
	}

	@Override
	public IntList getTokenList() {
		return IntArrayList.wrap(tokens, size);
	}

	@Override
	public IntSet getCandTokenSet() {
		return new IntOpenHashSet(getTokenList());
	}

	@Override
	public int getMaxRhsSize() {
		if ( maxRhsSize == 0 ) {
			maxRhsSize = 1;
			for ( int i=0; i<size; ++i) {
				for ( IntPair pair : suffixRuleLenPairs[i] ) {
					maxRhsSize = Math.max(maxRhsSize, pair.i2);
				}
			}
		}
		return maxRhsSize;
	}

	@Override
	public Iterable<Rule> getApplicableRules(int i) {
		if ( i >= size() ) {
			IndexOutOfBoundsException e = new IndexOutOfBoundsException();
			e.printStackTrace();
			System.exit(1);
		}
		return ObjectArrayList.wrap(applicableRules[i], nApp[i]);
	}

	@Override
	public Iterable<Rule> getSuffixApplicableRules(int i) {
		if ( i >= size() ) {
			IndexOutOfBoundsException e = new IndexOutOfBoundsException();
			e.printStackTrace();
			System.exit(1);
		}
		return ObjectArrayList.wrap(suffixApplicableRules[i], nSapp[i]);
	}

	@Override
	public int getNumApplicableRules(int i) {
		return nApp[i];
	}
	
	@Override
	public int getNumSuffixApplicableRules(int i) {
		return nSapp[i];
	}
	
	@Override
	public int getNumSuffixRuleLens(int i) {
		return nSRL[i];
	}

	@Override
	public int getMaxTransLength() {
		if ( maxTransLen == 0 ) preprocessTransformLength();
		return maxTransLen;
	}

	protected void preprocessTransformLength() {
		TransLenLazyCalculator cal = new TransLenLazyCalculator(null, this, 0, size(), 0);
		maxTransLen = cal.getUB(size()-1);
		minTransLen = cal.getLB(size()-1);
	}

	@Override
	public Iterable<IntPair> getSuffixRuleLens(int i) {
		if ( i >= size() ) {
			IndexOutOfBoundsException e = new IndexOutOfBoundsException();
			e.printStackTrace();
			System.exit(1);
		}
		return ObjectArrayList.wrap(suffixRuleLenPairs[i], nSRL[i]);
	}
}
