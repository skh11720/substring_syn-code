//package snu.kdd.substring_syn.data.record;
//
//import java.util.Arrays;
//import java.util.Iterator;
//
//import it.unimi.dsi.fastutil.ints.IntList;
//import it.unimi.dsi.fastutil.ints.IntSet;
//import snu.kdd.substring_syn.data.IntPair;
//import snu.kdd.substring_syn.data.Rule;
//
//public class ReusableRecord implements TransformableRecordInterface {
//	
//	int idx;
//	int id;
//	int[] tokens;
//	int hash;
//	
//	int size = 0;
//	int[] nApp;
//	int[] nSapp;
//	int[] nSRL;
//
//	Rule[][] applicableRules = null;
//	Rule[][] suffixApplicableRules = null;
//	IntPair[][] suffixRuleLenPairs = null;
//
//	int maxTransLen = 0;
//	int minTransLen = 0;
//	int maxRhsSize = 0;
//
//
//	public ReusableRecord() {
//	}
//
//	public void setIdx(int idx) {
//		this.idx = idx;
//	}
//	
//	public void setId(int id) {
//		this.id = id;
//	}
//	
//	private void setHash() {
//		hash = Record.getHash(idx, tokens, size);
//	}
//	
//	public void fit(int size) {
//		while ( tokens.length < size) doubleSize();
//		this.size = size;
//	}
//	
//	public void fitApp(int i, int n) {
//		while ( applicableRules[i].length < n ) applicableRules[i] = doubleRuleArray(applicableRules[i]); 
//		nApp[i] = n;
//	}
//	
//	public void fitSapp(int i, int n) {
//		while ( suffixApplicableRules[i].length < n ) suffixApplicableRules[i] = doubleRuleArray(suffixApplicableRules[i]);
//		nSapp[i] = n;
//	}
//	
//	public void fitSRL(int i, int n) {
//		while ( suffixRuleLenPairs[i].length < n ) suffixRuleLenPairs[i] = doubleIntPairArray(suffixRuleLenPairs[i]);
//		nSRL[i] = n;
//	}
//	
//	private void doubleSize() {
//		doubleTokenArray();
//		doubleAppRulesArray();
//		doubleSappRulesArray();
//		doubleSRLPairsArray();
//	}
//	
//	private void doubleTokenArray() {
//		int[] tokens0 = Arrays.copyOf(tokens, 2*tokens.length);
//		tokens = tokens0;
//	}
//	
//	private void doubleAppRulesArray() {
//		Rule[][] app0 = Arrays.copyOf(applicableRules, 2*applicableRules.length);
//		applicableRules = app0;
//		int[] nApp0 = Arrays.copyOf(nApp, 2*nApp.length);
//		nApp = nApp0;
//	}
//	
//	private void doubleSappRulesArray() {
//		Rule[][] sapp0 = Arrays.copyOf(suffixApplicableRules, 2*suffixApplicableRules.length);
//		suffixApplicableRules = sapp0;
//		int[] nSapp0 = Arrays.copyOf(nSapp, 2*nSapp.length);
//		nSapp = nSapp0;
//	}
//	
//	private void doubleSRLPairsArray() {
//		IntPair[][] srl0 = Arrays.copyOf(suffixRuleLenPairs, 2*suffixRuleLenPairs.length);
//		suffixRuleLenPairs = srl0;
//		int[] nSRL0 = Arrays.copyOf(nSRL, 2*nSRL.length);
//		nSRL = nSRL0;
//	}
//	
//	private Rule[] doubleRuleArray(Rule[] arr) {
//		return Arrays.copyOf(arr, 2*arr.length);
//	}
//
//	private IntPair[] doubleIntPairArray(IntPair[] arr) {
//		return Arrays.copyOf(arr, 2*arr.length);
//	}
//
//	@Override
//	public int getIdx() {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public int getID() {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public int size() {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public int getToken(int i) {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public int getSidx() {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public Record getSuperRecord() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public int[] getTokenArray() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public IntList getTokenList() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public IntSet getCandTokenSet() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public int getMaxRhsSize() {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public String toOriginalString() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public String toStringDetails() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Iterable<Rule> getApplicableRuleIterable() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Iterable<Rule> getApplicableRules(int i) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public Iterable<Rule> getSuffixApplicableRules(int i) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public int getNumApplicableRules(int pos) {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public int getMaxTransLength() {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//}
