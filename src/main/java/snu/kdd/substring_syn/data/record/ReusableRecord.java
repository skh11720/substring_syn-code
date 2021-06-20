package snu.kdd.substring_syn.data.record;

import java.util.Arrays;
import java.util.Iterator;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import snu.kdd.substring_syn.algorithm.filter.TransLenLazyCalculator;
import snu.kdd.substring_syn.data.IntPair;
import snu.kdd.substring_syn.data.Rule;
import snu.kdd.substring_syn.data.Ruleset;
import snu.kdd.substring_syn.data.Substring;

public class ReusableRecord extends AbstractTransformableRecord implements RecursiveRecordInterface {
	
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
	int[][] suffixRuleLenPairs = new int[INIT_SIZE][2*INIT_NUM_RULE];

	int maxTransLen = 0;
	int minTransLen = 0;
	int maxRhsSize = 0;


	public ReusableRecord() {
	}

	public void set(int idx, int id, Substring str) {
		int[] arr = Ruleset.getTokenIndexArray(str);
		set(idx, id, arr, arr.length);
	}
	
	public void set(int idx, int id, int[] arr, int size) {
		reset();
		setIdx(idx);
		setID(id);
		setTokens(arr, size);
	}
	
	public void set(int idx, int id, IntIterator iter, int size) {
		reset();
		setIdx(idx);
		setID(id);
		setTokens(iter, size);
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

	private void setTokens(IntIterator iter, int size) {
		setSize(size);
		for ( int i=0; i<size; ++i ) {
			tokens[i] = iter.nextInt();
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
		if ( nApp[i] >= applicableRules[i].length ) applicableRules[i] = doubleRuleArray(applicableRules[i]);
		applicableRules[i][nApp[i]] = r;
		nApp[i] += 1;
	}

	public void addSuffixApplicableRule(int i, Rule r) {
		if ( nSapp[i] >= suffixApplicableRules[i].length ) suffixApplicableRules[i] = doubleRuleArray(suffixApplicableRules[i]);
		suffixApplicableRules[i][nSapp[i]] = r;
		nSapp[i] += 1;
	}

	public void addSuffixRuleLenPairs(int i, int l1, int l2) {
		if ( 2*nSRL[i] >= suffixRuleLenPairs[i].length ) suffixRuleLenPairs[i] = doubleIntArray(suffixRuleLenPairs[i]);
		suffixRuleLenPairs[i][2*nSRL[i]] = l1;
		suffixRuleLenPairs[i][2*nSRL[i]+1] = l2;
		nSRL[i] += 1;
	}

    @Override
    public void preprocessApplicableRules() {
    	if ( nApp[0] > 0 ) return;
        Rule.automata.computeApplicableRules(this);
        return;
    }
    
    @Override
    public void preprocessSuffixApplicableRules() {
    	if ( nSapp[0] > 0 ) return;
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
                addSuffixRuleLenPairs(i, ip.i1, ip.i2);
                maxRhsSize = Math.max(maxRhsSize, ip.i2);
            }
        }
        return;
    }
	
	
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
		int[][] srl0 = Arrays.copyOf(suffixRuleLenPairs, 2*suffixRuleLenPairs.length);
		for ( int i=suffixRuleLenPairs.length; i<2*suffixRuleLenPairs.length; ++i ) srl0[i] = new int[2*INIT_NUM_RULE];
		suffixRuleLenPairs = srl0;
		int[] nSRL0 = Arrays.copyOf(nSRL, 2*nSRL.length);
		nSRL = nSRL0;
	}
	
	private int[] doubleIntArray(int[] arr) {
		return Arrays.copyOf(arr,  2*arr.length);
	}
	
	private Rule[] doubleRuleArray(Rule[] arr) {
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
	public int getMaxRhsSize() {
		if ( maxRhsSize == 0 ) {
			maxRhsSize = 1;
			for ( int i=0; i<size; ++i) {
				for ( IntPair pair : getSuffixRuleLens(i) ) {
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
	public Iterable<IntPair> getSuffixRuleLens( int k ) {
		if ( suffixRuleLenPairs == null ) {
			return null;
		}
		else {
			return new Iterable<IntPair>() {
				
				@Override
				public Iterator<IntPair> iterator() {
					return new Iterator<IntPair>() {
						
						IntPair pair = new IntPair();
						int i = 0;
						
						@Override
						public IntPair next() {
							pair.i1 = suffixRuleLenPairs[k][2*i];
							pair.i2 = suffixRuleLenPairs[k][2*i+1];
							i += 1;
							return pair;
						}
						
						@Override
						public boolean hasNext() {
							return i < getNumSuffixRuleLens(k);
						}
					};
				}
			};
		}
	}

	@Override
	public int getSidx() {
		return 0;
	}

	@Override
	public TransformableRecordInterface getSuperRecord() {
		return this;
	}
	
	public Record toRecord() {
		int[] tokens = Arrays.copyOf(this.tokens, size);
		Record rec = new Record(idx, id, tokens);
		rec.applicableRules = new Rule[size][];
		rec.suffixApplicableRules = new Rule[size][];
		rec.suffixRuleLenPairs = new int[size][];
		for ( int i=0; i<size; ++i ) {
			rec.applicableRules[i] = new Rule[nApp[i]];
			for ( int j=0; j<nApp[i]; ++j ) rec.applicableRules[i][j] = this.applicableRules[i][j];
			rec.suffixApplicableRules[i] = new Rule[nSapp[i]];
			for ( int j=0; j<nSapp[i]; ++j ) rec.suffixApplicableRules[i][j] = this.suffixApplicableRules[i][j];
			rec.suffixRuleLenPairs[i] = new int[2*nSRL[i]];
			for ( int j=0; j<2*nSRL[i]; ++j ) rec.suffixRuleLenPairs[i][j] = this.suffixRuleLenPairs[i][j];
		}
		rec.maxRhsSize = maxRhsSize;
		rec.maxTransLen = maxTransLen;
		rec.minTransLen = minTransLen;
		return rec;
	}
}
